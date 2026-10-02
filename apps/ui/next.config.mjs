import { fileURLToPath } from "node:url"

/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",
  outputFileTracingRoot: fileURLToPath(new URL("../..", import.meta.url)),
  allowedDevOrigins: ["192.168.8.100"],
  agentRules: false,
}

export default nextConfig
