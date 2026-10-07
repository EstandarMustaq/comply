const flagTypeLabels: Record<string, string> = {
  KYC_INCOMPLETE: "Customer profile incomplete",
  THRESHOLD_VALUE: "Threshold exceeded",
  THRESHOLD_EXCEEDED: "Threshold exceeded",
  STRUCTURING_SUSPECTED: "Suspicious structuring",
  SANCTIONS_MATCH: "Restricted-party match",
  PEP_REVIEW_DUE: "PEP review overdue",
  PEP_TRANSACTION: "Transaction linked to a politically exposed person",
  RAPID_MOVEMENT: "Rapid movement",
  SIMPLIFIED_REGIME_BREACH: "Simplified-regime limit exceeded",
}

const flagStatusLabels: Record<string, string> = {
  PENDING: "Pending",
  IN_REVIEW: "In review",
  UNDER_REVIEW: "In review",
  ESCALATED: "Escalated",
  ESCALATED_TO_STR: "Escalated to report",
  DISMISSED: "Dismissed",
  RESOLVED: "Resolved",
}

const importStatusLabels: Record<string, string> = {
  UPLOADED: "Received",
  MAPPING_CONFIRMED: "Mapping confirmed",
  PROCESSING: "Processing",
  COMPLETED: "Complete",
  FAILED: "Failed",
}

const reportStatusLabels: Record<string, string> = {
  DRAFT: "Draft",
  PENDING_SUBMISSION: "Pending submission",
  SUBMITTED: "Submitted",
  ARCHIVED: "Archived",
}

const transactionTypeLabels: Record<string, string> = {
  DEPOSIT: "Deposit",
  WITHDRAWAL: "Withdrawal",
  TRANSFER: "Transfer",
  CREDIT: "Credit",
}

const userRoleLabels: Record<string, string> = {
  ADMIN: "Administrator",
  COMPLIANCE_OFFICER: "Compliance officer",
  OCOS: "OCOS",
  AUDITOR_READONLY: "Read-only auditor",
}

const auditActionLabels: Record<string, string> = {
  LOGIN: "Signed in",
  LOGOUT: "Signed out",
  MFA_ENROLLED: "Multi-factor authentication set up",
  MFA_VERIFIED: "Multi-factor authentication verified",
  UPLOAD: "File imported",
  FLAG_REVIEWED: "Alert reviewed",
  STR_SUBMITTED: "Report submitted",
  CTR_SUBMITTED: "Cash transaction report submitted",
  CLIENT_UPDATED: "Customer profile updated",
  PEP_REVIEWED: "PEP review completed",
  SETTINGS_UPDATED: "Settings updated",
  USER_CREATED: "User created",
  USER_UPDATED: "User updated",
  REGULATORY_EXPORT: "Report exported",
}

const resourceTypeLabels: Record<string, string> = {
  clients: "Customer",
  flags: "Alert",
  transactions: "Transaction",
  users: "User",
  organizations: "Organization",
  import_batches: "Import batch",
  suspicious_activity_reports: "Suspicious activity report",
  currency_transaction_reports: "Cash transaction report",
  system_config: "System settings",
}

const ruleTypeLabels: Record<string, string> = {
  SINGLE_TRANSACTION_VALUE: "Single transaction value",
  MONTHLY_AGGREGATE_VALUE: "Monthly aggregate value",
  STRUCTURING: "Structuring",
}

const regimeLabels: Record<string, string> = {
  NORMAL: "Standard / enhanced",
  SIMPLIFIED: "Simplified",
  SIMPLIFICADO: "Simplified",
  ALL: "All",
  TODOS: "All",
}

const clientTypeLabels: Record<string, string> = {
  INDIVIDUAL: "Individual",
  CORPORATE: "Business",
}

const riskRatingLabels: Record<string, string> = {
  LOW: "Low",
  BAIXO: "Low",
  MODERATE: "Moderate",
  MEDIO: "Moderate",
  HIGH: "High",
  ALTO: "High",
}

function labelFor(labels: Record<string, string>, value: string, fallback: string) {
  return labels[value] ?? fallback
}

export const getFlagTypeLabel = (value: string) =>
  labelFor(flagTypeLabels, value, "Other alert")
export const getFlagStatusLabel = (value: string) =>
  labelFor(flagStatusLabels, value, "Unknown status")
export const getImportStatusLabel = (value: string) =>
  labelFor(importStatusLabels, value, "Unknown status")
export const getReportStatusLabel = (value: string) =>
  labelFor(reportStatusLabels, value, "Unknown status")
export const getTransactionTypeLabel = (value: string) =>
  labelFor(transactionTypeLabels, value, "Other type")
export const getUserRoleLabel = (value: string) =>
  labelFor(userRoleLabels, value, "Other role")
export const getAuditActionLabel = (value: string) =>
  labelFor(auditActionLabels, value, "Other action")
export const getResourceTypeLabel = (value: string) =>
  labelFor(resourceTypeLabels, value, "Other resource")
export const getRuleTypeLabel = (value: string) =>
  labelFor(ruleTypeLabels, value, "Other rule")
export const getRegimeLabel = (value: string) =>
  labelFor(regimeLabels, value, "Other regime")
export const getClientTypeLabel = (value: string) =>
  labelFor(clientTypeLabels, value, "Other customer type")
export const getRiskRatingLabel = (value: string) =>
  labelFor(riskRatingLabels, value, "Unrated")
