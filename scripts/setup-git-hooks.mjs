import { chmodSync } from "node:fs"
import { spawnSync } from "node:child_process"
import { fileURLToPath } from "node:url"
import { join } from "node:path"

const root = fileURLToPath(new URL("../", import.meta.url))
const hooksPath = ".githooks"
const hookFiles = ["pre-commit", "commit-msg", "pre-push"]
const checkOnly = process.argv.includes("--check")

function git(args, { allowFailure = false } = {}) {
  const result = spawnSync("git", args, {
    cwd: root,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  })
  if (result.error) throw result.error
  if (result.status !== 0 && !allowFailure) {
    throw new Error(result.stderr.trim() || `git ${args.join(" ")} failed`)
  }
  return result.status === 0 ? result.stdout.trim() : ""
}

if (process.env.CI === "true" || process.env.CI === "1") {
  console.log("Skipping local Git hook installation in CI.")
  process.exit(0)
}

try {
  if (git(["rev-parse", "--show-toplevel"]) !== root.replace(/\/$/, "")) {
    throw new Error("Run this command from the Complyz Git worktree.")
  }

  for (const file of hookFiles) {
    chmodSync(join(root, hooksPath, file), 0o755)
  }

  const configuredPath = git(["config", "--local", "--get", "core.hooksPath"], {
    allowFailure: true,
  })
  if (checkOnly) {
    if (configuredPath !== hooksPath) {
      throw new Error(
        `Git hooks are not enabled. Run pnpm hooks:install (current: ${configuredPath || "unset"}).`
      )
    }
    console.log(`Git hooks are enabled from ${hooksPath}.`)
  } else {
    git(["config", "--local", "core.hooksPath", hooksPath])
    console.log(`Git hooks enabled from ${hooksPath}.`)
  }
} catch (error) {
  console.error(`Git hook setup failed: ${error.message}`)
  process.exit(1)
}
