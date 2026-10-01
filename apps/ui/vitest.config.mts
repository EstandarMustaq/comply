import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

export default defineConfig({
  resolve: { alias: { "@": fileURLToPath(new URL("./", import.meta.url)) } },
  test: {
    projects: [
      { extends: true, test: { name: "unit", include: ["tests/unit/**/*.test.ts"], environment: "jsdom", setupFiles: ["tests/setup.ts"] } },
      { extends: true, test: { name: "contract", include: ["tests/contract/**/*.test.ts"], environment: "node" } },
    ],
  },
});
