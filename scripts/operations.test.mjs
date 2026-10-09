import assert from "node:assert/strict"
import { mkdir, mkdtemp, readFile, rm, writeFile } from "node:fs/promises"
import { tmpdir } from "node:os"
import { join } from "node:path"
import { spawnSync } from "node:child_process"
import { fileURLToPath } from "node:url"
import { retentionDate, validateBackupEnvironment } from "./backup-s3.mjs"
import { parseArguments, validateDeploymentEnvironment } from "./deploy-production.mjs"

const root = fileURLToPath(new URL("../", import.meta.url))
const temporaryDirectory = await mkdtemp(join(tmpdir(), "comply-operations-test-"))
const binaryDirectory = join(temporaryDirectory, "bin")
const statePath = join(temporaryDirectory, "aws-state.json")

function executable(name, source) {
  return writeFile(join(binaryDirectory, name), `#!/usr/bin/env node\n${source}`, {
    mode: 0o755,
  })
}

function run(script, args = [], env = {}) {
  return spawnSync(process.execPath, [join(root, script), ...args], {
    cwd: root,
    encoding: "utf8",
    env: {
      ...process.env,
      PATH: `${binaryDirectory}:${process.env.PATH}`,
      ...env,
    },
  })
}

const productionEnvironment = {
  DOMAIN: "trycomply.io",
  API_DOMAIN: "api.trycomply.io",
  COMPLY_ENVIRONMENT: "production",
  SPRING_PROFILES_ACTIVE: "production",
  POSTGRES_DB: "comply_db",
  POSTGRES_USER: "comply_app",
  POSTGRES_PASSWORD: "strong-postgres-password-value",
  REDIS_PASSWORD: "strong-redis-password-value",
  JWT_SECRET: "a".repeat(64),
  MFA_ENCRYPTION_KEY: "b".repeat(64),
  POSTGRES_VOLUME_NAME: "comply_postgres_prod_data",
  REDIS_VOLUME_NAME: "comply_redis_prod_data",
  CORS_ALLOWED_ORIGINS: "https://trycomply.io",
  NEXT_PUBLIC_API_URL: "https://api.trycomply.io/api/v1",
  COMPLY_BOOTSTRAP_ENABLED: "false",
}

