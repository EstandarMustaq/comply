import { fileURLToPath } from "node:url"

const nextConfig = {
  output: "standalone",
  outputFileTracingRoot: fileURLToPath(new URL("../..", import.meta.url)),
  allowedDevOrigins: ["192.168.8.100"],
  agentRules: false,
}

export default nextConfig
