import { env } from "./env";
import {
  AuthResponse,
  MfaEnrollResponse,
  InsightsData,
  Client,
  Flag,
  ImportBatch,
  SuspiciousActivityReport,
  CurrencyTransactionReport,
  ThresholdRule,
  AuditLog,
  User,
} from "./types";

const API_BASE_URL = env.NEXT_PUBLIC_API_URL;

export interface CurrentUser {
  id: string;
  fullName: string;
  email: string;
  role: string;
  mfaEnabled: boolean;
}

class ApiClient {
  private getToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem("fincomp_token");
  }

  public setToken(token: string) {
    if (typeof window !== "undefined") {
      localStorage.setItem("fincomp_token", token);
    }
  }

  public clearToken() {
    if (typeof window !== "undefined") {
      localStorage.removeItem("fincomp_token");
    }
  }

  public hasToken(): boolean {
    if (typeof window === "undefined") return false;
    return !!localStorage.getItem("fincomp_token");
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    if (!API_BASE_URL) {
      throw new Error("NEXT_PUBLIC_API_URL is not configured.");
    }
    const token = this.getToken();
    const headers: Record<string, string> = {
      ...(options.headers as Record<string, string>),
    };

    if (token && !headers["Authorization"]) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    if (!(options.body instanceof FormData) && !headers["Content-Type"]) {
      headers["Content-Type"] = "application/json";
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    if (response.status === 401) {
      if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
        this.clearToken();
        window.location.href = "/login";
      }
      throw new Error("Session expired. Please sign in again.");
    }

    if (!response.ok) {
      let errorMsg = `Request failed: ${response.status} ${response.statusText}`;
      try {
        const errorData = await response.json();
        if (errorData.message) errorMsg = errorData.message;
        else if (typeof errorData === "string") errorMsg = errorData;
      } catch {
        // fallback to status text
      }
      throw new Error(errorMsg);
    }

    const contentType = response.headers.get("content-type");
    if (contentType && contentType.includes("application/json")) {
      return response.json();
    }
    return response.text() as unknown as T;
  }

  // --- Auth ---
  async login(email: string, password: string): Promise<AuthResponse> {
    const res = await this.request<AuthResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password, passwordHash: password }),
    });
    const token = res.accessToken || res.token;
    if (token) {
      this.setToken(token);
    }
    return res;
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
    });
    const token = res.accessToken || res.token;
    if (token) {
      this.setToken(token);
    }
    return res;
  }

  async enrollMfa(enrollmentToken?: string): Promise<MfaEnrollResponse> {
    return this.request<MfaEnrollResponse>("/auth/mfa/enroll", {
      method: "POST",
      body: JSON.stringify({ enrollmentToken, tempToken: enrollmentToken }),
    });
  }

  async confirmMfa(code: string, enrollmentToken?: string): Promise<AuthResponse> {
    const res = await this.request<AuthResponse>("/auth/mfa/confirm", {
      method: "POST",
      body: JSON.stringify({ code, enrollmentToken }),
    });
    const token = res.accessToken || res.token;
    if (token) {
      this.setToken(token);
    }
    return res;
  }

  async confirmMfaEnroll(tempToken: string, totpCode: string): Promise<AuthResponse> {
    return this.confirmMfa(totpCode, tempToken);
  }

  async logout(): Promise<void> {
    try {
      await this.request("/auth/logout", { method: "POST" });
    } catch {
      // Best-effort: always clear the local token
    } finally {
      this.clearToken();
    }
  }

  // --- Current User (from JWT via backend) ---
  async getMe(): Promise<CurrentUser> {
    return this.request<CurrentUser>("/auth/me");
  }

  // --- Insights & Dashboard ---
  async getInsights(): Promise<InsightsData> {
    return this.request<InsightsData>("/insights");
  }

  // --- Clients & KYC ---
  async getClients(page = 0, size = 50, search?: string): Promise<Client[]> {
    const query = search ? `?search=${encodeURIComponent(search)}` : "";
    return this.request<Client[]>(`/clients${query}`);
  }

  async getClientById(id: string): Promise<Client> {
    return this.request<Client>(`/clients/${id}`);
  }

  async createClient(clientData: Partial<Client>): Promise<Client> {
    return this.request<Client>("/clients", {
      method: "POST",
      body: JSON.stringify(clientData),
    });
  }

  // --- Flags & Risk Queue ---
  async getFlags(status?: string, flagType?: string): Promise<Flag[]> {
    const params = new URLSearchParams();
    if (status) params.append("status", status);
    if (flagType) params.append("flagType", flagType);
    const queryString = params.toString() ? `?${params.toString()}` : "";
    return this.request<Flag[]>(`/flags${queryString}`);
  }

  async getFlagById(id: string): Promise<Flag> {
    return this.request<Flag>(`/flags/${id}`);
  }

  async reviewFlag(
    id: string,
    action: "DISMISS" | "RESOLVE",
    notes: string
  ): Promise<Flag> {
    return this.request<Flag>(`/flags/${id}/review`, {
      method: "POST",
      body: JSON.stringify({ action, notes }),
    });
  }

  async escalateFlagToStr(id: string, notes: string): Promise<SuspiciousActivityReport> {
    return this.request<SuspiciousActivityReport>(`/flags/${id}/escalate-str`, {
      method: "POST",
      body: JSON.stringify({ notes }),
    });
  }

  // --- Ingestion / Batches ---
  async uploadBatch(file: File): Promise<ImportBatch> {
    const formData = new FormData();
    formData.append("file", file);
    return this.request<ImportBatch>("/import/upload", {
      method: "POST",
      body: formData,
    });
  }

  async confirmMapping(batchId: string, mapping: Record<string, string>): Promise<any> {
    return this.request(`/import/${batchId}/confirm-mapping`, {
      method: "POST",
      body: JSON.stringify(mapping),
    });
  }

  async getBatches(): Promise<ImportBatch[]> {
    return this.request<ImportBatch[]>("/import/batches");
  }

  async getBatchById(id: string): Promise<ImportBatch> {
    return this.request<ImportBatch>(`/import/batches/${id}`);
  }

  // --- Reports (STR & CTR) ---
  async getStrs(): Promise<SuspiciousActivityReport[]> {
    return this.request<SuspiciousActivityReport[]>("/reports/str");
  }

  async getStrById(id: string): Promise<SuspiciousActivityReport> {
    return this.request<SuspiciousActivityReport>(`/reports/str/${id}`);
  }

  async submitStr(
    id: string,
    submissionData: {
      suspicionGrounds: string;
      operationDetails: string;
      evidenceSummary: string;
      actionTaken: string;
      gifimProtocolNumber: string;
    }
  ): Promise<SuspiciousActivityReport> {
    return this.request<SuspiciousActivityReport>(`/reports/str/${id}/submit`, {
      method: "POST",
      body: JSON.stringify(submissionData),
    });
  }

  async getCtrs(): Promise<CurrencyTransactionReport[]> {
    return this.request<CurrencyTransactionReport[]>("/reports/ctr");
  }

  // --- Threshold Rules & Admin Settings ---
  async getThresholdRules(): Promise<ThresholdRule[]> {
    return this.request<ThresholdRule[]>("/settings/threshold-rules");
  }

  async updateThresholdRule(
    id: string,
    data: Partial<ThresholdRule>
  ): Promise<ThresholdRule> {
    return this.request<ThresholdRule>(`/settings/threshold-rules/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    });
  }

  async getAuditLogs(): Promise<AuditLog[]> {
    return this.request<AuditLog[]>("/audit-log");
  }
}

export const api = new ApiClient();
