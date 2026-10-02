import { defineConfig, devices } from "@playwright/test"
import { fileURLToPath } from "node:url"

const port = 3100
const baseURL = `http://127.0.0.1:${port}`
const uiRoot = fileURLToPath(new URL(".", import.meta.url))

export default defineConfig({
  testDir: "../../packages/ui/tests/e2e",
  fullyParallel: true,
  workers: process.env.CI ? 2 : 1,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    colorScheme: "light",
    locale: "pt-MZ",
    launchOptions: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
      ? { executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH }
      : undefined,
  },
  projects: [
    { name: "desktop", use: { ...devices["Desktop Chrome"] } },
    { name: "mobile", use: { ...devices["Pixel 7"] } },
  ],
  webServer: {
    command: `pnpm --dir ${uiRoot} dev --hostname 127.0.0.1 --port ${port}`,
    url: baseURL,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
    env: {
      NEXT_PUBLIC_API_URL: "/api",
      NEXT_PUBLIC_APP_NAME: "Complyz",
    },
  },
})
