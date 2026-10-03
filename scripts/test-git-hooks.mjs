import { chmod, cp, mkdtemp, mkdir, readFile, rm, writeFile } from "node:fs/promises"
import { spawnSync } from "node:child_process"
import { tmpdir } from "node:os"
import { join } from "node:path"
import { fileURLToPath } from "node:url"

const sourceRoot = fileURLToPath(new URL("../", import.meta.url))
const sandbox = await mkdtemp(join(tmpdir(), "complyz-git-hooks-"))
const env = {
  ...process.env,
  CI: "false",
  GIT_AUTHOR_NAME: "Complyz Hook Test",
  GIT_AUTHOR_EMAIL: "hooks@example.invalid",
  GIT_COMMITTER_NAME: "Complyz Hook Test",
  GIT_COMMITTER_EMAIL: "hooks@example.invalid",
}
let assertions = 0

function command(executable, args, options = {}) {
  const result = spawnSync(executable, args, {
    cwd: sandbox,
    env,
    encoding: "utf8",
    ...options,
  })
  if (result.error) throw result.error
  return result
}

function expect(condition, message) {
  assertions += 1
  if (!condition) throw new Error(message)
}

function git(...args) {
  const result = command("git", args)
  if (result.status !== 0)
    throw new Error(result.stderr || `git ${args.join(" ")} failed`)
  return result.stdout.trim()
}

function runHook(name, ...args) {
  return command(process.execPath, ["scripts/git-hooks.mjs", name, ...args])
}

function commit(message) {
  git("commit", "-m", message)
  return git("rev-parse", "HEAD")
}

async function write(relativePath, contents) {
  const path = join(sandbox, relativePath)
  await mkdir(join(path, ".."), { recursive: true })
  await writeFile(path, contents)
  return path
}

