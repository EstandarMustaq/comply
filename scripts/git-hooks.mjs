import { existsSync, readFileSync } from "node:fs"
import { spawnSync } from "node:child_process"
import { fileURLToPath } from "node:url"

const root = fileURLToPath(new URL("../", import.meta.url))
const migrationDir = "apps/api/src/main/resources/db/migration"
const [hook, ...args] = process.argv.slice(2)

function gitOutput(gitArgs, { allowFailure = false } = {}) {
  const result = spawnSync("git", gitArgs, {
    cwd: root,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
    maxBuffer: 16 * 1024 * 1024,
  })
  if (result.error) throw result.error
  if (result.status !== 0 && !allowFailure) {
    throw new Error(result.stderr.trim() || `git ${gitArgs.join(" ")} failed`)
  }
  return result.status === 0 ? result.stdout : ""
}

function run(executable, commandArgs) {
  const result = spawnSync(executable, commandArgs, {
    cwd: root,
    stdio: "inherit",
  })
  if (result.error) throw result.error
  if (result.status !== 0) process.exit(result.status ?? 1)
}

function fail(message) {
  throw new Error(message)
}

function stagedPaths(filter = "ACMR") {
  return gitOutput(["diff", "--cached", "--name-only", `--diff-filter=${filter}`, "--"])
    .split("\n")
    .filter(Boolean)
}

function checkStagedSecrets() {
  const files = stagedPaths()
  const forbiddenEnvironmentFiles = files.filter(
    (path) => /(^|\/)\.env(?:\..*)?$/.test(path) && !/(^|\/)\.env\.example$/.test(path)
  )
  if (forbiddenEnvironmentFiles.length) {
    fail(
      `Environment files cannot be committed:\n${forbiddenEnvironmentFiles.join("\n")}`
    )
  }

  const privateKeyFiles = files.filter((path) => /\.(?:pem|key|p12|pfx|jks)$/i.test(path))
  if (privateKeyFiles.length) {
    fail(`Private-key files cannot be committed:\n${privateKeyFiles.join("\n")}`)
  }

  const stagedAdditions = gitOutput([
    "diff",
    "--cached",
    "--no-ext-diff",
    "--unified=0",
    "--",
  ])
    .split("\n")
    .filter((line) => line.startsWith("+") && !line.startsWith("+++"))
    .join("\n")
  const secretPatterns = [
    /-----BEGIN (?:RSA |OPENSSH |EC |DSA |PGP )?PRIVATE KEY-----/,
    /\bAKIA[0-9A-Z]{16}\b/,
    /\bgh[pousr]_[A-Za-z0-9_]{20,}\b/,
    /\bxox[baprs]-[0-9A-Za-z-]{20,}\b/,
  ]
  if (secretPatterns.some((pattern) => pattern.test(stagedAdditions))) {
    fail("A high-confidence credential pattern was found in staged changes.")
  }
}

function checkMigrationHistory() {
  const changedExisting = gitOutput([
    "diff",
    "--cached",
    "--name-only",
    "--diff-filter=MDTR",
    "--",
    migrationDir,
  ])
    .split("\n")
    .filter(Boolean)
  if (changedExisting.length) {
    fail(
      `Existing Flyway migrations are immutable; add a new version instead:\n${changedExisting.join("\n")}`
    )
  }

  const added = stagedPaths("A").filter((path) => path.startsWith(`${migrationDir}/`))
  if (!added.length) return

  const headFiles = gitOutput(
    ["ls-tree", "-r", "--name-only", "HEAD", "--", migrationDir],
    {
      allowFailure: true,
    }
  ).split("\n")
  const versionPattern = /\/V(\d+)__[^/]+\.sql$/
  const committedVersions = headFiles
    .map((path) => Number(path.match(versionPattern)?.[1]))
    .filter(Number.isFinite)
  const latestCommitted = Math.max(0, ...committedVersions)
  const addedVersions = added.map((path) => {
    const match = path.match(versionPattern)
    if (!match)
      fail(`Invalid Flyway migration filename: ${path}. Use V<n>__description.sql.`)
    return { path, version: Number(match[1]) }
  })
  const versions = addedVersions.map(({ version }) => version)
  if (new Set(versions).size !== versions.length) {
    fail("Staged Flyway migrations contain duplicate version numbers.")
  }
  const stale = addedVersions.filter(({ version }) => version <= latestCommitted)
  if (stale.length) {
    fail(
      `New Flyway versions must be higher than V${latestCommitted}:\n${stale.map(({ path }) => path).join("\n")}`
    )
  }
}

function checkProductLanguage(paths) {
  const productPaths = [
    "apps/api/src/main/java/",
    "apps/api/src/main/resources/db/migration/",
    "apps/ui/app/",
    "apps/ui/components/",
    "apps/ui/lib/",
    "docs/",
  ]
  if (paths.some((path) => productPaths.some((prefix) => path.startsWith(prefix)))) {
    run("pnpm", ["product-language:check"])
  }
}

