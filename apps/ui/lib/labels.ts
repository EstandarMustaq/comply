const flagTypeLabels: Record<string, string> = {
  KYC_INCOMPLETE: "Perfil incompleto",
  THRESHOLD_VALUE: "Limiar excedido",
  THRESHOLD_EXCEEDED: "Limiar excedido",
  STRUCTURING_SUSPECTED: "Fraccionamento suspeito",
  SANCTIONS_MATCH: "Correspondência em lista de triagem",
  PEP_REVIEW_DUE: "Revisão periódica vencida",
  PEP_TRANSACTION: "Operação associada a pessoa exposta politicamente",
  RAPID_MOVEMENT: "Movimentação rápida",
  SIMPLIFIED_REGIME_BREACH: "Limite do regime simplificado excedido",
}

const flagStatusLabels: Record<string, string> = {
  PENDING: "Pendente",
  IN_REVIEW: "Em análise",
  UNDER_REVIEW: "Em análise",
  ESCALATED: "Escalado",
  ESCALATED_TO_STR: "Escalado para comunicação",
  DISMISSED: "Descartado",
  RESOLVED: "Resolvido",
}

const importStatusLabels: Record<string, string> = {
  UPLOADED: "Recebido",
  MAPPING_CONFIRMED: "Mapeamento confirmado",
  PROCESSING: "Em processamento",
  COMPLETED: "Concluído",
  FAILED: "Falhou",
}

const reportStatusLabels: Record<string, string> = {
  DRAFT: "Rascunho",
  PENDING_SUBMISSION: "Pendente de submissão",
  SUBMITTED: "Submetido",
  ARCHIVED: "Arquivado",
}

const transactionTypeLabels: Record<string, string> = {
  DEPOSIT: "Depósito",
  WITHDRAWAL: "Levantamento",
  TRANSFER: "Transferência",
  CREDIT: "Crédito",
}

const userRoleLabels: Record<string, string> = {
  ADMIN: "Administrador",
  COMPLIANCE_OFFICER: "Responsável de conformidade",
  OCOS: "OCOS",
  AUDITOR_READONLY: "Auditor (leitura)",
}

const auditActionLabels: Record<string, string> = {
  LOGIN: "Início de sessão",
  LOGOUT: "Fim de sessão",
  MFA_ENROLLED: "Autenticação multifactor configurada",
  MFA_VERIFIED: "Autenticação multifactor confirmada",
  UPLOAD: "Ficheiro importado",
  FLAG_REVIEWED: "Alerta analisado",
  STR_SUBMITTED: "Comunicação submetida",
  CTR_SUBMITTED: "Comunicação de transacções submetida",
  CLIENT_UPDATED: "Perfil de cliente actualizado",
  PEP_REVIEWED: "Revisão periódica concluída",
  SETTINGS_UPDATED: "Definições actualizadas",
  USER_CREATED: "Utilizador criado",
  USER_UPDATED: "Utilizador actualizado",
  REGULATORY_EXPORT: "Relatório exportado",
}

const resourceTypeLabels: Record<string, string> = {
  clients: "Cliente",
  flags: "Alerta",
  transactions: "Transacção",
  users: "Utilizador",
  organizations: "Instituição",
  import_batches: "Lote de importação",
  suspicious_activity_reports: "Comunicação de operação suspeita",
  currency_transaction_reports: "Comunicação de transacções",
  system_config: "Definições do sistema",
}

const ruleTypeLabels: Record<string, string> = {
  SINGLE_TRANSACTION_VALUE: "Valor de uma transacção",
  MONTHLY_AGGREGATE_VALUE: "Volume mensal acumulado",
  STRUCTURING: "Fraccionamento de transacções",
}

const regimeLabels: Record<string, string> = {
  NORMAL: "Normal",
  SIMPLIFIED: "Simplificado",
  SIMPLIFICADO: "Simplificado",
  ALL: "Todos",
  TODOS: "Todos",
}

const clientTypeLabels: Record<string, string> = {
  INDIVIDUAL: "Pessoa singular",
  CORPORATE: "Pessoa colectiva",
}

const riskRatingLabels: Record<string, string> = {
  LOW: "Baixo",
  BAIXO: "Baixo",
  MODERATE: "Médio",
  MEDIO: "Médio",
  HIGH: "Elevado",
  ALTO: "Elevado",
}

function labelFor(labels: Record<string, string>, value: string, fallback: string) {
  return labels[value] ?? fallback
}

export const getFlagTypeLabel = (value: string) =>
  labelFor(flagTypeLabels, value, "Outro alerta")
export const getFlagStatusLabel = (value: string) =>
  labelFor(flagStatusLabels, value, "Estado não reconhecido")
export const getImportStatusLabel = (value: string) =>
  labelFor(importStatusLabels, value, "Estado não reconhecido")
export const getReportStatusLabel = (value: string) =>
  labelFor(reportStatusLabels, value, "Estado não reconhecido")
export const getTransactionTypeLabel = (value: string) =>
  labelFor(transactionTypeLabels, value, "Outro tipo")
export const getUserRoleLabel = (value: string) =>
  labelFor(userRoleLabels, value, "Outro perfil")
export const getAuditActionLabel = (value: string) =>
  labelFor(auditActionLabels, value, "Outra acção")
export const getResourceTypeLabel = (value: string) =>
  labelFor(resourceTypeLabels, value, "Outro recurso")
export const getRuleTypeLabel = (value: string) =>
  labelFor(ruleTypeLabels, value, "Outra regra")
export const getRegimeLabel = (value: string) =>
  labelFor(regimeLabels, value, "Outro regime")
export const getClientTypeLabel = (value: string) =>
  labelFor(clientTypeLabels, value, "Outro tipo de cliente")
export const getRiskRatingLabel = (value: string) =>
  labelFor(riskRatingLabels, value, "Não classificado")
