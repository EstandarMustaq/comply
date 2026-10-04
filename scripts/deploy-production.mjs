import { spawn } from "node:child_process"
import { resolve } from "node:path"
import { fileURLToPath } from "node:url"

const root = fileURLToPath(new URL("../", import.meta.url))
const usage = `Usage: pnpm deploy [--no-cache]
       pnpm deploy -- --down --confirm-production-down

Deploys the production Compose profile after validating production settings and
existing data volume names. Shutdown preserves named volumes.`

const requiredVariables = [
  "DOMAIN",
  "API_DOMAIN",
  "POSTGRES_DB",
  "POSTGRES_USER",
  "POSTGRES_PASSWORD",
  "REDIS_PASSWORD",
  "JWT_SECRET",
  "MFA_ENCRYPTION_KEY",
  "SPRING_PROFILES_ACTIVE",
  "POSTGRES_VOLUME_NAME",
  "REDIS_VOLUME_NAME",
]

export function parseArguments(args) {
  const options = { down: false, confirmDown: false, noCache: false, help: false }
  for (const arg of args) {
    switch (arg) {
      case "--help":
      case "-h":
        options.help = true
        break
      case "--down":
        options.down = true
        break
      case "--confirm-production-down":
        options.confirmDown = true
        break
      case "--no-cache":
        options.noCache = true
        break
      default:
        throw new Error(`Unsupported deployment option: ${arg}`)
    }
  }
  if (options.down && options.noCache) {
    throw new Error("--no-cache cannot be combined with --down.")
  }
  if (options.down !== options.confirmDown) {
    throw new Error(
      options.down
        ? "Production shutdown requires --confirm-production-down."
        : "--confirm-production-down is only valid with --down."
    )
  }
  return options
}

export function validateDeploymentEnvironment(env) {
  const missing = requiredVariables.filter((name) => !env[name]?.trim())
  if (missing.length) {
    throw new Error(`Missing required production settings: ${missing.join(", ")}.`)
  }
  if (env.SPRING_PROFILES_ACTIVE !== "production") {
    throw new Error("SPRING_PROFILES_ACTIVE must be 'production'.")
  }
  if (env.COMPLYZ_ENVIRONMENT !== "production") {
    throw new Error("COMPLYZ_ENVIRONMENT must be 'production'.")
  }
  if (env.POSTGRES_VOLUME_NAME !== "complyz_postgres_prod_data") {
    throw new Error("POSTGRES_VOLUME_NAME must preserve complyz_postgres_prod_data.")
  }
  if (env.REDIS_VOLUME_NAME !== "complyz_redis_prod_data") {
    throw new Error("REDIS_VOLUME_NAME must preserve complyz_redis_prod_data.")
  }
  if (env.DOMAIN === "localhost" || env.DOMAIN.includes("://")) {
    throw new Error("DOMAIN must be a production hostname without a URL scheme.")
  }
  if (env.DOMAIN !== "trycomplyz.com" || env.API_DOMAIN !== "api.trycomplyz.com") {
    throw new Error("Production domains must be trycomplyz.com and api.trycomplyz.com.")
  }
  if (env.JWT_SECRET.length < 32 || env.JWT_SECRET.includes("404E635266556A586E3272")) {
    throw new Error("JWT_SECRET is missing, too short, or uses the development default.")
  }
  if (
    env.MFA_ENCRYPTION_KEY.length < 32 ||
    env.MFA_ENCRYPTION_KEY.includes("0123456789abcdef")
  ) {
    throw new Error(
      "MFA_ENCRYPTION_KEY is missing, too short, or uses the development default."
    )
  }
  for (const [name, developmentValue] of [
    ["POSTGRES_PASSWORD", "complyz_dev_password"],
    ["REDIS_PASSWORD", "redis_dev_password"],
  ]) {
    if (env[name].length < 12 || env[name] === developmentValue) {
      throw new Error(`${name} is too short or uses its development default.`)
    }
  }
  if (env.COMPLYZ_BOOTSTRAP_ENABLED === "true") {
    throw new Error(
      "COMPLYZ_BOOTSTRAP_ENABLED must be false for a production deployment."
    )
  }
  const origins = (env.CORS_ALLOWED_ORIGINS ?? "")
    .split(",")
    .map((origin) => origin.trim())
  if (
    origins.length === 0 ||
    origins.some((origin) => !origin || /localhost|127\.0\.0\.1|\*/i.test(origin))
  ) {
    throw new Error("CORS_ALLOWED_ORIGINS must contain only explicit production origins.")
  }
  if (!origins.includes("https://trycomplyz.com")) {
    throw new Error("CORS_ALLOWED_ORIGINS must allow https://trycomplyz.com.")
  }
  if (env.NEXT_PUBLIC_API_URL !== "https://api.trycomplyz.com/api/v1") {
    throw new Error("NEXT_PUBLIC_API_URL must use https://api.trycomplyz.com/api/v1.")
  }
}

function loadEnvironment() {
  try {
    process.loadEnvFile(`${root}.env`)
  } catch (error) {
    if (error.code !== "ENOENT") throw error
  }
}

function run(args) {
  return new Promise((resolve, reject) => {
    const child = spawn("docker", args, {
      cwd: root,
      stdio: "inherit",
      env: process.env,
    })
    child.once("error", reject)
    child.once("close", (code, signal) => {
      if (code === 0) resolve()
      else
        reject(
          new Error(
            `docker ${args.slice(0, 3).join(" ")} failed (${signal ?? `exit ${code}`}).`
          )
        )
    })
  })
}

function composeArgs(...args) {
  return [
    "compose",
    "-f",
    `${root}docker-compose.yml`,
    "--profile",
    "production",
    ...args,
  ]
}

async function waitForHealthy(container, attempts = 40, intervalMs = 3000) {
  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    const result = await new Promise((resolve, reject) => {
      const child = spawn(
        "docker",
        ["inspect", "--format={{.State.Health.Status}}", container],
        { cwd: root, stdio: ["ignore", "pipe", "ignore"], env: process.env }
      )
      let stdout = ""
      child.stdout.setEncoding("utf8")
      child.stdout.on("data", (chunk) => (stdout += chunk))
      child.once("error", reject)
      child.once("close", (code) => resolve(code === 0 ? stdout.trim() : "unavailable"))
    })
    if (result === "healthy") {
      console.log(`${container} is healthy.`)
      return
    }
    if (attempt < attempts)
      await new Promise((resolve) => setTimeout(resolve, intervalMs))
  }
  throw new Error(`${container} did not become healthy within ${attempts} checks.`)
}

async function main() {
  const options = parseArguments(process.argv.slice(2))
  if (options.help) {
    console.log(usage)
    return
  }
  loadEnvironment()
  validateDeploymentEnvironment(process.env)

  await run(["info"])
  await run(composeArgs("config", "--quiet"))
  if (options.down) {
    console.warn("Stopping production services. Named data volumes will be preserved.")
    await run(composeArgs("down"))
    return
  }

  const buildArgs = ["up", "-d", "--build"]
  if (options.noCache) buildArgs.push("--no-cache")
  await run(composeArgs(...buildArgs))
  for (const container of [
    "complyz-postgres",
    "complyz-redis",
    "complyz-api",
    "complyz-ui",
    "complyz-caddy",
  ]) {
    await waitForHealthy(container)
  }
  console.log(`Production deployment is healthy at https://${process.env.DOMAIN}.`)
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((error) => {
    console.error(`Production deployment failed: ${error.message}`)
    process.exitCode = 1
  })
}
