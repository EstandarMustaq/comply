import { env } from "./env"
import {
  type AuthResponse,
  type MfaEnrollResponse,
  type InsightsData,
  type Client,
  type Flag,
  type ImportBatch,
  type SuspiciousActivityReport,
  type CurrencyTransactionReport,
  type ThresholdRule,
  type AuditLog,
} from "./types"

function resolveApiBaseUrl(value: string): string {
  if (!value) return ""
  const baseUrl = value.replace(/\/+$/, "")
  if (baseUrl.endsWith("/api/v1")) return baseUrl
  if (baseUrl.endsWith("/api")) return `${baseUrl}/v1`
  return `${baseUrl}/api/v1`
}

const API_BASE_URL = resolveApiBaseUrl(env.NEXT_PUBLIC_API_URL)

// The contract test validates these routes against the running API schema.
export { apiContract } from "@complyz/api-clients"

export interface CurrentUser {
  id: string
  fullName: string
  email: string
  role: string
  mfaEnabled: boolean
}

class ApiClient {
  private getToken(): string | null {
    if (typeof window === "undefined") return null
    return localStorage.getItem("complyz_token")
  }

  public setToken(token: string) {
    if (typeof window !== "undefined") {
      localStorage.setItem("complyz_token", token)
    }
  }

  public clearToken() {
    if (typeof window !== "undefined") {
      localStorage.removeItem("complyz_token")
    }
  }

  public hasToken(): boolean {
    if (typeof window === "undefined") return false
    return !!localStorage.getItem("complyz_token")
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    if (!API_BASE_URL) {
      throw new Error("A variável NEXT_PUBLIC_API_URL não está configurada.")
    }
    const token = this.getToken()
    const headers: Record<string, string> = {
      ...(options.headers as Record<string, string>),
    }

    if (token && !headers["Authorization"]) {
      headers["Authorization"] = `Bearer ${token}`
    }

    if (!(options.body instanceof FormData) && !headers["Content-Type"]) {
      headers["Content-Type"] = "application/json"
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    })

    if (response.status === 401) {
      if (typeof window !== "undefined") {
        const refreshToken = localStorage.getItem("complyz_refresh_token")
        if (refreshToken && !endpoint.includes("/auth/refresh")) {
          try {
            const refreshed = await this.request<AuthResponse>("/auth/refresh", {
              method: "POST",
              body: JSON.stringify({ refreshToken }),
            })
            if (refreshed.accessToken) {
              this.setToken(refreshed.accessToken)
              if (refreshed.refreshToken)
                localStorage.setItem("complyz_refresh_token", refreshed.refreshToken)
              return this.request<T>(endpoint, options)
            }
          } catch {
            // Clear credentials below when rotation fails.
          }
        }
        this.clearToken()
        localStorage.removeItem("complyz_refresh_token")
        // Redirect to the external authentication service after session expiry.
        if (env.NEXT_PUBLIC_AUTH_URL) {
          window.location.href = env.NEXT_PUBLIC_AUTH_URL
        }
      }
      throw new Error("A sessão expirou. Inicie sessão novamente.")
    }

    if (!response.ok) {
      let errorMsg = `O pedido falhou: ${response.status} ${response.statusText}`
      try {
        const errorData = await response.json()
        if (errorData.error?.message) errorMsg = errorData.error.message
        else if (errorData.message) errorMsg = errorData.message
        else if (typeof errorData === "string") errorMsg = errorData
      } catch {
        // fallback to status text
      }
      throw new Error(errorMsg)
    }

