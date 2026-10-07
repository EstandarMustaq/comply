import { validateDeploymentEnvironment } from "./deploy-production.mjs"

try {
  validateDeploymentEnvironment(process.env)
} catch (error) {
  console.error(`Production Compose preflight failed: ${error.message}`)
  process.exitCode = 1
}
