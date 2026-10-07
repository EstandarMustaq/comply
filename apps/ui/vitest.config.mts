import { fileURLToPath } from "node:url"
import { defineConfig } from "vitest/config"

export default defineConfig({
  // Next.js preserves JSX for its compiler; component tests need Vite to transform it.
  oxc: { jsx: { runtime: "automatic" } },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./", import.meta.url)),
      "@complyz/api-clients": fileURLToPath(
        new URL("../../packages/api-clients/src/index.ts", import.meta.url)
      ),
    },
  },
  test: {
    projects: [
      {
        extends: true,
        test: {
          name: "unit",
          include: ["../../packages/ui/tests/unit/**/*.test.ts"],
          environment: "jsdom",
          setupFiles: ["../../packages/ui/tests/setup.ts"],
        },
      },
      {
        extends: true,
        test: {
          name: "contract",
          include: ["../../packages/ui/tests/contract/**/*.test.ts"],
          environment: "node",
        },
      },
    ],
  },
})