/** Exercise deployment and backup safety with isolated command fixtures. */
async function main() {
  await mkdir(binaryDirectory)
  await executable(
    "docker",
    `import { appendFile, writeFile } from "node:fs/promises";\n` +
      `const args = process.argv.slice(2);\n` +
      `if (args[0] === "exec" && process.env.FAKE_DOCKER_FAIL === "true") process.exit(17);\n` +
      `if (process.env.FAKE_DOCKER_STATE) await appendFile(process.env.FAKE_DOCKER_STATE, JSON.stringify(args) + "\\n");\n` +
      `if (process.env.FAKE_COMPOSE_PROFILE_STATE) await writeFile(process.env.FAKE_COMPOSE_PROFILE_STATE, JSON.stringify({ args, profiles: process.env.COMPOSE_PROFILES }));\n` +
      `if (process.env.FAKE_DOCKER_ENV_STATE && args[0] === "compose") await writeFile(process.env.FAKE_DOCKER_ENV_STATE, process.env.COMPOSE_PROFILES ?? "");\n` +
      `if (args[0] === "exec") process.stdout.write("mock database dump");\n` +
      `else if (args[0] === "inspect") process.stdout.write("healthy");\n`
  )
  await executable("gzip", "process.stdin.pipe(process.stdout);\n")
  await executable(
    "gpg",
    `import { createReadStream, createWriteStream } from "node:fs";\n` +
      `createReadStream(null, { fd: 3, autoClose: false }).resume();\n` +
      `const index = process.argv.indexOf("--output");\n` +
      `if (index < 0) process.exit(18);\n` +
      `process.stdin.pipe(createWriteStream(process.argv[index + 1]));\n`
  )
  await executable(
    "aws",
    `import { copyFile, stat, writeFile } from "node:fs/promises";\n` +
      `const args = process.argv.slice(2);\n` +
      `if (args[0] === "s3" && args[1] === "cp") {\n` +
      `  if (process.env.FAKE_AWS_UPLOAD_FAIL === "true") process.exit(19);\n` +
      `  const source = args[2];\n` +
      `  const metadata = args[args.indexOf("--metadata") + 1].split("=")[1];\n` +
      `  const size = (await stat(source)).size;\n` +
      `  await writeFile(process.env.FAKE_AWS_STATE, JSON.stringify({ size, checksum: metadata }));\n` +
      `} else if (args[0] === "s3api" && args[1] === "head-object") {\n` +
      `  const state = JSON.parse(await (await import("node:fs/promises")).readFile(process.env.FAKE_AWS_STATE, "utf8"));\n` +
      `  if (process.env.FAKE_AWS_BAD_CHECKSUM === "true") state.checksum = "invalid";\n` +
      `  process.stdout.write(JSON.stringify({ ContentLength: state.size, Metadata: { sha256: state.checksum } }));\n` +
      `} else process.exit(20);\n`
  )

  assert.throws(
    () => validateBackupEnvironment({}),
    /S3_BACKUP_BUCKET, AWS_REGION, BACKUP_ENCRYPTION_PASSPHRASE/
  )
  const backupConfig = validateBackupEnvironment({
    S3_BACKUP_BUCKET: "comply-backups",
    AWS_REGION: "af-south-1",
    BACKUP_ENCRYPTION_PASSPHRASE: "secret-value",
  })
  assert.equal(backupConfig.retentionYears, 10)
  assert.equal(backupConfig.container, "comply-postgres-dev")
  assert.equal(
    validateBackupEnvironment({
      S3_BACKUP_BUCKET: "comply-backups",
      AWS_REGION: "af-south-1",
      BACKUP_ENCRYPTION_PASSPHRASE: "secret-value",
      COMPLY_ENVIRONMENT: "production",
    }).container,
    "comply-postgres"
  )
  assert.match(retentionDate(1), /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$/)

  assert.throws(() => parseArguments(["--skip-security-check"]), /Unsupported/)
  assert.throws(() => parseArguments(["--down"]), /requires --confirm-production-down/)
  assert.deepEqual(parseArguments(["--down", "--confirm-production-down"]).down, true)
  assert.throws(
    () =>
      validateDeploymentEnvironment({
        ...productionEnvironment,
        SPRING_PROFILES_ACTIVE: "dev",
      }),
    /SPRING_PROFILES_ACTIVE/
  )
  assert.throws(
    () =>
      validateDeploymentEnvironment({
        ...productionEnvironment,
        POSTGRES_VOLUME_NAME: "comply_postgres_data",
      }),
    /preserve comply_postgres_prod_data/
  )
  assert.throws(
    () =>
      validateDeploymentEnvironment({
        ...productionEnvironment,
        CORS_ALLOWED_ORIGINS: "http://localhost:3000",
      }),
    /explicit production origins/
  )
  assert.throws(
    () =>
      validateDeploymentEnvironment({
        ...productionEnvironment,
        COMPLY_BOOTSTRAP_ENABLED: "TRUE",
      }),
    /COMPLY_BOOTSTRAP_ENABLED must be false/
  )
  assert.throws(
    () =>
      validateDeploymentEnvironment({
        ...productionEnvironment,
        COMPLY_BOOTSTRAP_ENABLED: "",
      }),
    /Missing required production settings: COMPLY_BOOTSTRAP_ENABLED/
  )
  assert.doesNotThrow(() => validateDeploymentEnvironment(productionEnvironment))
  for (const origins of [
    " https://trycomply.io , https://portal.example.com:8443 ",
    "https://portal.example.com,https://trycomply.io",
  ]) {
    assert.doesNotThrow(() =>
      validateDeploymentEnvironment({
        ...productionEnvironment,
        CORS_ALLOWED_ORIGINS: origins,
      })
    )
  }
  for (const origin of [
    "https://TRYCOMPLY.io:443",
    "https://TRYCOMPLY.io",
    "HTTPS://trycomply.io",
    "https://trycomply.io:443",
    "https://trycomply.io/",
    "https://%74rycomply.io",
    "https://trycomply.io:0443",
  ]) {
    for (const origins of [origin, `https://trycomply.io,${origin}`]) {
      assert.throws(
        () =>
          validateDeploymentEnvironment({
            ...productionEnvironment,
            CORS_ALLOWED_ORIGINS: origins,
          }),
        /explicit production origins/,
        `Must reject a non-canonical origin: ${JSON.stringify(origins)}`
      )
    }
  }
  for (const origin of [
    "",
    " ",
    "not-a-url",
    "http://example.com",
    "https://localhost",
    "https://app.localhost",
    "https://127.1",
    "https://127.0.0.2",
    "https://[::1]",
    "https://*.example.com",
    "https://example.com/path",
    "https://example.com/..",
    "https://example.com?",
    "https://example.com#",
    "https://example.com?q=1",
    "https://example.com#fragment",
    "https://user:password@example.com",
    "https://example.com\\path",
    "https://exa\tmple.com",
  ]) {
    assert.throws(
      () =>
        validateDeploymentEnvironment({
          ...productionEnvironment,
          CORS_ALLOWED_ORIGINS: `https://trycomply.io,${origin}`,
        }),
      /explicit production origins/,
      `Must reject an invalid origin even alongside the required origin: ${JSON.stringify(origin)}`
    )
  }
  for (const origins of [
    ",https://trycomply.io",
    "https://trycomply.io,,https://example.com",
  ]) {
    assert.throws(
      () =>
        validateDeploymentEnvironment({
          ...productionEnvironment,
          CORS_ALLOWED_ORIGINS: origins,
        }),
      /explicit production origins/
    )
  }
  for (const origin of [
    "https://trycomply.io.attacker.example",
    "https://attacker-trycomply.io",
    "https://trycomply.io:8443",
    "https://attacker.example",
  ]) {
    assert.throws(
      () =>
        validateDeploymentEnvironment({
          ...productionEnvironment,
          CORS_ALLOWED_ORIGINS: origin,
        }),
      /must allow https:\/\/trycomply\.io/
    )
  }

  const productionPreflight = run("scripts/production-compose-preflight.mjs", [], {
    ...productionEnvironment,
  })
  assert.equal(productionPreflight.status, 0, productionPreflight.stderr)
  const insecurePreflight = run("scripts/production-compose-preflight.mjs", [], {
    ...productionEnvironment,
    JWT_SECRET: "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
  })
  assert.notEqual(insecurePreflight.status, 0)
  assert.match(insecurePreflight.stderr, /JWT_SECRET/)

  const composeProfilePath = join(temporaryDirectory, "compose-profile.json")
  const composeUp = run("scripts/compose.mjs", ["up"], {
    COMPOSE_PROFILES: "production",
    FAKE_COMPOSE_PROFILE_STATE: composeProfilePath,
  })
  assert.equal(composeUp.status, 0, composeUp.stderr)
  const composeInvocation = JSON.parse(await readFile(composeProfilePath, "utf8"))
  assert.ok(composeInvocation.args.includes("app"))
  assert.equal(composeInvocation.profiles, "app")

  const backupEnv = {
    S3_BACKUP_BUCKET: "comply-backups-test",
    S3_BACKUP_PREFIX: "verification",
    AWS_REGION: "af-south-1",
    BACKUP_ENCRYPTION_PASSPHRASE: "test-passphrase-not-a-production-secret",
    BACKUP_OBJECT_LOCK_RETENTION_YEARS: "1",
    FAKE_AWS_STATE: statePath,
  }
  const successfulBackup = run("scripts/backup-s3.mjs", [], backupEnv)
  assert.equal(successfulBackup.status, 0, successfulBackup.stderr)
  assert.match(successfulBackup.stdout, /uploaded and verified/)
  assert.doesNotMatch(
    successfulBackup.stdout + successfulBackup.stderr,
    /test-passphrase/
  )

  const failedDump = run("scripts/backup-s3.mjs", [], {
    ...backupEnv,
    FAKE_DOCKER_FAIL: "true",
  })
  assert.notEqual(failedDump.status, 0, "A failed database dump must fail the backup.")

  const failedUpload = run("scripts/backup-s3.mjs", [], {
    ...backupEnv,
    FAKE_AWS_UPLOAD_FAIL: "true",
  })
  assert.notEqual(failedUpload.status, 0, "A failed object upload must fail the backup.")

  const failedVerification = run("scripts/backup-s3.mjs", [], {
    ...backupEnv,
    FAKE_AWS_BAD_CHECKSUM: "true",
  })
  assert.notEqual(
    failedVerification.status,
    0,
    "A mismatched remote checksum must fail the backup."
  )

  const deploymentHelp = run("scripts/deploy-production.mjs", ["--help"])
  assert.equal(deploymentHelp.status, 0, deploymentHelp.stderr)
  const invalidDeployment = run("scripts/deploy-production.mjs", [], {
    PATH: binaryDirectory,
  })
  assert.notEqual(invalidDeployment.status, 0)
  assert.match(invalidDeployment.stderr, /Missing required production settings/)

  const dockerStatePath = join(temporaryDirectory, "docker-state.jsonl")
  const dockerProfilesPath = join(temporaryDirectory, "docker-profiles.txt")
  const deploymentEnvironment = {
    ...productionEnvironment,
    PATH: `${binaryDirectory}:${process.env.PATH}`,
    COMPOSE_PROFILES: "app",
    FAKE_DOCKER_ENV_STATE: dockerProfilesPath,
    FAKE_DOCKER_STATE: dockerStatePath,
  }
  const deployment = run("scripts/deploy-production.mjs", [], deploymentEnvironment)
  assert.equal(deployment.status, 0, deployment.stderr)
  assert.equal(await readFile(dockerProfilesPath, "utf8"), "production")
  assert.match(deployment.stdout, /Production deployment is healthy/)
  const deploymentCalls = (await readFile(dockerStatePath, "utf8"))
    .trim()
    .split("\n")
    .map((line) => JSON.parse(line))
  assert.ok(
    deploymentCalls.some((call) => call.includes("up") && call.includes("--build"))
  )
  assert.equal(deploymentCalls.filter((call) => call[0] === "inspect").length, 5)

  await writeFile(dockerStatePath, "")
  const shutdown = run(
    "scripts/deploy-production.mjs",
    ["--down", "--confirm-production-down"],
    deploymentEnvironment
  )
  assert.equal(shutdown.status, 0, shutdown.stderr)
  const shutdownCalls = (await readFile(dockerStatePath, "utf8"))
    .trim()
    .split("\n")
    .map((line) => JSON.parse(line))
  const shutdownCommand = shutdownCalls.find((call) => call.includes("down"))
  assert.ok(shutdownCommand, "Confirmed shutdown should invoke Compose down.")
  assert.ok(!shutdownCommand.includes("-v"), "Shutdown must preserve named data volumes.")

  console.log("Operations validation passed.")
}

try {
  await main()
} finally {
  await rm(temporaryDirectory, { recursive: true, force: true })
}
