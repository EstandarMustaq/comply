import { rm } from "node:fs/promises"
import { resolve } from "node:path"
import { fileURLToPath } from "node:url"

const root = fileURLToPath(new URL("../", import.meta.url))
const generatedPaths = [
  "apps/ui/.next",
  "apps/ui/out",
  "apps/ui/build",
  "packages/ui/test-results",
  "apps/ui/storybook-static",
  ".turbo",
  "build",
]

await Promise.all(
  generatedPaths.map((path) => rm(resolve(root, path), { recursive: true, force: true }))
)
