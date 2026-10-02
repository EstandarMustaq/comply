import { expect, test, type Page, type Route } from "@playwright/test"
import AxeBuilder from "@axe-core/playwright"

const insights = {
  totalClients: 42,
  kycCompleteCount: 37,
  kycCompletenessPercentage: 88,
  normalRegimeCount: 12,
  simplifiedRegimeCount: 30,
  sanctionsMatchCount: 1,
  pendingFlagsCount: 3,
  overdueSlaCount: 1,
  pepReviewDueCount: 2,
  retentionExpiringCount: 0,
  totalTransactionsCount: 1840,
  reportedStrsCount: 2,
  complianceScorePercent: 92,
}

async function fulfillApi(route: Route) {
  const pathname = new URL(route.request().url()).pathname
  const body = pathname.endsWith("/insights")
    ? insights
    : pathname.endsWith("/auth/me")
      ? {
          id: "user-1",
          fullName: "Oficial de Conformidade",
          email: "oficial@complyz.test",
          role: "COMPLIANCE_OFFICER",
          mfaEnabled: true,
        }
      : pathname.endsWith("/clients")
        ? { content: [] }
        : pathname.endsWith("/flags")
          ? []
          : pathname.endsWith("/reports")
            ? []
            : pathname.endsWith("/settings/threshold-rules")
              ? []
              : pathname.endsWith("/settings/audit-logs")
                ? []
                : pathname.endsWith("/import-batches")
                  ? []
                  : {}

  await route.fulfill({ contentType: "application/json", body: JSON.stringify(body) })
}

async function openDashboard(page: Page) {
  await page.route("**/api/**", fulfillApi)
  await page.addInitScript(() => {
    localStorage.setItem("complyz_token", "e2e-token")
  })
  await page.goto("/", { waitUntil: "domcontentloaded" })
  await expect(page.getByRole("heading", { name: "Visão geral" })).toBeVisible()
}

test("navega pelo shell, comandos e atalhos de teclado", async ({ page }, testInfo) => {
  test.skip(
    testInfo.project.name !== "desktop",
    "keyboard shell flow is covered on desktop"
  )
  await openDashboard(page)

  await page.getByRole("button", { name: /Pesquisar/ }).click()
  await expect(page.getByRole("dialog", { name: "Pesquisar" })).toBeVisible()
  await page.getByPlaceholder("Pesquisar...").fill("Indicadores")
  await page.getByRole("button", { name: "Indicadores" }).click()
  await expect(page).toHaveURL(/\/insights$/)
  await expect(page.getByRole("heading", { name: "Indicadores" })).toBeVisible()

  await page.keyboard.press("Control+\\")
  await expect(page.getByLabel("Expandir sidebar")).toBeVisible()
  await page.keyboard.press("Control+\\")
  await expect(page.getByLabel("Colapsar sidebar")).toBeVisible()
})

test("respeita a estrutura acessível e o estado visual desktop", async ({
  page,
}, testInfo) => {
  test.skip(testInfo.project.name !== "desktop", "snapshot desktop only")
  test.slow()
  await openDashboard(page)

  const accessibility = await new AxeBuilder({ page })
    .include('[data-slot="app-shell"]')
    .analyze()
  expect(accessibility.violations).toEqual([])

  await expect(page).toHaveScreenshot("dashboard-desktop.png", {
    fullPage: true,
    animations: "disabled",
    maxDiffPixelRatio: 0.05,
  })
})

test("abre a navegação móvel e mantém a interface enquadrada", async ({
  page,
}, testInfo) => {
  test.skip(testInfo.project.name !== "mobile", "snapshot mobile only")
  await openDashboard(page)
  await page.getByLabel("Abrir menu").click()
  await expect(
    page.getByRole("navigation").getByText("Monitorização", { exact: true })
  ).toBeVisible()
  await expect(page).toHaveScreenshot("dashboard-mobile-menu.png", {
    fullPage: true,
    animations: "disabled",
    maxDiffPixelRatio: 0.05,
  })
})
