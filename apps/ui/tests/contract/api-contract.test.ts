import { describe, expect, it } from "vitest"
import { apiContract } from "@/lib/api-client"

type OpenApiDocument = { paths?: Record<string, Record<string, unknown>> }
const openApiUrl = process.env.FINCOMP_OPENAPI_URL

describe.skipIf(!openApiUrl)("GRC engine API contract", () => {
  it("declares every method and route used by the design-system client", async () => {
    const response = await fetch(openApiUrl as string)
    expect(response.ok).toBe(true)
    const document = (await response.json()) as OpenApiDocument
    for (const [method, path] of apiContract) {
      expect(document.paths?.[path]?.[method]).toBeDefined()
    }
  })
})
