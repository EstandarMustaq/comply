import { writeFile } from "node:fs/promises"
import { resolve } from "node:path"
import { fileURLToPath } from "node:url"
import openapiTS, { astToString } from "openapi-typescript"
import prettier from "prettier"

const root = fileURLToPath(new URL("../", import.meta.url))
const url = process.env.COMPLYZ_OPENAPI_URL ?? "http://127.0.0.1:8080/v3/api-docs"
const response = await fetch(url)

if (!response.ok) {
  throw new Error(`OpenAPI endpoint returned ${response.status}: ${url}`)
}

const document = await response.json()
if (
  typeof document.openapi !== "string" ||
  !document.paths ||
  typeof document.paths !== "object"
) {
  throw new Error("Springdoc returned an invalid OpenAPI document.")
}

const methods = new Set([
  "get",
  "put",
  "post",
  "delete",
  "options",
  "head",
  "patch",
  "trace",
])
const routes = Object.entries(document.paths)
  .flatMap(([path, pathItem]) =>
    Object.keys(pathItem)
      .filter((method) => methods.has(method))
      .map((method) => [method, path])
  )
  .sort(([leftMethod, leftPath], [rightMethod, rightPath]) =>
    `${leftPath}:${leftMethod}`.localeCompare(`${rightPath}:${rightMethod}`)
  )

if (routes.length === 0) {
  throw new Error("Springdoc returned no API operations.")
}

const generated = astToString(await openapiTS(document))
const clientDirectory = resolve(root, "packages/api-clients")
const generatedPath = resolve(clientDirectory, "src/generated.ts")
const prettierOptions = (await prettier.resolveConfig(generatedPath)) ?? {}
const formattedGenerated = await prettier.format(`${generated}\n`, {
  ...prettierOptions,
  filepath: generatedPath,
})

await Promise.all([
  writeFile(
    resolve(clientDirectory, "openapi.json"),
    `${JSON.stringify(document, null, 2)}\n`
  ),
  writeFile(generatedPath, formattedGenerated),
])

console.log(
  `Generated API client types from Springdoc (${routes.length} operations found)`
)
