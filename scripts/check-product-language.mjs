import { readdir, readFile } from "node:fs/promises"
import { relative, resolve, sep } from "node:path"
import { fileURLToPath } from "node:url"

const root = fileURLToPath(new URL("../", import.meta.url))
// Preserve V1–V20 as immutable history; their legacy literals are not current product copy.
const lastImmutableMigrationVersion = 20
const migrationPath = "apps/api/src/main/resources/db/migration"
const targets = [
  "apps/api/src/main/java",
  migrationPath,
  "apps/ui/app",
  "apps/ui/components",
  "apps/ui/lib",
]
const citationPattern = /[0-9]{1,3}\/20[0-9]{2}|[0-9]{1,3}\([0-9]{1,3}\)/
const ignoredDirectories = new Set(["node_modules", ".next"])

async function collectFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true })
  const paths = await Promise.all(
    entries.map(async (entry) => {
      const path = resolve(directory, entry.name)
      if (entry.isDirectory()) {
        if (ignoredDirectories.has(entry.name)) return []
        return collectFiles(path)
      }
      return entry.isFile() ? [path] : []
    })
  )
  return paths.flat()
}

async function main() {
  const files = []
  for (const target of targets) {
    const directory = resolve(root, target)
    try {
      files.push(...(await collectFiles(directory)))
    } catch (error) {
      if (error.code === "ENOENT") {
        throw new Error(`Product-language check target does not exist: ${target}`, {
          cause: error,
        })
      }
      throw error
    }
  }

  const findings = []
  for (const file of files.sort()) {
    const relativePath = relative(root, file).split(sep).join("/")
    const migration = relativePath.match(
      /^apps\/api\/src\/main\/resources\/db\/migration\/V(\d+)__/
    )
    if (migration && Number(migration[1]) <= lastImmutableMigrationVersion) continue

    const contents = await readFile(file, "utf8")
    for (const [index, line] of contents.split(/\r?\n/).entries()) {
      if (citationPattern.test(line)) {
        findings.push(`${relative(root, file).split(sep).join("/")}:${index + 1}:${line}`)
      }
    }
  }

  if (findings.length > 0) {
    console.error(findings.join("\n"))
    throw new Error(
      "An external citation was found in application source or a post-V20 migration."
    )
  }
  console.log("Application and post-V20 migration citation check passed.")
}

main().catch((error) => {
  console.error(error.message)
  process.exitCode = 1
})
