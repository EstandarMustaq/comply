import { describe, expect, it, vi } from "vitest"
import { api } from "@/lib/api-client"

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  })
}

describe("api client", () => {
  it("posts credentials and stores the access token", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        jsonResponse({ accessToken: "access-token", refreshToken: "refresh-token" })
      )
    vi.stubGlobal("fetch", fetchMock)
    await api.login("officer@fincomp.test", "secret")
    expect(fetchMock).toHaveBeenCalledWith(
      "http://grc-engine:8080/api/auth/login",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          email: "officer@fincomp.test",
          password: "secret",
          passwordHash: "secret",
        }),
      })
    )
    expect(localStorage.getItem("fincomp_token")).toBe("access-token")
    expect(localStorage.getItem("fincomp_refresh_token")).toBe("refresh-token")
  })

  it("refreshes a rejected request once and retries with the new token", async () => {
    localStorage.setItem("fincomp_token", "expired-token")
    localStorage.setItem("fincomp_refresh_token", "refresh-token")
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ message: "expired" }, 401))
      .mockResolvedValueOnce(jsonResponse({ accessToken: "new-token" }))
      .mockResolvedValueOnce(jsonResponse({ content: [] }))
    vi.stubGlobal("fetch", fetchMock)
    await expect(api.getClients()).resolves.toEqual([])
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "http://grc-engine:8080/api/auth/refresh",
      expect.objectContaining({ method: "POST" })
    )
    expect(fetchMock).toHaveBeenLastCalledWith(
      "http://grc-engine:8080/api/clients?page=0&size=50",
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer new-token" }),
      })
    )
  })

  it("uploads files as form data and maps the preview response", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        batchId: "batch-1",
        fileName: "transactions.csv",
        format: "CSV",
        headers: ["amount"],
        previewRows: [["100"]],
        suggestedMapping: { amount: "amount" },
      })
    )
    vi.stubGlobal("fetch", fetchMock)
    const batch = await api.uploadBatch(new File(["amount\\n100"], "transactions.csv"))
    const [, options] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(options.body).toBeInstanceOf(FormData)
    expect((options.headers as Record<string, string>)["Content-Type"]).toBeUndefined()
    expect(batch).toMatchObject({ id: "batch-1", status: "UPLOADED" })
  })

  it("maps backend clients to the design-system model", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        content: [
          {
            id: "client-1",
            fullName: "Ada Lovelace",
            idDocumentType: "BI",
            idDocumentNumber: "123",
            dueDiligenceRegime: "SIMPLIFIED",
            riskRating: "HIGH",
            pep: true,
            createdAt: "2026-09-19T00:00:00Z",
          },
        ],
      })
    )
    vi.stubGlobal("fetch", fetchMock)
    await expect(api.getClients()).resolves.toEqual([
      expect.objectContaining({
        id: "client-1",
        documentNumber: "123",
        regime: "SIMPLIFICADO",
        riskRating: "ALTO",
        isPep: true,
      }),
    ])
  })
})