    const contentType = response.headers.get("content-type")
    if (contentType && contentType.includes("application/json")) {
      const body: unknown = await response.json()
      if (typeof body === "object" && body !== null && "data" in body && "meta" in body) {
        return body.data as T
      }
      return body as T
    }
    return response.text() as unknown as T
  }

  // --- Auth ---
  async login(email: string, password: string): Promise<AuthResponse> {
    const res = await this.request<AuthResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password, passwordHash: password }),
    })
    const token = res.accessToken || res.token
    if (token) {
      this.setToken(token)
    }
    if (res.refreshToken && typeof window !== "undefined")
      localStorage.setItem("complyz_refresh_token", res.refreshToken)
    return res
  }

  async verifyMfa(challengeToken: string, code: string): Promise<AuthResponse> {
    const res = await this.request<AuthResponse>("/auth/mfa/verify", {
      method: "POST",
      body: JSON.stringify({
        challengeToken,
        tempToken: challengeToken,
        code,
        totpCode: code,
      }),
    })
    const token = res.accessToken || res.token
    if (token) {
      this.setToken(token)
    }
    if (res.refreshToken && typeof window !== "undefined")
      localStorage.setItem("complyz_refresh_token", res.refreshToken)
    return res
  }

  async enrollMfa(enrollmentToken?: string): Promise<MfaEnrollResponse> {
    return this.request<MfaEnrollResponse>("/auth/mfa/enroll", {
      method: "POST",
      body: JSON.stringify({ enrollmentToken, tempToken: enrollmentToken }),
    })
  }

  async confirmMfa(code: string, enrollmentToken?: string): Promise<AuthResponse> {
    const res = await this.request<AuthResponse>("/auth/mfa/confirm", {
      method: "POST",
      body: JSON.stringify({ code, enrollmentToken }),
    })
    const token = res.accessToken || res.token
    if (token) {
      this.setToken(token)
    }
    if (res.refreshToken && typeof window !== "undefined")
      localStorage.setItem("complyz_refresh_token", res.refreshToken)
    return res
  }

  async confirmMfaEnroll(tempToken: string, totpCode: string): Promise<AuthResponse> {
    return this.confirmMfa(totpCode, tempToken)
  }

  async logout(): Promise<void> {
    try {
      await this.request("/auth/logout", { method: "POST" })
    } catch {
      // Best-effort: always clear the local token
    } finally {
      this.clearToken()
      if (typeof window !== "undefined") localStorage.removeItem("complyz_refresh_token")
    }
  }

  // --- Current User (from JWT via GRC engine) ---
  async getMe(): Promise<CurrentUser> {
    return this.request<CurrentUser>("/auth/me")
  }

  // --- Insights & Dashboard ---
  async getInsights(): Promise<InsightsData> {
    return this.request<InsightsData>("/insights/summary").then((data) => ({
      ...data,
      simplifiedRegimeClients: data.simplifiedRegimeCount,
      normalRegimeClients: data.normalRegimeCount,
      totalTransactions: data.totalTransactionsCount,
      urgentSanctionsFlagsCount: data.sanctionsMatchCount,
    }))
  }

  // --- Clients & KYC ---
  async getClients(page = 0, size = 50, search?: string): Promise<Client[]> {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (search) params.set("search", search)
    return this.request<{ content: GrcEngineClient[] }>(`/clients?${params}`).then(
      (response) => response.content.map(mapClient)
    )
  }

  async getClientById(id: string): Promise<Client> {
    return this.request<GrcEngineClient>(`/clients/${id}`).then(mapClient)
  }

  async createClient(clientData: Partial<Client>): Promise<Client> {
    return this.request<GrcEngineClient>("/clients", {
      method: "POST",
      body: JSON.stringify({
        fullName: clientData.fullName,
        idDocumentType: clientData.idDocumentType ?? "BI",
        idDocumentNumber: clientData.idDocumentNumber ?? clientData.documentNumber,
        clientType: clientData.clientType ?? "INDIVIDUAL",
        nuib: clientData.nuib,
        dueDiligenceRegime:
          clientData.dueDiligenceRegime ??
          (clientData.regime === "SIMPLIFICADO" ? "SIMPLIFIED" : "NORMAL"),
        isPep: clientData.isPep ?? false,
        pepCategory: clientData.pepCategory,
        seniorManagementApproved: false,
      }),
    }).then(mapClient)
  }

  // --- Flags & Risk Queue ---
  async getFlags(status?: string, flagType?: string): Promise<Flag[]> {
    const params = new URLSearchParams()
    if (status) params.append("status", status)
    if (flagType) params.append("type", flagType)
    const queryString = params.toString() ? `?${params.toString()}` : ""
    return this.request<GrcEngineFlag[]>(`/flags${queryString}`).then((flags) =>
      flags.map(mapFlag)
    )
  }

  async reviewFlag(id: string, notes: string): Promise<Flag> {
    return this.request<GrcEngineFlag>(`/flags/${id}/review`, {
      method: "PATCH",
      body: JSON.stringify({
        note: notes,
        status: "RESOLVED",
      }),
    }).then(mapFlag)
  }

  async escalateFlagToStr(id: string, _notes: string): Promise<SuspiciousActivityReport> {
    const result = await this.request<{ strId: string }>(`/flags/${id}/escalate-to-str`, {
      method: "POST",
    })
    return this.getStrById(result.strId)
  }

  // --- Ingestion / Batches ---
  async uploadBatch(file: File): Promise<ImportBatch> {
    const formData = new FormData()
    formData.append("file", file)
    const response = await this.request<GrcEngineUploadPreview>("/import-batches", {
      method: "POST",
      body: formData,
    })
    return {
      id: response.batchId,
      fileName: response.fileName,
      fileType: response.format,
      format: response.format,
      status: "UPLOADED",
      totalRows: 0,
      processedRows: 0,
      errorRows: 0,
      createdAt: new Date().toISOString(),
      headers: response.headers,
      previewRows: response.previewRows,
      suggestedMapping: response.suggestedMapping,
      columnMapping: response.suggestedMapping,
    }
  }

  async confirmMapping(
    batchId: string,
    mapping: Record<string, string>
  ): Promise<Record<string, unknown>> {
    return this.request(`/import-batches/${batchId}/confirm-mapping`, {
      method: "POST",
      body: JSON.stringify(mapping),
    })
  }

  async getBatches(): Promise<ImportBatch[]> {
    return this.request<GrcEngineImportBatch[]>("/import-batches").then((batches) =>
      batches.map(mapImportBatch)
    )
  }

  async getBatchById(id: string): Promise<ImportBatch> {
    return this.request<GrcEngineImportBatch>(`/import-batches/${id}`).then(
      mapImportBatch
    )
  }

  // --- Reports (STR & CTR) ---
  async getStrs(): Promise<SuspiciousActivityReport[]> {
    return this.request<GrcEngineStr[]>("/reports/str").then((reports) =>
      reports.map(mapStr)
    )
  }

  async getStrById(id: string): Promise<SuspiciousActivityReport> {
    return this.request<GrcEngineStr>(`/reports/str/${id}`).then(mapStr)
  }

  async submitStr(
    id: string,
    submissionData: {
      suspicionGrounds?: string
      operationDetails?: string
      evidenceSummary?: string
      actionTaken?: string
      gifimProtocolNumber: string
    }
  ): Promise<SuspiciousActivityReport> {
    return this.request<GrcEngineStr>(`/reports/str/${id}/submit`, {
      method: "POST",
      body: JSON.stringify({
        gifimRegistrationNumber: submissionData.gifimProtocolNumber,
      }),
    }).then(mapStr)
  }

  async getCtrs(): Promise<CurrencyTransactionReport[]> {
    return this.request<GrcEngineCtr[]>("/reports/ctr").then((reports) =>
      reports.map(mapCtr)
    )
  }

  // --- Threshold Rules & Admin Settings ---
  async getThresholdRules(): Promise<ThresholdRule[]> {
    return this.request<ThresholdRule[]>("/settings/threshold-rules")
  }

  async updateThresholdRule(
    id: string,
    data: Partial<ThresholdRule>
  ): Promise<ThresholdRule> {
    return this.request<ThresholdRule>(`/settings/threshold-rules/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    })
  }

  async getAuditLogs(): Promise<AuditLog[]> {
    return this.request<AuditLog[]>("/audit-log")
  }
}

export const api = new ApiClient()

type GrcEngineClient = {
  id: string
  fullName: string
  idDocumentType: string
  idDocumentNumber: string
  nuib?: string
  dueDiligenceRegime: "NORMAL" | "SIMPLIFIED"
  riskRating: string
  pep: boolean
  pepCategory?: string
  pepLastReviewedAt?: string
  createdAt: string
}

type GrcEngineFlag = {
  id: string
  type: Flag["flagType"]
  status: Flag["status"]
  reason: string
  slaDeadline?: string
  createdAt: string
  client?: GrcEngineClient
}

type GrcEngineUploadPreview = {
  batchId: string
  fileName: string
  format: "CSV" | "XLSX"
  headers: string[]
  previewRows: string[][]
  suggestedMapping: Record<string, string>
}

type GrcEngineImportBatch = {
  id: string
  fileName: string
  format: "CSV" | "XLSX"
  totalRows: number
  errorRows: number
  importedAt: string
  columnMapping?: Record<string, string>
}

type GrcEngineStr = {
  id: string
  reportingInstitution: string
  accountNumber?: string
  accountHolder: string
  transactionExecutor?: string
  monetaryValue?: number
  suspicionDescription: string
  businessRelationshipDescription: string
  internalEmployeeInvolved: boolean
  voluntaryStatement?: string
  ocosName: string
  gifimRegistrationNumber?: string
  submittedAt?: string
  retentionUntil?: string
  createdAt: string
}

type GrcEngineCtr = {
  id: string
  aggregationPeriodStart: string
  aggregationPeriodEnd: string
  aggregateValue: number
  currency: string
  submittedAt?: string
  retentionUntil?: string
}

function mapClient(client: GrcEngineClient): Client {
  return {
    ...client,
    clientType: "INDIVIDUAL",
    documentNumber: client.idDocumentNumber,
    regime: client.dueDiligenceRegime === "SIMPLIFIED" ? "SIMPLIFICADO" : "NORMAL",
    riskRating:
      client.riskRating === "HIGH"
        ? "ALTO"
        : client.riskRating === "MEDIUM"
          ? "MEDIO"
          : "BAIXO",
    isPep: client.pep,
  }
}

function mapFlag(flag: GrcEngineFlag): Flag {
  return {
    ...flag,
    flagType: flag.type,
    ruleTriggered: flag.reason,
    client: flag.client ? mapClient(flag.client) : undefined,
  }
}

function mapImportBatch(batch: GrcEngineImportBatch): ImportBatch {
  return {
    id: batch.id,
    fileName: batch.fileName,
    fileType: batch.format,
    format: batch.format,
    status: "COMPLETED",
    totalRows: batch.totalRows,
    processedRows: batch.totalRows - batch.errorRows,
    errorRows: batch.errorRows,
    createdAt: batch.importedAt,
    importedAt: batch.importedAt,
    columnMapping: batch.columnMapping,
  }
}

function mapStr(report: GrcEngineStr): SuspiciousActivityReport {
  return {
    ...report,
    strReference: report.id,
    suspicionGrounds: report.suspicionDescription,
    operationDetails: report.accountNumber ?? "",
    evidenceSummary: report.voluntaryStatement ?? "",
    actionTaken: "",
    gifimProtocolNumber: report.gifimRegistrationNumber,
    submissionDate: report.submittedAt,
    retentionDeadline: report.retentionUntil,
    status: report.submittedAt ? "SUBMITTED" : "DRAFT",
  }
}

function mapCtr(report: GrcEngineCtr): CurrencyTransactionReport {
  return {
    ...report,
    ctrReference: report.id,
    reportingMonth: Number(report.aggregationPeriodStart.slice(5, 7)),
    reportingYear: Number(report.aggregationPeriodStart.slice(0, 4)),
    aggregateAmount: report.aggregateValue,
    transactionCount: 0,
    status: report.submittedAt ? "SUBMITTED" : "PENDING_SUBMISSION",
    gifimSubmissionDate: report.submittedAt,
    createdAt: report.aggregationPeriodStart,
  }
}
