import { spawn, spawnSync } from "node:child_process"
import { fileURLToPath } from "node:url"

const root = fileURLToPath(new URL("../", import.meta.url))
const isWindows = process.platform === "win32"
const pnpm = isWindows ? "pnpm.cmd" : "pnpm"
const gradlew = isWindows ? "gradlew.bat" : "./gradlew"
const [command = "help", ...rawArgs] = process.argv.slice(2)
const args = rawArgs[0] === "--" ? rawArgs.slice(1) : rawArgs

try {
  process.loadEnvFile(`${root}.env`)
} catch (error) {
  if (error.code !== "ENOENT") throw error
}

function run(executable, executableArgs, options = {}) {
  const result = spawnSync(executable, executableArgs, {
    cwd: root,
    stdio: "inherit",
    ...options,
  })
  if (result.error) throw result.error
  if (result.status !== 0) process.exit(result.status ?? 1)
}

function runPnpm(...pnpmArgs) {
  run(pnpm, pnpmArgs)
}

function runGradle(...gradleArgs) {
  run(gradlew, ["--no-daemon", ...gradleArgs])
}

function startDev() {
  run("docker", [
    "compose",
    "-f",
    `${root}docker-compose.yml`,
    "up",
    "-d",
    "postgres",
    "redis",
  ])
  const detached = !isWindows
  const children = [
    spawn(gradlew, ["--no-daemon", ":api:bootRun"], {
      cwd: root,
      stdio: "inherit",
      detached,
    }),
    spawn(pnpm, ["exec", "turbo", "run", "dev", "--filter=@comply/web"], {
      cwd: root,
      stdio: "inherit",
      detached,
    }),
  ]
  let stopping = false
  const stop = (signal) => {
    if (stopping) return
    stopping = true
    for (const child of children) {
      if (detached && child.pid) process.kill(-child.pid, signal)
      else child.kill(signal)
    }
  }
  process.on("SIGINT", () => stop("SIGINT"))
  process.on("SIGTERM", () => stop("SIGTERM"))
  for (const child of children) {
    child.on("error", (error) => {
      console.error(error)
      stop("SIGTERM")
      process.exitCode = 1
    })
    child.on("exit", (status) => {
      if (!stopping && status !== 0) {
        process.exitCode = status ?? 1
        stop("SIGTERM")
      }
    })
  }
}

switch (command) {
  case "install":
    runPnpm("install", "--frozen-lockfile")
    break
  case "dev":
    startDev()
    break
  case "build":
    runGradle(":api:bootJar")
    runPnpm("exec", "turbo", "run", "build")
    break
  case "test":
    runGradle(":api:test")
    runPnpm("exec", "turbo", "run", "test:unit", "test:contract")
    break
  case "check":
  case "quality":
    runGradle(":api:spotlessCheck")
    runGradle(":api:test")
    runPnpm("exec", "turbo", "run", "check", "typecheck", "test:unit")
    runPnpm("exec", "prettier", "--check", ".")
    run(process.execPath, [`${root}scripts/operations.test.mjs`])
    run("docker", [
      "compose",
      "-f",
      `${root}docker-compose.yml`,
      "--profile",
      "app",
      "--profile",
      "production",
      "config",
      "--quiet",
    ])
    run("git", ["-C", root, "diff", "--check"])
    break
  case "api:build":
    runGradle(":api:bootJar")
    break
  case "api:format":
    runGradle(":api:spotlessApply")
    break
  case "api:test":
    runGradle(":api:test")
    break
  case "api:run":
    runGradle(":api:bootRun", ...args)
    break
  case "migrate":
    runGradle(":api:check")
    break
  case "deploy":
    run(process.execPath, [`${root}scripts/deploy-production.mjs`, ...args])
    break
  case "backup":
    run(process.execPath, [`${root}scripts/backup-s3.mjs`, ...args])
    break
  case "keys":
    run(process.execPath, [`${root}scripts/generate-keys.mjs`, ...args])
    break
  case "clean":
    runGradle(":api:clean")
    run(process.execPath, [`${root}scripts/clean.mjs`])
    break
  case "help":
  case "--help":
  case "-h":
    console.log(
      "Usage: pnpm <install|dev|build|test|check|quality|api:build|api:test|api:run|api:openapi|compose:config|compose:up|compose:down|migrate|deploy|backup|keys|clean>"
    )
    break
  default:
    console.error(`Unknown workspace command: ${command}`)
    process.exit(2)
}
