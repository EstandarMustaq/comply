import { createHash } from "node:crypto"
import { createReadStream } from "node:fs"
import { mkdtemp, rm, stat } from "node:fs/promises"
import { tmpdir } from "node:os"
import { join } from "node:path"
import { spawn } from "node:child_process"
import { pipeline } from "node:stream/promises"
import { fileURLToPath, pathToFileURL } from "node:url"

const root = fileURLToPath(new URL("../", import.meta.url))
const args = process.argv.slice(2)

function usage() {
  console.log(`Usage: pnpm backup

Creates an encrypted PostgreSQL logical backup and uploads it to S3 with
Object Lock retention. Requires Docker, gzip, GPG, and AWS CLI access.
The backup includes PostgreSQL only; Redis and imported files are not included.`)
}

function loadEnvironment() {
  try {
    process.loadEnvFile(join(root, ".env"))
  } catch (error) {
    if (error.code !== "ENOENT") throw error
  }
}

function required(name) {
  const value = process.env[name]?.trim()
  if (!value) throw new Error(`Required environment variable ${name} is not set.`)
  return value
}

function run(executable, commandArgs, options = {}) {
  const child = spawn(executable, commandArgs, {
    cwd: root,
    stdio: "inherit",
    ...options,
  })
  return new Promise((resolve, reject) => {
    child.once("error", reject)
    child.once("close", (code, signal) => {
      if (code === 0) resolve()
      else reject(new Error(`${executable} failed (${signal ?? `exit ${code}`}).`))
    })
  })
}

function start(executable, commandArgs, options = {}) {
  const child = spawn(executable, commandArgs, {
    cwd: root,
    stdio: "pipe",
    ...options,
  })
  let stderr = ""
  child.stderr?.setEncoding("utf8")
  child.stderr?.on("data", (chunk) => {
    stderr += chunk
  })
  const closed = new Promise((resolve, reject) => {
    child.once("error", reject)
    child.once("close", (code, signal) => {
      if (code === 0) resolve()
      else
        reject(
          new Error(
            `${executable} failed (${signal ?? `exit ${code}`}): ${stderr.trim()}`
          )
        )
    })
  })
  return { child, closed }
}

export function retentionDate(years) {
  const date = new Date()
  const day = date.getUTCDate()
  date.setUTCFullYear(date.getUTCFullYear() + years)
  if (date.getUTCDate() !== day) date.setUTCDate(0)
  return date.toISOString().replace(/\.\d{3}Z$/, "Z")
}

async function createEncryptedDump({ container, database, user, passphrase, path }) {
  const dump = start("docker", [
    "exec",
    container,
    "pg_dump",
    "--no-password",
    "--username",
    user,
    "--dbname",
    database,
  ])
  const compressor = start("gzip", ["--stdout", "--no-name"])
  const encryptor = start(
    "gpg",
    [
      "--batch",
      "--yes",
      "--pinentry-mode",
      "loopback",
      "--passphrase-fd",
      "3",
      "--symmetric",
      "--cipher-algo",
      "AES256",
      "--output",
      path,
    ],
    { stdio: ["pipe", "ignore", "pipe", "pipe"] }
  )
  let interrupted
  const abort = (signal) => {
    interrupted = signal
    dump.child.kill(signal)
    compressor.child.kill(signal)
    encryptor.child.kill(signal)
  }
  const onSigint = () => abort("SIGINT")
  const onSigterm = () => abort("SIGTERM")
  process.once("SIGINT", onSigint)
  process.once("SIGTERM", onSigterm)
  try {
    const secret = encryptor.child.stdio[3]
    secret.end(passphrase)
    await Promise.all([
      pipeline(dump.child.stdout, compressor.child.stdin),
      pipeline(compressor.child.stdout, encryptor.child.stdin),
      dump.closed,
      compressor.closed,
      encryptor.closed,
    ])
    if (interrupted) throw new Error(`Backup interrupted by ${interrupted}.`)
  } catch (error) {
    for (const child of [dump.child, compressor.child, encryptor.child]) {
      if (child.exitCode === null && child.signalCode === null) child.kill("SIGTERM")
    }
    throw error
  } finally {
    process.removeListener("SIGINT", onSigint)
    process.removeListener("SIGTERM", onSigterm)
  }
}