async function main() {
  const logPath = join(sandbox, "pnpm-calls.log")
  const binPath = join(sandbox, "bin")
  await mkdir(binPath)
  await write(
    "bin/pnpm",
    "#!/usr/bin/env node\nimport { appendFileSync } from 'node:fs'\nappendFileSync(process.env.HOOK_TEST_LOG, process.argv.slice(2).join(' ') + '\\n')\n"
  )
  await chmod(join(binPath, "pnpm"), 0o755)
  env.PATH = `${binPath}${process.platform === "win32" ? ";" : ":"}${process.env.PATH}`
  env.HOOK_TEST_LOG = logPath

  await mkdir(join(sandbox, "scripts"), { recursive: true })
  await mkdir(join(sandbox, ".githooks"), { recursive: true })
  await cp(
    join(sourceRoot, "scripts/git-hooks.mjs"),
    join(sandbox, "scripts/git-hooks.mjs")
  )
  await cp(
    join(sourceRoot, "scripts/setup-git-hooks.mjs"),
    join(sandbox, "scripts/setup-git-hooks.mjs")
  )
  await cp(join(sourceRoot, ".githooks"), join(sandbox, ".githooks"), { recursive: true })
  await write("README.md", "Complyz hook sandbox\n")
  await write(
    "skills-lock.json",
    JSON.stringify(
      { version: 1, skills: { "vendored-test-skill": { sourceType: "github" } } },
      null,
      2
    ) + "\n"
  )
  await write("apps/api/src/main/resources/db/migration/V1__baseline.sql", "SELECT 1;\n")
  git("init", "--quiet")
  git(
    "add",
    "README.md",
    "skills-lock.json",
    "apps/api/src/main/resources/db/migration/V1__baseline.sql"
  )
  commit("chore: initialize hook test sandbox")

  const setup = command(process.execPath, ["scripts/setup-git-hooks.mjs"])
  expect(setup.status === 0, `hook installation failed: ${setup.stderr}`)
  expect(
    git("config", "--local", "--get", "core.hooksPath") === ".githooks",
    "installer did not set core.hooksPath"
  )
  expect(
    command(process.execPath, ["scripts/setup-git-hooks.mjs"]).status === 0,
    "repeated hook installation failed"
  )
  expect(
    command(process.execPath, ["scripts/setup-git-hooks.mjs", "--check"]).status === 0,
    "hook check failed"
  )

  await write(
    ".agents/skills/vendored-test-skill/SKILL.md",
    "Upstream example with trailing whitespace   \n"
  )
  git("add", ".agents/skills/vendored-test-skill/SKILL.md")
  expect(
    runHook("pre-commit").status === 0,
    "whitespace in a locked external skill was rejected"
  )
  git("reset", "--quiet", "--", ".agents/skills/vendored-test-skill/SKILL.md")
  await rm(join(sandbox, ".agents/skills/vendored-test-skill"), {
    recursive: true,
    force: true,
  })

  await write(
    ".agents/skills/local-skill/SKILL.md",
    "Project-authored skill with trailing whitespace   \n"
  )
  git("add", ".agents/skills/local-skill/SKILL.md")
  expect(
    runHook("pre-commit").status !== 0,
    "whitespace in a project-authored skill was accepted"
  )
  git("reset", "--quiet", "--", ".agents/skills/local-skill/SKILL.md")
  await rm(join(sandbox, ".agents/skills/local-skill"), {
    recursive: true,
    force: true,
  })

  const invalidMessage = await write("invalid-message.txt", "updated file\n")
  const invalidMessageResult = runHook("commit-msg", invalidMessage)
  expect(invalidMessageResult.status !== 0, "invalid commit message was accepted")
  const validMessage = await write(
    "valid-message.txt",
    "fix(api): scope reports by organization\n"
  )
  expect(
    runHook("commit-msg", validMessage).status === 0,
    "valid Conventional Commit was rejected"
  )

  await write("apps/api/src/main/resources/db/migration/V1__baseline.sql", "SELECT 2;\n")
  git("add", "apps/api/src/main/resources/db/migration/V1__baseline.sql")
  expect(runHook("pre-commit").status !== 0, "editing an existing migration was accepted")
  await write("apps/api/src/main/resources/db/migration/V1__baseline.sql", "SELECT 1;\n")
  git("add", "apps/api/src/main/resources/db/migration/V1__baseline.sql")
  await write("apps/api/src/main/resources/db/migration/V2__add_index.sql", "SELECT 1;\n")
  git("add", "apps/api/src/main/resources/db/migration/V2__add_index.sql")
  expect(runHook("pre-commit").status === 0, "append-only migration was rejected")

  await write(".env.production.local", "PRIVATE_VALUE=not-a-real-secret\n")
  git("add", ".env.production.local")
  expect(runHook("pre-commit").status !== 0, "environment file was accepted")
  git("reset", "--quiet", "--", ".env.production.local")
  await rm(join(sandbox, ".env.production.local"))

  await write("credential.txt", ["AKIA", "1234567890ABCDEF"].join("") + "\n")
  git("add", "credential.txt")
  expect(
    runHook("pre-commit").status !== 0,
    "high-confidence credential pattern was accepted"
  )
  git("reset", "--quiet", "--", "credential.txt")
  await rm(join(sandbox, "credential.txt"))

  const base = git("rev-parse", "HEAD")
  await write("apps/api/src/main/java/com/complyz/Example.java", "package com.complyz;\n")
  git("add", "apps/api/src/main/java/com/complyz/Example.java")
  const apiHead = commit("test(api): exercise scoped push checks")
  const apiResult = command(process.execPath, ["scripts/git-hooks.mjs", "pre-push"], {
    input: `refs/heads/test ${apiHead} refs/heads/test ${base}\n`,
  })
  expect(apiResult.status === 0, `API pre-push check failed: ${apiResult.stderr}`)
  expect(
    (await readFile(logPath, "utf8")).includes("api:test"),
    "API pre-push did not run API tests"
  )

  const apiLog = await readFile(logPath, "utf8")
  await write("apps/ui/app/page.tsx", "export default function Page() { return null }\n")
  git("add", "apps/ui/app/page.tsx")
  const webHead = commit("test(ui): exercise scoped push checks")
  const webResult = command(process.execPath, ["scripts/git-hooks.mjs", "pre-push"], {
    input: `refs/heads/test ${webHead} refs/heads/test ${apiHead}\n`,
  })
  expect(webResult.status === 0, `web pre-push check failed: ${webResult.stderr}`)
  const allCalls = await readFile(logPath, "utf8")
  expect(
    allCalls.includes("check") && allCalls.includes("test:unit"),
    "web pre-push did not run workspace checks"
  )
  expect(allCalls.startsWith(apiLog), "pre-push command log changed unexpectedly")

  const fallbackResult = command(
    process.execPath,
    ["scripts/git-hooks.mjs", "pre-push"],
    {
      input: `refs/heads/test ${webHead} refs/heads/test ${"f".repeat(40)}\n`,
    }
  )
  expect(
    fallbackResult.status === 0,
    `pre-push fallback failed: ${fallbackResult.stderr}`
  )
  const fallbackCalls = (await readFile(logPath, "utf8")).slice(allCalls.length)
  expect(
    fallbackCalls.includes("api:test") &&
      fallbackCalls.includes("check") &&
      fallbackCalls.includes("test:unit"),
    "pre-push did not run broad checks when the remote base was unavailable"
  )
}

try {
  await main()
  console.log(`Git hook validation passed (${assertions} assertions).`)
} catch (error) {
  console.error(`Git hook validation failed: ${error.message}`)
  process.exitCode = 1
} finally {
  await rm(sandbox, { recursive: true, force: true })
}
