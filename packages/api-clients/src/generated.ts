export interface paths {
  "/api/settings/threshold-rules/{id}": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put: operations["updateThresholdRule"]
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/settings/system-config/{key}": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put: operations["updateSystemConfig"]
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/reports/str/{id}": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["getStr"]
    put: operations["updateStr"]
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/settings/users": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["listUsers"]
    put?: never
    post: operations["createUser"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/reports/str/{id}/submit": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post: operations["submitStr"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/reports/ctr/{id}/submit": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post: operations["submitCtr"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/import-batches": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["listBatches"]
    put?: never
    post: operations["uploadBatch"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/import-batches/{id}/confirm-mapping": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post: operations["confirmMapping"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/flags/{id}/escalate-to-str": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post: operations["escalateToStr"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/clients": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["listClients"]
    put?: never
    post: operations["createClient"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/clients/{clientId}/kyc/recalculate": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post: operations["recalculate"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/clients/{clientId}/beneficial-owners": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["listOwners"]
    put?: never
    post: operations["addOwner"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/clients/beneficial-owners/{ownerId}/verify": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post: operations["verifyOwner"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/auth/refresh": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post: operations["refresh"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/auth/mfa/verify": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post: operations["verifyMfa"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/auth/mfa/enroll": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post: operations["enrollMfa"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/auth/mfa/confirm": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post: operations["confirmMfa"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/auth/logout": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post: operations["logout"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/auth/login": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post: operations["login"]
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/settings/users/{id}/reset-mfa": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch: operations["resetMfa"]
    trace?: never
  }
  "/api/flags/{id}/review": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch: operations["reviewFlag"]
    trace?: never
  }
  "/api/clients/{id}/risk-rating": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch: operations["updateRiskRating"]
    trace?: never
  }
  "/api/clients/{id}/pep-review": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get?: never
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch: operations["registerPepReview"]
    trace?: never
  }
  "/api/settings/threshold-rules": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["listThresholdRules"]
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/settings/system-config": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["listSystemConfigs"]
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/reports/str": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["listStrs"]
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/reports/exports/str/{id}.pdf": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["exportStrPdf"]
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/reports/exports/annual/{year}.xlsx": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["exportAnnual"]
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/reports/ctr": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["listCtrs"]
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/insights/summary": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["getSummary"]
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/import-batches/{id}": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["getBatch"]
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/flags": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["listFlags"]
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/clients/{id}": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["getClient"]
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/auth/me": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["getCurrentUserInfo"]
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  "/api/audit-log": {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations["listAuditLogs"]
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
}
export type webhooks = Record<string, never>
export interface components {
  schemas: {
    UpdateThresholdRuleRequest: {
      valueThreshold?: number
      /** Format: int32 */
      aggregationWindowDays?: number
      active?: boolean
    }
    UpdateSystemConfigRequest: {
      jsonValue: string
    }
    UpdateStrRequest: {
      accountNumber?: string
      accountHolder?: string
      transactionExecutor?: string
      monetaryValue?: number
      suspicionDescription?: string
      businessRelationshipDescription?: string
      internalEmployeeInvolved?: boolean
      voluntaryStatement?: string
    }
    CreateUserRequest: {
      fullName: string
      /** Format: email */
      email: string
      password: string
      /** @enum {string} */
      role: "ADMIN" | "COMPLIANCE_OFFICER" | "OCOS" | "AUDITOR_READONLY"
    }
    SubmitStrRequest: {
      gifimRegistrationNumber: string
    }
    CreateClientRequest: {
      fullName: string
      idDocumentType: string
      idDocumentNumber: string
      /** @enum {string} */
      clientType?: "INDIVIDUAL" | "CORPORATE"
      nuib?: string
      /** @enum {string} */
      dueDiligenceRegime?: "NORMAL" | "SIMPLIFIED"
      isPep?: boolean
      pepCategory?: string
      seniorManagementApproved?: boolean
    }
    BeneficialOwnerRequest: {
      fullName: string
      /** @enum {string} */
      role: "FOUNDER" | "ADMINISTRATOR" | "TRUSTEE" | "BENEFICIARY" | "CONTROLLING_PERSON"
      ownershipPercentage?: number
    }
    RefreshTokenRequest: {
      refreshToken: string
    }
    MfaVerifyRequest: {
      challengeToken?: string
      tempToken?: string
      code?: string
      totpCode?: string
    }
    MfaConfirmRequest: {
      code?: string
      enrollmentToken?: string
    }
    LoginRequest: {
      /** Format: email */
      email: string
      password?: string
      passwordHash?: string
    }
    ReviewFlagRequest: {
      note: string
      /** @enum {string} */
      status?: "PENDING" | "IN_REVIEW" | "ESCALATED_TO_STR" | "RESOLVED"
    }
    UpdateRiskRatingRequest: {
      /** @enum {string} */
      riskRating: "LOW" | "MODERATE" | "HIGH"
    }
    ThresholdRule: {
      /** Format: uuid */
      id?: string
      name?: string
      /** @enum {string} */
      type?: "SINGLE_TRANSACTION_VALUE" | "MONTHLY_AGGREGATE_VALUE" | "STRUCTURING"
      valueThreshold?: number
      /** Format: int32 */
      aggregationWindowDays?: number
      active?: boolean
    }
    Organization: {
      /** Format: uuid */
      id?: string
      slug?: string
      name?: string
      /** Format: date-time */
      createdAt?: string
    }
    SystemConfig: {
      key?: string
      value?: string
      updatedBy?: components["schemas"]["User"]
      /** Format: date-time */
      updatedAt?: string
    }
    User: {
      /** Format: uuid */
      id?: string
      fullName?: string
      email?: string
      /** @enum {string} */
      role?: "ADMIN" | "COMPLIANCE_OFFICER" | "OCOS" | "AUDITOR_READONLY"
      mfaEnabled?: boolean
      /** Format: date-time */
      createdAt?: string
      /** Format: date-time */
      lastLoginAt?: string
      organization?: components["schemas"]["Organization"]
    }
    Flag: {
      /** Format: uuid */
      id?: string
      /** @enum {string} */
      type?:
        | "KYC_INCOMPLETE"
        | "THRESHOLD_VALUE"
        | "STRUCTURING_SUSPECTED"
        | "SANCTIONS_MATCH"
        | "PEP_REVIEW_DUE"
      reason?: string
      /** @enum {string} */
      status?: "PENDING" | "IN_REVIEW" | "ESCALATED_TO_STR" | "RESOLVED"
      /** Format: date-time */
      slaDeadline?: string
      /** Format: date-time */
      createdAt?: string
      /** Format: date-time */
      reviewedAt?: string
      reviewNote?: string
    }
    SuspiciousActivityReport: {
      /** Format: uuid */
      id?: string
      flag?: components["schemas"]["Flag"]
      reportingInstitution?: string
      accountNumber?: string
      accountHolder?: string
      transactionExecutor?: string
      monetaryValue?: number
      suspicionDescription?: string
      businessRelationshipDescription?: string
      internalEmployeeInvolved?: boolean
      voluntaryStatement?: string
      ocosName?: string
      gifimRegistrationNumber?: string
      /** Format: date-time */
      submittedAt?: string
      /** Format: date */
      retentionUntil?: string
      submitted?: boolean
    }
    Client: {
      /** Format: uuid */
      id?: string
      fullName?: string
      idDocumentType?: string
      idDocumentNumber?: string
      /** @enum {string} */
      clientType?: "INDIVIDUAL" | "CORPORATE"
      nuib?: string
      /** @enum {string} */
      riskRating?: "LOW" | "MODERATE" | "HIGH"
      /** Format: date */
      riskRatingUpdatedAt?: string
      /** @enum {string} */
      dueDiligenceRegime?: "NORMAL" | "SIMPLIFIED"
      pep?: boolean
      pepCategory?: string
      /** Format: date */
      pepLastReviewedAt?: string
      seniorManagementApproved?: boolean
      kycComplete?: boolean
      sanctionsFrozen?: boolean
      /** Format: date-time */
      sanctionsFrozenAt?: string
      sanctionsFreezeReason?: string
      /** Format: date-time */
      createdAt?: string
      /** Format: date-time */
      updatedAt?: string
      organization?: components["schemas"]["Organization"]
    }
    CurrencyTransactionReport: {
      /** Format: uuid */
      id?: string
      client?: components["schemas"]["Client"]
      /** Format: date */
      aggregationPeriodStart?: string
      /** Format: date */
      aggregationPeriodEnd?: string
      aggregateValue?: number
      currency?: string
      thresholdRule?: components["schemas"]["ThresholdRule"]
      /** Format: date-time */
      submittedAt?: string
      /** Format: date */
      retentionUntil?: string
      submitted?: boolean
    }
    InsightsSummaryResponse: {
      /** Format: int64 */
      totalClients?: number
      /** Format: int64 */
      kycCompleteCount?: number
      /** Format: double */
      kycCompletenessPercentage?: number
      /** Format: int64 */
      simplifiedRegimeCount?: number
      /** Format: int64 */
      normalRegimeCount?: number
      /** Format: int64 */
      pendingFlagsCount?: number
      /** Format: int64 */
      sanctionsMatchCount?: number
      /** Format: int64 */
      overdueSlaCount?: number
      /** Format: int64 */
      pepReviewDueCount?: number
      /** Format: int64 */
      totalTransactionsCount?: number
      /** Format: int64 */
      retentionExpiringCount?: number
    }
    ImportBatch: {
      /** Format: uuid */
      id?: string
      fileName?: string
      /** @enum {string} */
      format?: "CSV" | "XLSX"
      columnMapping?: string
      /** Format: int32 */
      totalRows?: number
      /** Format: int32 */
      errorRows?: number
      sourceFilePath?: string
      importedBy?: components["schemas"]["User"]
      organization?: components["schemas"]["Organization"]
      /** Format: date-time */
      importedAt?: string
    }
    Pageable: {
      /** Format: int32 */
      page?: number
      /** Format: int32 */
      size?: number
      sort?: string[]
    }
    BeneficialOwner: {
      /** Format: uuid */
      id?: string
      fullName?: string
      /** @enum {string} */
      role?:
        "FOUNDER" | "ADMINISTRATOR" | "TRUSTEE" | "BENEFICIARY" | "CONTROLLING_PERSON"
      ownershipPercentage?: number
      /** Format: date */
      verifiedAt?: string
    }
    PageClient: {
      /** Format: int32 */
      totalPages?: number
      /** Format: int64 */
      totalElements?: number
      /** Format: int32 */
      size?: number
      content?: components["schemas"]["Client"][]
      /** Format: int32 */
      number?: number
      sort?: components["schemas"]["SortObject"]
      pageable?: components["schemas"]["PageableObject"]
      /** Format: int32 */
      numberOfElements?: number
      first?: boolean
      last?: boolean
      empty?: boolean
    }
    PageableObject: {
      /** Format: int64 */
      offset?: number
      paged?: boolean
      /** Format: int32 */
      pageNumber?: number
      /** Format: int32 */
      pageSize?: number
      sort?: components["schemas"]["SortObject"]
      unpaged?: boolean
    }
    SortObject: {
      empty?: boolean
      sorted?: boolean
      unsorted?: boolean
    }
    AuditLog: {
      /** Format: uuid */
      id?: string
      actor?: components["schemas"]["User"]
      /** @enum {string} */
      action?:
        | "LOGIN"
        | "LOGOUT"
        | "MFA_ENROLLED"
        | "MFA_VERIFIED"
        | "UPLOAD"
        | "FLAG_REVIEWED"
        | "STR_SUBMITTED"
        | "CTR_SUBMITTED"
        | "CLIENT_UPDATED"
        | "PEP_REVIEWED"
        | "SETTINGS_UPDATED"
        | "USER_CREATED"
        | "USER_UPDATED"
        | "REGULATORY_EXPORT"
      entity?: string
      /** Format: uuid */
      entityId?: string
      details?: string
      /** Format: date-time */
      createdAt?: string
    }
    PageAuditLog: {
      /** Format: int32 */
      totalPages?: number
      /** Format: int64 */
      totalElements?: number
      /** Format: int32 */
      size?: number
      content?: components["schemas"]["AuditLog"][]
      /** Format: int32 */
      number?: number
      sort?: components["schemas"]["SortObject"]
      pageable?: components["schemas"]["PageableObject"]
      /** Format: int32 */
      numberOfElements?: number
      first?: boolean
      last?: boolean
      empty?: boolean
    }
  }
  responses: never
  parameters: never
  requestBodies: never
  headers: never
  pathItems: never
}
export type $defs = Record<string, never>
export interface operations {
  updateThresholdRule: {
    parameters: {
      query?: never
      header?: never
      path: {
        id: string
      }
      cookie?: never
    }
    requestBody: {
      content: {
        "application/json": components["schemas"]["UpdateThresholdRuleRequest"]
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  updateSystemConfig: {
    parameters: {
      query?: never
      header?: never
      path: {
        key: string
      }
      cookie?: never
    }
    requestBody: {
      content: {
        "application/json": components["schemas"]["UpdateSystemConfigRequest"]
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  getStr: {
    parameters: {
      query?: never
      header?: never
      path: {
        id: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": components["schemas"]["SuspiciousActivityReport"]
        }
      }
    }
  }
  updateStr: {
    parameters: {
      query?: never
      header?: never
      path: {
        id: string
      }
      cookie?: never
    }
    requestBody: {
      content: {
        "application/json": components["schemas"]["UpdateStrRequest"]
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  listUsers: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": {
            [key: string]: unknown
          }[]
        }
      }
    }
  }
  createUser: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody: {
      content: {
        "application/json": components["schemas"]["CreateUserRequest"]
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  submitStr: {
    parameters: {
      query?: never
      header?: never
      path: {
        id: string
      }
      cookie?: never
    }
    requestBody: {
      content: {
        "application/json": components["schemas"]["SubmitStrRequest"]
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  submitCtr: {
    parameters: {
      query?: never
      header?: never
      path: {
        id: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  listBatches: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": components["schemas"]["ImportBatch"][]
        }
      }
    }
  }
  uploadBatch: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody?: {
      content: {
        "application/json": {
          /** Format: binary */
          file: string
        }
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  confirmMapping: {
    parameters: {
      query?: never
      header?: never
      path: {
        id: string
      }
      cookie?: never
    }
    requestBody: {
      content: {
        "application/json": {
          [key: string]: string
        }
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  escalateToStr: {
    parameters: {
      query?: never
      header?: never
      path: {
        id: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  listClients: {
    parameters: {
      query: {
        regime?: "NORMAL" | "SIMPLIFIED"
        risk?: "LOW" | "MODERATE" | "HIGH"
        pep?: boolean
        search?: string
        pageable: components["schemas"]["Pageable"]
      }
      header?: never
      path?: never
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": components["schemas"]["PageClient"]
        }
      }
    }
  }
  createClient: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody: {
      content: {
        "application/json": components["schemas"]["CreateClientRequest"]
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  recalculate: {
    parameters: {
      query?: never
      header?: never
      path: {
        clientId: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  listOwners: {
    parameters: {
      query?: never
      header?: never
      path: {
        clientId: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": components["schemas"]["BeneficialOwner"][]
        }
      }
    }
  }
  addOwner: {
    parameters: {
      query?: never
      header?: never
      path: {
        clientId: string
      }
      cookie?: never
    }
    requestBody: {
      content: {
        "application/json": components["schemas"]["BeneficialOwnerRequest"]
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  verifyOwner: {
    parameters: {
      query?: never
      header?: never
      path: {
        ownerId: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  refresh: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody: {
      content: {
        "application/json": components["schemas"]["RefreshTokenRequest"]
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  verifyMfa: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody: {
      content: {
        "application/json": components["schemas"]["MfaVerifyRequest"]
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  enrollMfa: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody?: {
      content: {
        "application/json": {
          [key: string]: string
        }
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  confirmMfa: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody: {
      content: {
        "application/json": components["schemas"]["MfaConfirmRequest"]
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  logout: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  login: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody: {
      content: {
        "application/json": components["schemas"]["LoginRequest"]
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  resetMfa: {
    parameters: {
      query?: never
      header?: never
      path: {
        id: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  reviewFlag: {
    parameters: {
      query?: never
      header?: never
      path: {
        id: string
      }
      cookie?: never
    }
    requestBody: {
      content: {
        "application/json": components["schemas"]["ReviewFlagRequest"]
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  updateRiskRating: {
    parameters: {
      query?: never
      header?: never
      path: {
        id: string
      }
      cookie?: never
    }
    requestBody: {
      content: {
        "application/json": components["schemas"]["UpdateRiskRatingRequest"]
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  registerPepReview: {
    parameters: {
      query?: never
      header?: never
      path: {
        id: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  listThresholdRules: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": components["schemas"]["ThresholdRule"][]
        }
      }
    }
  }
  listSystemConfigs: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": components["schemas"]["SystemConfig"][]
        }
      }
    }
  }
  listStrs: {
    parameters: {
      query?: {
        submitted?: boolean
      }
      header?: never
      path?: never
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": components["schemas"]["SuspiciousActivityReport"][]
        }
      }
    }
  }
  exportStrPdf: {
    parameters: {
      query?: never
      header?: never
      path: {
        id: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": string
        }
      }
    }
  }
  exportAnnual: {
    parameters: {
      query?: never
      header?: never
      path: {
        year: number
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": string
        }
      }
    }
  }
  listCtrs: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": components["schemas"]["CurrencyTransactionReport"][]
        }
      }
    }
  }
  getSummary: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": components["schemas"]["InsightsSummaryResponse"]
        }
      }
    }
  }
  getBatch: {
    parameters: {
      query?: never
      header?: never
      path: {
        id: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": components["schemas"]["ImportBatch"]
        }
      }
    }
  }
  listFlags: {
    parameters: {
      query?: {
        status?: "PENDING" | "IN_REVIEW" | "ESCALATED_TO_STR" | "RESOLVED"
        type?:
          | "KYC_INCOMPLETE"
          | "THRESHOLD_VALUE"
          | "STRUCTURING_SUSPECTED"
          | "SANCTIONS_MATCH"
          | "PEP_REVIEW_DUE"
      }
      header?: never
      path?: never
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": components["schemas"]["Flag"][]
        }
      }
    }
  }
  getClient: {
    parameters: {
      query?: never
      header?: never
      path: {
        id: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": components["schemas"]["Client"]
        }
      }
    }
  }
  getCurrentUserInfo: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": Record<string, never>
        }
      }
    }
  }
  listAuditLogs: {
    parameters: {
      query: {
        action?:
          | "LOGIN"
          | "LOGOUT"
          | "MFA_ENROLLED"
          | "MFA_VERIFIED"
          | "UPLOAD"
          | "FLAG_REVIEWED"
          | "STR_SUBMITTED"
          | "CTR_SUBMITTED"
          | "CLIENT_UPDATED"
          | "PEP_REVIEWED"
          | "SETTINGS_UPDATED"
          | "USER_CREATED"
          | "USER_UPDATED"
          | "REGULATORY_EXPORT"
        pageable: components["schemas"]["Pageable"]
      }
      header?: never
      path?: never
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          "*/*": components["schemas"]["PageAuditLog"]
        }
      }
    }
  }
}