function preCommit() {
  const paths = stagedPaths()
  if (!paths.length) return
  const diffCheck = spawnSync("git", ["diff", "--cached", "--check"], {
    cwd: root,
    stdio: "inherit",
  })
  if (diffCheck.error) throw diffCheck.error
  if (diffCheck.status !== 0) fail("Staged changes contain whitespace errors.")
  checkStagedSecrets()
  checkMigrationHistory()
  checkProductLanguage(paths)
}

function commitMessage(messagePath) {
  if (!messagePath || !existsSync(messagePath)) {
    fail("commit-msg hook requires a readable commit message file.")
  }
  const firstLine = readFileSync(messagePath, "utf8").split(/\r?\n/, 1)[0] ?? ""
  if (/^(?:Merge |Revert |fixup!|squash!)/.test(firstLine)) return
  const pattern =
    /^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([a-z0-9._/-]+\))?!?: .{3,72}$/
  if (!pattern.test(firstLine)) {
    fail(
      [
        "Invalid commit message.",
        "Use Conventional Commits: type(scope): summary",
        "Types: feat fix docs style refactor perf test build ci chore revert",
        "Example: feat(api): add sanctions list version endpoint",
      ].join("\n")
    )
  }
}

function changesForPush(input) {
  const changed = new Set()
  let missingBase = false
  const lines = input.split(/\r?\n/).filter(Boolean)
  for (const line of lines) {
    const [, localSha, , remoteSha] = line.trim().split(/\s+/)
    if (!localSha || /^0+$/.test(localSha)) continue

    let base =
      remoteSha && !/^0+$/.test(remoteSha) && commitExists(remoteSha) ? remoteSha : ""
    if (!base) {
      base = gitOutput(["merge-base", "origin/main", localSha], {
        allowFailure: true,
      }).trim()
    }
    if (base) {
      const result = spawnSync(
        "git",
        ["diff", "--name-only", `${base}..${localSha}`, "--"],
        {
          cwd: root,
          encoding: "utf8",
          stdio: ["ignore", "pipe", "pipe"],
        }
      )
      if (result.error) throw result.error
      if (result.status === 0) {
        for (const path of result.stdout.split("\n").filter(Boolean)) changed.add(path)
      } else {
        missingBase = true
      }
    } else {
      missingBase = true
    }
  }
  if (missingBase) {
    const localSha = lines
      .map((line) => line.trim().split(/\s+/)[1])
      .find((sha) => sha && !/^0+$/.test(sha))
    if (localSha) {
      const tracked = gitOutput(["ls-tree", "-r", "--name-only", localSha], {
        allowFailure: true,
      })
      for (const path of tracked.split("\n").filter(Boolean)) changed.add(path)
    }
  }
  return [...changed]
}

function commitExists(commit) {
  return Boolean(
    gitOutput(["rev-parse", "--verify", `${commit}^{commit}`], { allowFailure: true })
  )
}

async function prePush() {
  const input = await new Promise((resolve, reject) => {
    let value = ""
    process.stdin.setEncoding("utf8")
    process.stdin.on("data", (chunk) => {
      value += chunk
    })
    process.stdin.on("end", () => resolve(value))
    process.stdin.on("error", reject)
  })
  const paths = changesForPush(input)
  if (!paths.length) return

  const apiChanged = paths.some((path) => path.startsWith("apps/api/"))
  const webChanged = paths.some(
    (path) => path.startsWith("apps/ui/") || path.startsWith("packages/")
  )
  const centralConfigChanged = paths.some(
    (path) =>
      path.startsWith(".github/") ||
      path.startsWith("scripts/") ||
      [
        "package.json",
        "pnpm-lock.yaml",
        "pnpm-workspace.yaml",
        "turbo.json",
        "build.gradle.kts",
        "settings.gradle.kts",
        "Dockerfile",
        "docker-compose.yml",
      ].includes(path)
  )

  if (apiChanged || centralConfigChanged) run("pnpm", ["api:test"])
  if (webChanged || centralConfigChanged) {
    run("pnpm", ["check"])
    run("pnpm", ["test:unit"])
  }
  checkProductLanguage(paths)
  if (paths.some((path) => path === "docker-compose.yml" || path === "Dockerfile")) {
    run("pnpm", ["compose:config"])
  }
}

try {
  switch (hook) {
    case "pre-commit":
      preCommit()
      break
    case "commit-msg":
      commitMessage(args[0])
      break
    case "pre-push":
      await prePush()
      break
    default:
      fail("Usage: node scripts/git-hooks.mjs <pre-commit|commit-msg|pre-push>.")
  }
} catch (error) {
  console.error(`${hook ?? "git-hook"}: ${error.message}`)
  process.exit(1)
}