export function validateBackupEnvironment(env) {
  const requiredNames = ["S3_BACKUP_BUCKET", "AWS_REGION", "BACKUP_ENCRYPTION_PASSPHRASE"]
  const missing = requiredNames.filter((name) => !env[name]?.trim())
  if (missing.length) {
    throw new Error(`Required environment variables are not set: ${missing.join(", ")}.`)
  }
  const retentionYears = Number(env.BACKUP_OBJECT_LOCK_RETENTION_YEARS ?? "10")
  if (!Number.isSafeInteger(retentionYears) || retentionYears < 1) {
    throw new Error("BACKUP_OBJECT_LOCK_RETENTION_YEARS must be a positive integer.")
  }
  const prefix = (env.S3_BACKUP_PREFIX ?? "daily-dumps").replace(/^\/+|\/+$/g, "")
  if (!prefix || prefix.split("/").includes("..")) {
    throw new Error("S3_BACKUP_PREFIX must be a non-empty object-key prefix.")
  }
  return {
    bucket: env.S3_BACKUP_BUCKET.trim(),
    region: env.AWS_REGION.trim(),
    passphrase: env.BACKUP_ENCRYPTION_PASSPHRASE,
    retentionYears,
    prefix,
    database: env.POSTGRES_DB ?? "complyz_db",
    user: env.POSTGRES_USER ?? "complyz_app",
    container: env.POSTGRES_CONTAINER ?? "complyz-postgres",
  }
}

async function main() {
  if (args.includes("--help") || args.includes("-h")) {
    usage()
    return
  }
  if (args.length > 0) throw new Error(`Unsupported backup option: ${args[0]}`)

  loadEnvironment()
  const {
    bucket,
    region,
    passphrase,
    retentionYears,
    prefix,
    database,
    user,
    container,
  } = validateBackupEnvironment(process.env)
  const timestamp = new Date()
    .toISOString()
    .replace(/[-:]/g, "")
    .replace(/\.\d{3}Z$/, "Z")
  const temporaryDirectory = await mkdtemp(join(tmpdir(), "complyz-backup-"))
  const encryptedPath = join(
    temporaryDirectory,
    `complyz_${database}_${timestamp}.sql.gz.gpg`
  )
  try {
    await createEncryptedDump({
      container,
      database,
      user,
      passphrase,
      path: encryptedPath,
    })
    const encryptedStat = await stat(encryptedPath)
    if (encryptedStat.size === 0) throw new Error("Encrypted backup artifact is empty.")
    const digest = createHash("sha256")
    for await (const chunk of createReadStream(encryptedPath)) digest.update(chunk)
    const checksum = digest.digest("hex")
    const filename = encryptedPath.split(/[\\/]/).at(-1)
    if (!filename) throw new Error("Could not determine the generated backup filename.")
    const key = `${prefix}/${filename}`
    const retainUntil = retentionDate(retentionYears)
    await run("aws", [
      "s3",
      "cp",
      encryptedPath,
      `s3://${bucket}/${key}`,
      "--region",
      region,
      "--storage-class",
      "STANDARD_IA",
      "--checksum-algorithm",
      "SHA256",
      "--metadata",
      `sha256=${checksum}`,
      "--object-lock-mode",
      "COMPLIANCE",
      "--object-lock-retain-until-date",
      retainUntil,
    ])
    const result = await new Promise((resolve, reject) => {
      const child = spawn(
        "aws",
        [
          "s3api",
          "head-object",
          "--bucket",
          bucket,
          "--key",
          key,
          "--region",
          region,
          "--output",
          "json",
        ],
        { cwd: root, stdio: ["ignore", "pipe", "pipe"] }
      )
      let stdout = ""
      let stderr = ""
      child.stdout.setEncoding("utf8")
      child.stderr.setEncoding("utf8")
      child.stdout.on("data", (chunk) => (stdout += chunk))
      child.stderr.on("data", (chunk) => (stderr += chunk))
      child.once("error", reject)
      child.once("close", (code, signal) => {
        if (code !== 0) {
          reject(
            new Error(
              `aws s3api head-object failed (${signal ?? `exit ${code}`}): ${stderr.trim()}`
            )
          )
          return
        }
        try {
          resolve(JSON.parse(stdout))
        } catch (error) {
          reject(
            new Error("AWS CLI returned invalid head-object JSON.", { cause: error })
          )
        }
      })
    })
    const remoteChecksum = result.Metadata?.sha256
    if (
      Number(result.ContentLength) !== encryptedStat.size ||
      remoteChecksum !== checksum
    ) {
      throw new Error(
        "Uploaded backup verification failed: size or SHA-256 metadata does not match."
      )
    }
    console.log(
      `Encrypted PostgreSQL backup uploaded and verified: s3://${bucket}/${key}`
    )
  } finally {
    await rm(temporaryDirectory, { recursive: true, force: true })
  }
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  main().catch((error) => {
    console.error(`Backup failed: ${error.message}`)
    process.exitCode = 1
  })
}
