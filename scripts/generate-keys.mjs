import { randomBytes } from "node:crypto"
import { chmod, copyFile, readFile, writeFile } from "node:fs/promises"
import { isAbsolute, resolve } from "node:path"
import { fileURLToPath } from "node:url"

const root = fileURLToPath(new URL("../", import.meta.url))
const args = process.argv.slice(2)
let targetFile
let writeMode = false

function usage() {
  console.log(`Usage: node scripts/generate-keys.mjs [options]

Options:
  --write, -w          Write generated keys to the root .env file
  --file, -f <path>    Write generated keys to a specified environment file
  --stdout, -s         Print generated keys without writing a file (default)
  --help, -h           Show this help message

Generated values include the JWT and MFA keys, database and cache passwords,
and the backup encryption passphrase.`)
}

for (let index = 0; index < args.length; index += 1) {
  const arg = args[index]
  switch (arg) {
    case "--write":
    case "-w":
      writeMode = true
      targetFile = resolve(root, ".env")
      break
    case "--file":
    case "-f": {
      const path = args[index + 1]
      if (!path || path.startsWith("-")) {
        throw new Error(`${arg} requires a file path.`)
      }
      writeMode = true
      targetFile = isAbsolute(path) ? path : resolve(path)
      index += 1
      break
    }
    case "--stdout":
    case "-s":
      writeMode = false
      targetFile = undefined
      break
    case "--help":
    case "-h":
      usage()
      process.exit(0)
    default:
      throw new Error(`Unknown option: ${arg}`)
  }
}

const mfaKey = randomBytes(32).toString("base64")
const values = {
  JWT_SECRET: randomBytes(32).toString("hex"),
  MFA_ENCRYPTION_KEY: mfaKey,
  MFA_AES_KEY: mfaKey,
  POSTGRES_PASSWORD: randomBytes(16).toString("hex"),
  REDIS_PASSWORD: randomBytes(16).toString("hex"),
  BACKUP_ENCRYPTION_PASSPHRASE: randomBytes(16).toString("hex"),
}
const output = Object.entries(values)
  .map(([key, value]) => `${key}=${value}`)
  .join("\n")

if (!writeMode) {
  console.log(output)
  process.exit(0)
}

if (!targetFile) {
  throw new Error("A destination file is required when writing generated keys.")
}

let contents
try {
  contents = await readFile(targetFile, "utf8")
} catch (error) {
  if (error.code !== "ENOENT") throw error
  const exampleFile = resolve(root, ".env.example")
  try {
    await copyFile(exampleFile, targetFile)
    await chmod(targetFile, 0o600)
    contents = await readFile(targetFile, "utf8")
    console.log(`Created ${targetFile} from the root .env.example.`)
  } catch (copyError) {
    if (copyError.code !== "ENOENT") throw copyError
    contents = ""
    await writeFile(targetFile, contents, { mode: 0o600 })
  }
}

for (const [key, value] of Object.entries(values)) {
  const entry = new RegExp(`^${key}=.*$`, "m")
  if (entry.test(contents)) {
    contents = contents.replace(entry, `${key}=${value}`)
  } else {
    contents += `${contents && !contents.endsWith("\n") ? "\n" : ""}${key}=${value}\n`
  }
}

await writeFile(targetFile, contents, { mode: 0o600 })
await chmod(targetFile, 0o600)
console.log(`Updated generated security values in ${targetFile}.`)
