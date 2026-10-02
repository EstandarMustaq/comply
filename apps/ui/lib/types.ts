export type UserRole = "ADMIN" | "COMPLIANCE_OFFICER" | "OCOS" | "AUDITOR_READONLY"

export interface User {
  id: string
  email: string
  fullName: string
  role: UserRole
  mfaEnrolled: boolean
  isActive: boolean
  createdAt: string
}

export interface AuthResponse {
  status?: "SUCCESS" | "MFA_REQUIRED" | "MFA_ENROLLMENT_REQUIRED"
  challengeToken?: string
  enrollmentToken?: string
  accessToken?: string
  refreshToken?: string
  userId?: string
  fullName?: string
  email?: string
  role?: UserRole
  mfaEnabled?: boolean
  token?: string
  tokenType?: string
  mfaRequired?: boolean
  mfaEnrolled?: boolean
  tempToken?: string
  requiresEnrollment?: boolean
  message?: string
  backupCodes?: string[]
}

export interface MfaEnrollResponse {
  secretKey?: string
  qrCodeBase64?: string
  otpauthUrl?: string
  secret?: string
  qrCodeUri?: string
  backupCodes?: string[]
}

export interface InsightsData {
  totalClients: number
  kycCompleteCount: number
  kycCompletenessPercentage: number
  simplifiedRegimeCount: number
  normalRegimeCount: number
  pendingFlagsCount: number
  sanctionsMatchCount: number
  overdueSlaCount: number
  pepReviewDueCount: number
  totalTransactionsCount: number
  retentionExpiringCount: number
  simplifiedRegimeClients?: number
  normalRegimeClients?: number
  totalTransactions?: number
  urgentSanctionsFlagsCount?: number
  totalVolumeMt?: number
  reportedStrsCount?: number
  complianceScorePercent?: number
}

export type ClientRegime = "SIMPLIFICADO" | "NORMAL"
export type ClientRiskRating = "BAIXO" | "MEDIO" | "ALTO"

export interface Client {
  id: string
  fullName: string
  documentNumber: string
  idDocumentType?: string
  idDocumentNumber?: string
  nuib?: string
  clientType: "INDIVIDUAL" | "CORPORATE"
  regime: ClientRegime
  dueDiligenceRegime?: "NORMAL" | "SIMPLIFIED"
  riskRating: ClientRiskRating
  monthlyVolumeMt?: number
  annualVolumeMt?: number
  isPep: boolean
  pepCategory?: string
  lastPepReviewDate?: string
  beneficialOwners?: BeneficialOwner[]
  createdAt: string
}

export interface BeneficialOwner {
  id: string
  fullName: string
  documentNumber: string
  nationality: string
  ownershipPercentage: number
  isPep: boolean
}

export interface Transaction {
  id: string
  clientId?: string
  client?: Client
  accountNumber: string
  transactionType: string
  amount: number
  currency: string
  transactionDate: string
  channel: string
  counterpartyName?: string
  counterpartyDocument?: string
  counterpartyAccountNumber?: string
  counterpartyBank?: string
}

export type FlagType =
  | "KYC_INCOMPLETE"
  | "THRESHOLD_VALUE"
  | "STRUCTURING_SUSPECTED"
  | "SANCTIONS_MATCH"
  | "PEP_REVIEW_DUE"

export type FlagStatus = "PENDING" | "IN_REVIEW" | "ESCALATED_TO_STR" | "RESOLVED"

export interface Flag {
  id: string
  flagType: FlagType
  type?: FlagType
  status: FlagStatus
  ruleTriggered: string
  reason?: string
  details?: Record<string, unknown>
  slaDeadline?: string
  notes?: string
  transaction?: Transaction
  client?: Client
  assignedTo?: User
  createdAt: string
  resolvedAt?: string
}

export interface ImportBatch {
  id: string
  fileName: string
  fileType: "CSV" | "XLSX"
  status: "UPLOADED" | "MAPPING_CONFIRMED" | "PROCESSING" | "COMPLETED" | "FAILED"
  totalRows: number
  processedRows: number
  errorRows: number
  detectedHeaders?: string[]
  columnMapping?: Record<string, string>
  createdAt: string
  format?: "CSV" | "XLSX"
  importedAt?: string
  headers?: string[]
  previewRows?: string[][]
  suggestedMapping?: Record<string, string>
}

export interface SuspiciousActivityReport {
  id: string
  strReference: string
  client?: Client
  flag?: Flag
  suspicionGrounds: string
  operationDetails: string
  evidenceSummary: string
  actionTaken: string
  gifimProtocolNumber?: string
  submissionDate?: string
  retentionDeadline?: string
  status: "DRAFT" | "SUBMITTED" | "ARCHIVED"
  accountNumber?: string
  accountHolder?: string
  transactionExecutor?: string
  monetaryValue?: number
  suspicionDescription?: string
  businessRelationshipDescription?: string
  internalEmployeeInvolved?: boolean
  voluntaryStatement?: string
  reportingInstitution?: string
  ocosName?: string
  gifimRegistrationNumber?: string
  submittedAt?: string
  retentionUntil?: string
  submittedBy?: User
  createdAt: string
}

export interface CurrencyTransactionReport {
  id: string
  ctrReference: string
  client?: Client
  reportingMonth: number
  reportingYear: number
  aggregateAmount: number
  transactionCount: number
  status: "PENDING_SUBMISSION" | "SUBMITTED" | "ARCHIVED"
  gifimSubmissionDate?: string
  createdAt: string
  aggregationPeriodStart?: string
  aggregationPeriodEnd?: string
  aggregateValue?: number
  currency?: string
  submittedAt?: string
  retentionUntil?: string
}

export interface ThresholdRule {
  id: string
  name: string
  type: "SINGLE_TRANSACTION_VALUE" | "MONTHLY_AGGREGATE_VALUE" | "STRUCTURING"
  valueThreshold: number | null
  aggregationWindowDays: number | null
  active: boolean
}

export interface AuditLog {
  id: string
  actor?: User | null
  action: string
  entity: string
  entityId?: string | null
  details?: string | null
  createdAt: string
}
