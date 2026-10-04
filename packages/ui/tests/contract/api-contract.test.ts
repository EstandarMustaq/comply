import { describe, expect, it } from "vitest"
import { apiContract } from "@/lib/api-client"

type OpenApiDocument = {
  components?: {
    schemas?: Record<string, { properties?: Record<string, unknown> }>
    securitySchemes?: Record<string, unknown>
  }
  paths?: Record<
    string,
    Record<
      string,
      {
        responses?: Record<
          string,
          {
            content?: Record<
              string,
              { schema?: { properties?: Record<string, unknown> } }
            >
          }
        >
      }
    >
  >
  servers?: Array<{ url: string }>
}
const openApiUrl = process.env.COMPLYZ_OPENAPI_URL

describe.skipIf(!openApiUrl)("GRC engine API contract", () => {
  it("declares every method and route used by the design-system client", async () => {
    const response = await fetch(openApiUrl as string)
    expect(response.ok).toBe(true)
    const document = (await response.json()) as OpenApiDocument
    expect(
      document.servers?.some((server) => server.url === "https://api.trycomplyz.com")
    ).toBe(true)
    expect(document.components?.schemas?.ApiErrorEnvelope).toBeDefined()
    expect(document.components?.schemas?.ApiResponseMetadata).toBeDefined()
    expect(document.components?.schemas?.ApiResponseMeta).toBeUndefined()
    const systemConfigSchema = document.components?.schemas?.SystemConfigResponse
    expect(systemConfigSchema?.properties).toHaveProperty("key")
    expect(systemConfigSchema?.properties).toHaveProperty("value")
    expect(systemConfigSchema?.properties).not.toHaveProperty("updatedBy")
    expect(
      Object.keys(document.paths ?? {}).every((path) => path.startsWith("/api/v1/"))
    ).toBe(true)
    expect(document.components?.securitySchemes?.bearerAuth).toBeDefined()
    expect(
      document.paths?.["/api/v1/settings/users"]?.post?.responses?.["501"]
    ).toBeDefined()
    for (const [method, path] of apiContract) {
      expect(document.paths?.[path]?.[method]).toBeDefined()
      const operation = document.paths?.[path]?.[method]
      const success = Object.entries(operation?.responses ?? {}).find(([status]) =>
        status.startsWith("2")
      )
      const schema = success?.[1].content?.["application/json"]?.schema
      if (schema) {
        expect(schema.properties).toHaveProperty("data")
        expect(schema.properties).toHaveProperty("meta")
      }
    }
  })
})
