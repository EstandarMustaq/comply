import { readdir, readFile } from "node:fs/promises"
import { relative, resolve, sep } from "node:path"
import { fileURLToPath } from "node:url"

const root = fileURLToPath(new URL("../", import.meta.url))
// Preserve V1–V20 as immutable history; their legacy literals are not current product copy.
const lastImmutableMigrationVersion = 20
const migrationPath = "apps/api/src/main/resources/db/migration"
const citationTargets = [
  "apps/api/src/main/java",
  migrationPath,
  "apps/ui/app",
  "apps/ui/components",
  "apps/ui/lib",
]
const productCopyTargets = [
  "apps/api/src/main/java",
  migrationPath,
  "apps/ui/app",
  "apps/ui/components",
  "apps/ui/lib",
  "packages/api-clients",
  "packages/config",
  "docs",
  "tasks",
  ".github",
  ".cursor",
  ".agents/skills",
]
const explicitProductFiles = [
  "README.md",
  "ROADMAP.md",
  "GLOSSARY.md",
  "AGENTS.md",
  "CLAUDE.md",
  "apps/ui/README.md",
  "apps/api/README.md",
]
const textExtensions = new Set([
  ".java",
  ".json",
  ".md",
  ".mdc",
  ".mdx",
  ".mjs",
  ".ts",
  ".tsx",
  ".yaml",
  ".yml",
])
const citationPattern = /[0-9]{1,3}\/20[0-9]{2}|[0-9]{1,3}\([0-9]{1,3}\)/
const accentedWordPattern = /\b[\p{L}]*[áàâãéêíìóôõúüç][\p{L}]*\b/iu
const PortugueseProductTerms =
  /\b(?:nao|não|nenhum(?:a|as|os)?|obrigatori[oa]s?|inv[aá]lid[oa]s?|cliente|clientes|utilizador(?:es)?|ficheiro(?:s)?|relat[óo]ri(?:o|os)|transaç(?:ão|ões)|transacç(?:ão|ões)|submet(?:er|ido|ida|idos|idas)|submissão|organizaç(?:ão|ões)|permissão|configuraç(?:ão|ões)|definiç(?:ão|ões)|importaç(?:ão|ões)|sanções|ação|acção|seleccionar|seleção|selecção|concluído|pendente|responsável|instituiç(?:ão|ões)|titular|declaraç(?:ão|ões)|registo|revisão|análise|montante|ausente|positivo|futuro|erro ao|sucesso|actividade|actualizar|desconhecido|desconhecida|nunca|realizad[oa])\b/iu
const ignoredDirectories = new Set([
  "node_modules",
  ".next",
  "build",
  "dist",
  "reference",
  "data",
])

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

function normalizedRelativePath(path) {
  return relative(root, path).split(sep).join("/")
}

function isProductLanguageFile(path) {
  const relativePath = normalizedRelativePath(path)
  const extension = relative(root, path).slice(relative(root, path).lastIndexOf("."))
  if (!textExtensions.has(extension)) return false
  if (relativePath.startsWith(".agents/skills/")) {
    return relativePath.endsWith("/SKILL.md")
  }
  if (relativePath.includes("/src/test/") || relativePath.includes("/tests/")) {
    return false
  }
  return true
}

function isExpectedNonCopyLiteral(path, line) {
  const relativePath = normalizedRelativePath(path)

  if (
    relativePath ===
      "apps/api/src/main/java/com/complyz/ingest/service/ImportService.java" &&
    line.includes('"DEPÓSITO"')
  ) {
    return true
  }
  // Preserve the configured legal institution name as a proper noun.
  return (
    relativePath ===
      "apps/api/src/main/java/com/complyz/flags/controller/FlagController.java" &&
    line.includes("Complyz Microfinanças SARL")
  )
}

async function collectTargets(targets, { required = true } = {}) {
  const files = []
  for (const target of targets) {
    const directory = resolve(root, target)
    try {
      files.push(...(await collectFiles(directory)))
    } catch (error) {
      if (error.code === "ENOENT" && !required) continue
      if (error.code === "ENOENT") {
        throw new Error(`Product-language check target does not exist: ${target}`, {
          cause: error,
        })
      }
      throw error
    }
  }
  return files
}

async function checkCitations() {
  const findings = []
  const files = await collectTargets(citationTargets)
  for (const file of files.sort()) {
    const relativePath = normalizedRelativePath(file)
    const migration = relativePath.match(
      /^apps\/api\/src\/main\/resources\/db\/migration\/V(\d+)__/
    )
    if (migration && Number(migration[1]) <= lastImmutableMigrationVersion) continue

    const contents = await readFile(file, "utf8")
    for (const [index, line] of contents.split(/\r?\n/).entries()) {
      if (citationPattern.test(line)) {
        findings.push(`${relativePath}:${index + 1}: external citation`)
      }
    }
  }
  return findings
}

async function checkProductCopy() {
  const targetFiles = await collectTargets(productCopyTargets, { required: false })
  const explicitFiles = explicitProductFiles.map((path) => resolve(root, path))
  const files = [...new Set([...targetFiles, ...explicitFiles])]
    .filter(isProductLanguageFile)
    .sort()
  const findings = []

  for (const file of files) {
    const relativePath = normalizedRelativePath(file)
    const migration = relativePath.match(
      /^apps\/api\/src\/main\/resources\/db\/migration\/V(\d+)__/
    )
    if (migration && Number(migration[1]) <= lastImmutableMigrationVersion) continue

    const contents = await readFile(file, "utf8")
    let insideLegacyHeaderAliases = false
    for (const [index, line] of contents.split(/\r?\n/).entries()) {
      const startsLegacyHeaderAliases =
        relativePath ===
          "apps/api/src/main/java/com/complyz/ingest/service/ColumnMappingService.java" &&
        line.includes("matches(norm,")
      if (startsLegacyHeaderAliases || insideLegacyHeaderAliases) {
        // Keep existing Portuguese bank-file aliases as input compatibility, not product copy.
        insideLegacyHeaderAliases = !line.includes(")) {")
        continue
      }
      if (isExpectedNonCopyLiteral(file, line)) continue
      if (accentedWordPattern.test(line) || PortugueseProductTerms.test(line)) {
        findings.push(`${relativePath}:${index + 1}: ${line.trim()}`)
      }
    }
  }
  return findings
}

async function main() {
  const [citationFindings, languageFindings] = await Promise.all([
    checkCitations(),
    checkProductCopy(),
  ])
  const findings = [...citationFindings, ...languageFindings]
  if (findings.length > 0) {
    console.error(findings.join("\n"))
    throw new Error(
      "Product-language check failed. Use clear, direct English for product copy and public documentation."
    )
  }
  console.log(
    "Product-language check passed: product copy and first-party documentation use English; citation checks passed."
  )
}

main().catch((error) => {
  console.error(error.message)
  process.exitCode = 1
})
