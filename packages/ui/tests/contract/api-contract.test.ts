import { beforeAll, describe, expect, it } from "vitest"
import { apiContract } from "@/lib/api-client"

type Schema = {
  type?: string
  format?: string
  properties?: Record<string, Schema>
  required?: string[]
}
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
        parameters?: Array<{ name: string; in: string; required?: boolean }>
        requestBody?: {
          required?: boolean
          content?: Record<string, { schema?: Schema }>
        }
        responses?: Record<
          string,
          {
            content?: Record<string, { schema?: Schema }>
          }
        >
      }
    >
  >
  servers?: Array<{ url: string }>
}
const openApiUrl = process.env.COMPLYZ_OPENAPI_URL

describe.skipIf(!openApiUrl)("GRC engine API contract", () => {
  let document: OpenApiDocument
  beforeAll(async () => {
    const response = await fetch(openApiUrl as string)
    expect(response.ok).toBe(true)
    document = (await response.json()) as OpenApiDocument
  })

  it("declares every method and route used by the design-system client", () => {
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

  it("requires a multipart upload with a binary file", () => {
    const body = document.paths?.["/api/v1/import-batches"]?.post?.requestBody
    expect(body?.required).toBe(true)
    expect(Object.keys(body?.content ?? {})).toEqual(["multipart/form-data"])
    const schema = body?.content?.["multipart/form-data"]?.schema
    expect(schema?.required).toContain("file")
    expect(schema?.properties?.file).toMatchObject({ type: "string", format: "binary" })
  })

  it.each(["/api/v1/clients", "/api/v1/audit-log"])(
    "documents optional paging parameters for %s",
    (path) => {
      const parameters = document.paths?.[path]?.get?.parameters ?? []
      expect(parameters.some((p) => p.name === "pageable")).toBe(false)
      for (const name of ["page", "size", "sort"]) {
        expect(parameters.find((p) => p.name === name)).toMatchObject({
          in: "query",
          required: false,
        })
      }
    }
  )

  it.each([
    ["/api/v1/reports/exports/str/{id}.pdf", "application/pdf"],
    [
      "/api/v1/reports/exports/annual/{year}.xlsx",
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    ],
  ])("documents %s as a binary file with JSON errors", (path, mediaType) => {
    const responses = document.paths?.[path]?.get?.responses
    expect(Object.keys(responses?.["200"]?.content ?? {})).toEqual([mediaType])
    expect(responses?.["200"]?.content?.[mediaType]?.schema).toMatchObject({
      type: "string",
      format: "binary",
    })
    expect(responses?.default?.content?.["application/json"]).toBeDefined()
  })
})
