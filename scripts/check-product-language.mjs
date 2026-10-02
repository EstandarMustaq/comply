import { readdir, readFile } from "node:fs/promises"
import { relative, resolve, sep } from "node:path"
import { fileURLToPath } from "node:url"

const root = fileURLToPath(new URL("../", import.meta.url))
const targets = [
  "apps/api/src/main/java",
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
    const contents = await readFile(file, "utf8")
    for (const [index, line] of contents.split(/\r?\n/).entries()) {
      if (citationPattern.test(line)) {
        findings.push(`${relative(root, file).split(sep).join("/")}:${index + 1}:${line}`)
      }
    }
  }

  if (findings.length > 0) {
    console.error(findings.join("\n"))
    throw new Error("An external citation was found in an application surface.")
  }
  console.log("Application source citation check passed.")
}

main().catch((error) => {
  console.error(error.message)
  process.exitCode = 1
})
