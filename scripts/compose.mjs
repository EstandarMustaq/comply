import { spawnSync } from "node:child_process"
import { fileURLToPath } from "node:url"

const root = fileURLToPath(new URL("../", import.meta.url))
const command = process.argv[2] ?? "up"
const allowedCommands = new Set(["config", "up", "down", "logs", "ps", "pull", "build"])

if (!allowedCommands.has(command)) {
  console.error(`Unsupported compose command: ${command}`)
  process.exit(2)
}

const args = ["compose", "-f", `${root}docker-compose.yml`]
if (command === "config") {
  args.push("--profile", "app", "--profile", "production", "config", "--quiet")
} else {
  if (command === "up" && !process.argv.slice(3).includes("--profile")) {
    args.push("--profile", "app")
  }
  args.push(command, ...process.argv.slice(3))
}

const result = spawnSync("docker", args, { cwd: root, stdio: "inherit" })
if (result.error) throw result.error
process.exit(result.status ?? 1)
