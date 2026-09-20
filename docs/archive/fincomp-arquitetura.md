# Fincomp — Arquitetura Completa (v2)

> **Arquivo histórico — terminologia atualizada:** Este documento registra uma arquitetura anterior. O produto atual é **Financial Compliance Intelligent**, organizado como workspace Fincomp com `package/grc-engine` e `app/design-system`. Os requisitos atuais estão em `docs/architecture/README.md`, `docs/api/README.md` e `fincomp-workspace.yaml`.

Ferramenta de compliance/auditoria AML-CFT para instituições de microfinança e microcrédito em Moçambique. Deploy single-tenant, uma instância por cliente.

**Base legal coberta com profundidade** (não apenas citada):
- Lei n.º 14/2023, de 28 de Agosto — regime geral de prevenção e combate ao branqueamento de capitais e financiamento do terrorismo
- Aviso n.º 10/GBM/2024, de 30 de Agosto — Directrizes operacionais do Banco de Moçambique, 154 artigos, incluindo Capítulo XIII específico para operadores de microfinanças
- Lei n.º 15/2023, de 28 de Agosto — regime de terrorismo e proliferação de armas, Lista Designada Nacional, obrigação de congelamento em 24h
- Decreto n.º 53/2023 — regulamento complementar sobre identificação de clientes

---

## 1. Visão geral

- **GRC Engine**: Spring Boot (monólito modular), PostgreSQL, Flyway, Redis.
- **Design System**: Next.js, 100% shadcn/ui, dashboard única (não navegação multi-página).
- **Deploy**: EC2 t4g.small por cliente, Docker Compose (app + design system + db + redis + caddy).
- **Escopo**: single-tenant por entrega. Multi-tenant só acima de 5 clientes.
- **Gestão**: sistema assume múltiplos utilizadores por instituição (ADMIN, COMPLIANCE_OFFICER, OCOS, AUDITOR_READONLY), não uma pessoa só. Autenticação reforçada com 2FA obrigatório para ADMIN e OCOS.

---

## 2. Profundidade legal incorporada no desenho

Esta secção existe para que o sistema seja defensável perante um regulador, não apenas "baseado na lei" de forma decorativa.

### 2.1 Dois tipos de comunicação distintos ao GIFiM

A lei distingue claramente dois mecanismos, e o sistema modela-os como entidades separadas:

- **Suspicious Activity Report (STR)** — Participação de Operação Suspeita, accionada por julgamento humano do OCOS sobre uma transacção sinalizada, independente de qualquer limiar. Campos obrigatórios definidos no **Artigo 98** do Aviso 10: instituição, número de conta, titular, executor da transacção, valor, descrição da natureza da operação e circunstâncias da suspeita, relação de negócio entre o suspeito e a instituição, se o suspeito é funcionário interno, declaração voluntária sobre origem/destino dos fundos, impacto na credibilidade, nome e assinatura do OCOS.
- **Currency Transaction Report (CTR)** — Comunicação Automática, accionada automaticamente quando uma transacção ou o valor agregado de transacções da mesma pessoa num mês ultrapassa o limiar do **Artigo 44(3) da Lei 14/2023** (referido também no Artigo 129 do Aviso 10). Isto é reporte obrigatório por limiar, não por suspeita.

### 2.2 Detecção de fraccionamento (structuring)

O **Artigo 82** exige um registo centralizado e informatizado de todas as transacções ocasionais, independentemente do montante, especificamente para identificar fraccionamento de operações (várias transacções abaixo do limiar que, somadas, o ultrapassam). O sistema implementa isto como uma regra de agregação mensal por cliente, não apenas um limiar de transacção única.

### 2.3 Regime de diligência simplificado para microfinanças (Capítulo XIII, Artigos 134-141)

Isto aplica-se directamente aos dois clientes que já tens. Uma instituição de microcrédito ou cooperativa de crédito pode aplicar diligência simplificada quando:

- a actividade do cliente não ultrapassa **30.000 MT mensais** e **240.000 MT anuais** (Artigo 137)
- a conta é a única que o cliente possui no sistema financeiro
- a instituição obteve autorização do Banco de Moçambique para tal

Sob este regime, a instituição pode dispensar-se de (Artigo 141): classificação de risco individual, sistema de gestão de triggers, prova de verificação de identidade, informação sobre fonte de rendimento, comprovativos de endereço, e ferramentas automatizadas de filtragem. O sistema modela isto como um campo `dueDiligenceRegime` por cliente (NORMAL / SIMPLIFIED), com validação de que o SIMPLIFIED só é aceite dentro dos limites legais.

### 2.4 Pessoas Politicamente Expostas (Artigos 65-66)

Aprovação da direcção de topo obrigatória antes de aceitar relação com uma PEP, e revisão contínua com periodicidade **não superior a doze meses**, porque um cliente pode tornar-se PEP depois de a relação já existir. O sistema marca automaticamente `pepLastReviewedAt` como vencido após 12 meses e gera uma flag.

### 2.5 Prazos de conservação diferenciados (Artigos 86-91)

Não é um único prazo de dez anos, são cinco regimes distintos:

| Tipo de registo | Prazo | Contado a partir de | Artigo |
|---|---|---|---|
| Identificação de cliente/beneficiário efectivo | 10 anos | Encerramento da conta / cessação da relação | 86 |
| Registos de transacções | 10 anos | Conclusão da operação | 88 |
| Relatórios internos, STR, CTR enviados ao GIFiM | 10 anos | Data de elaboração | 89 |
| Constatações sobre transacções complexas/suspeitas | 10 anos | Data da constatação | 90 |
| Registos de investigação em curso | Até confirmação de encerramento pelas autoridades | Sem prazo fixo | 91 |

### 2.6 Sanções e Lista Designada Nacional (Lei 15/2023, distinta da 14/2023)

Obrigação de congelamento de activos no prazo máximo de **24 horas** após detecção de correspondência com a Lista Designada Nacional, e comunicação ao GIFiM no mesmo prazo de 24 horas (Artigos 38-40 da Lei 15/2023). O sistema trata isto como um tipo de flag com SLA de 24 horas, distinto operacionalmente das flags normais de revisão.

### 2.7 Relatório anual ao Banco de Moçambique (Artigo 116)

Reporte anual até **31 de Janeiro** do ano seguinte, em formato Excel, conforme Anexo V das Directrizes. O sistema gera este relatório automaticamente a partir dos dados agregados do ano.

---

## 3. Database schema (PostgreSQL, English naming)

### `users`
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| full_name | varchar | |
| email | varchar | unique |
| password_hash | varchar | bcrypt |
| role | varchar | ADMIN / COMPLIANCE_OFFICER / OCOS / AUDITOR_READONLY |
| mfa_enabled | boolean | default false |
| mfa_secret_encrypted | varchar | nullable, AES-encrypted at rest |
| mfa_backup_codes_hash | text[] | nullable, hashed one-time backup codes |
| created_at | timestamp | |
| last_login_at | timestamp | nullable |

### `clients`
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| full_name | varchar | |
| id_document_type | varchar | BI / NUIT / PASSPORT |
| id_document_number | varchar | |
| nuib | varchar | nullable — Número Único de Identificação Bancária (Art. 138, simplified regime) |
| risk_rating | varchar | LOW / MODERATE / HIGH, not required under SIMPLIFIED regime (Art. 141a) |
| risk_rating_updated_at | date | must refresh at least annually (Art. 29) |
| due_diligence_regime | varchar | NORMAL / SIMPLIFIED (Art. 137) |
| is_pep | boolean | |
| pep_category | varchar | nullable |
| pep_last_reviewed_at | date | nullable, max 12-month interval (Art. 66) |
| senior_management_approved | boolean | required before onboarding a PEP (Art. 65d) |
| kyc_complete | boolean | |
| created_at | timestamp | |
| updated_at | timestamp | |

### `beneficial_owners`
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| client_id | UUID | FK clients |
| full_name | varchar | |
| role | varchar | FOUNDER / ADMINISTRATOR / TRUSTEE / BENEFICIARY / CONTROLLING_PERSON (Art. 36.3, non-corporate entities/trusts) |
| ownership_percentage | numeric | nullable, ≥10% threshold triggers mandatory identification (Art. 51.1.e) |
| verified_at | date | nullable |

### `import_batches`
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| file_name | varchar | |
| format | varchar | CSV / XLSX |
| column_mapping | jsonb | |
| total_rows | int | |
| error_rows | int | |
| imported_by | UUID | FK users |
| imported_at | timestamp | |

### `transactions`
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| client_id | UUID | FK clients |
| import_batch_id | UUID | FK import_batches |
| amount | numeric(18,2) | |
| currency | varchar | default MZN |
| transaction_date | date | |
| type | varchar | DEPOSIT / WITHDRAWAL / TRANSFER / CREDIT |
| counterparty_details | jsonb | nullable, needed for reconstitution (Art. 88.e) |
| retention_until | date | = transaction_date + 10 years (Art. 88) |
| created_at | timestamp | |

### `threshold_rules`
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| name | varchar | |
| type | varchar | SINGLE_TRANSACTION_VALUE / MONTHLY_AGGREGATE_VALUE / STRUCTURING |
| value_threshold | numeric | nullable |
| aggregation_window_days | int | nullable, default 30 for monthly aggregation (Art. 129.2) |
| legal_basis | varchar | e.g. "Art. 44(3) Lei 14/2023", "Art. 82 Aviso 10/GBM/2024" |
| active | boolean | |

### `sanctions_list_entries`
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| full_name | varchar | |
| entity_type | varchar | INDIVIDUAL / ORGANIZATION |
| source_list | varchar | LISTA_DESIGNADA_NACIONAL / UN_CONSOLIDATED |
| raw_reference | varchar | identifier from source list |
| imported_at | timestamp | |

### `flags`
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| transaction_id | UUID | FK, nullable |
| client_id | UUID | FK, nullable |
| type | varchar | KYC_INCOMPLETE / THRESHOLD_VALUE / STRUCTURING_SUSPECTED / SANCTIONS_MATCH / PEP_REVIEW_DUE |
| reason | text | |
| status | varchar | PENDING / IN_REVIEW / ESCALATED_TO_STR / RESOLVED |
| sla_deadline | timestamp | nullable, = created_at + 24h for SANCTIONS_MATCH (Lei 15/2023, Art. 38-40) |
| created_at | timestamp | |
| reviewed_by | UUID | FK users, nullable |
| reviewed_at | timestamp | nullable |
| review_note | text | nullable |

### `suspicious_activity_reports` (STR — campos exactos do Art. 98)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| flag_id | UUID | FK, nullable |
| reporting_institution | varchar | |
| account_number | varchar | nullable |
| account_holder | varchar | |
| transaction_executor | varchar | nullable |
| monetary_value | numeric | nullable |
| suspicion_description | text | |
| business_relationship_description | text | |
| internal_employee_involved | boolean | |
| voluntary_statement | text | nullable |
| ocos_name | varchar | |
| gifim_registration_number | varchar | nullable (Art. 97) |
| submitted_at | timestamp | nullable |
| retention_until | date | = submitted_at + 10 years (Art. 89) |

### `currency_transaction_reports` (CTR — Art. 44/129)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| client_id | UUID | FK |
| aggregation_period_start | date | |
| aggregation_period_end | date | monthly window (Art. 129.2) |
| aggregate_value | numeric | |
| currency | varchar | |
| threshold_rule_id | UUID | FK |
| submitted_at | timestamp | nullable |
| retention_until | date | |

### `audit_log` (append-only, sem UPDATE/DELETE ao nível de permissão de BD)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| actor_id | UUID | FK users |
| action | varchar | UPLOAD / FLAG_REVIEWED / STR_SUBMITTED / MFA_ENROLLED / LOGIN |
| entity | varchar | |
| entity_id | UUID | |
| details | jsonb | |
| created_at | timestamp | |

### `system_config` (key-value, gerido via Configurações)
| Field | Type | Notes |
|---|---|---|
| key | varchar | PK — e.g. "retention.identity_years", "diligence.simplified_monthly_limit" |
| value | jsonb | |
| updated_by | UUID | FK users |
| updated_at | timestamp | |

---

## 4. GRC Engine package structure (Spring Boot, English)

```
com.fincomp
├── auth            (LoginController, JwtService, MfaEnrollmentService, MfaVerificationService)
├── users           (User entity, repository, UserManagementController)
├── clients         (Client entity, BeneficialOwner entity, service, controller)
├── ingest          (ImportController, ImportService, CsvParser, XlsxParser, ColumnMappingService)
├── compliance      (KycValidationService, DueDiligenceRegimeValidator — Art. 137/141, PepScreeningService — Art. 65/66)
├── monitoring      (ThresholdRule entity, ThresholdScreeningService, StructuringDetectionService — Art. 82)
├── sanctions       (SanctionsListEntry entity, SanctionsScreeningService, 24h freeze workflow)
├── flags           (Flag entity, repository, FlagService)
├── reporting       (StrReportService — Art. 98, CtrReportService — Art. 44/129, AnnualRegulatorReportService — Art. 116, PdfGenerator, ExcelGenerator)
├── audit           (AuditLog entity, AuditService — injectado em todos os módulos de escrita)
├── insights        (InsightsController, queries de agregação)
├── settings        (SystemConfig entity, SettingsController — utilizadores, regras, regime de diligência, retenção)
└── config          (SecurityConfig, RedisConfig, CorsConfig)
```

`resources/db/migration/`: sequência Flyway `V1` a `V12`, uma por tabela, mais migrações incrementais para ajustes futuros de esquema.

---

## 5. Redis — onde entra e porquê

Não é decoração, cada uso resolve um problema concreto de um sistema gerido por várias pessoas:

- **Revogação de sessão/refresh token**: JWT sozinho não permite logout forçado; Redis guarda a lista de tokens revogados com TTL igual à validade do token.
- **Rate limiting de login e MFA**: chave `login_attempts:{email}:{ip}` com TTL, bloqueio temporário após N tentativas falhadas, protecção contra força bruta num sistema que guarda dados de compliance sensíveis.
- **Desafio MFA pendente**: entre a validação de password e a validação do código TOTP, um `mfaChallengeToken` de curta duração (5 min) fica em Redis, nunca na base de dados principal.
- **Janela de replay do TOTP**: cada código usado fica registado por 30-60 segundos para impedir reutilização do mesmo código dentro da janela de validade.
- **Fila de processamento de importação**: uploads grandes (histórico de anos de transacções) são processados de forma assíncrona; o endpoint de upload responde de imediato e o estado do processamento fica em Redis, consultável pelo design system.
- **Cache de agregações de insights**: TTL curto (poucos minutos) para evitar recalcular agregações pesadas a cada carregamento do dashboard.

---

## 6. Autenticação com 2FA (TOTP + QR code)

Fluxo de enrolamento:

1. `POST /api/auth/mfa/enroll` (autenticado) — gera um segredo TOTP, guarda-o temporariamente em Redis (não na base de dados até confirmação), devolve um URI `otpauth://` convertido em QR code (biblioteca ZXing) para digitalizar com Google Authenticator ou Authy, mais a chave em texto para inserção manual.
2. `POST /api/auth/mfa/confirm` — utilizador insere o código gerado pela app, o GRC engine valida contra o segredo pendente, persiste `mfa_secret_encrypted` e `mfa_enabled=true` na tabela `users`, gera códigos de recuperação de uso único (guardados apenas como hash) e devolve-os uma única vez.

Fluxo de login com MFA activo:

1. `POST /api/auth/login` (email + password) — se as credenciais estão correctas e `mfaEnabled=true`, não emite JWT ainda; emite um `mfaChallengeToken` de curta duração guardado em Redis.
2. `POST /api/auth/mfa/verify` (challengeToken + código TOTP) — valida o código, confirma que não foi usado na janela de replay, emite o JWT e o refresh token.

MFA obrigatório para os papéis ADMIN e OCOS, dado que são estes que têm acesso a submissão de relatórios ao GIFiM e gestão de utilizadores. Opcional, mas recomendado, para COMPLIANCE_OFFICER.

---

## 7. API REST (principais rotas)

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/login` | passo 1, password |
| POST | `/api/auth/mfa/verify` | passo 2, código TOTP |
| POST | `/api/auth/mfa/enroll` | gera QR code |
| POST | `/api/auth/mfa/confirm` | confirma enrolamento, devolve códigos de recuperação |
| POST | `/api/import-batches` | upload multipart |
| POST | `/api/import-batches/{id}/confirm-mapping` | confirma mapeamento, processa em background |
| GET | `/api/import-batches/{id}/status` | estado do processamento assíncrono (Redis) |
| GET | `/api/clients` | lista, filtros por regime, risco, estado KYC |
| PATCH | `/api/clients/{id}/risk-rating` | actualiza classificação de risco |
| PATCH | `/api/clients/{id}/pep-review` | regista revisão de PEP |
| GET | `/api/flags?status=&type=` | fila de revisão, inclui SLA de 24h para sanções |
| PATCH | `/api/flags/{id}/review` | marca revisto |
| POST | `/api/flags/{id}/escalate-to-str` | cria rascunho de STR a partir de uma flag |
| GET | `/api/reports/str/{id}` / `POST /api/reports/str/{id}/submit` | gerar e submeter STR |
| GET | `/api/reports/ctr` | listar CTR pendentes de submissão |
| POST | `/api/reports/annual-bm` | gera relatório anual em Excel (Art. 116) |
| GET | `/api/insights/*` | completude KYC, flags por tipo, PEP a vencer, retenção a expirar |
| GET/PUT | `/api/settings/users` | gestão de utilizadores (ADMIN) |
| GET/PUT | `/api/settings/threshold-rules` | configuração de limiares |
| GET/PUT | `/api/settings/diligence-regime` | política de diligência por defeito |
| GET | `/api/audit-log` | só ADMIN, somente leitura |

---

## 8. Design System (Next.js, 100% shadcn/ui, dashboard única)

Shell persistente baseado nos blocos `dashboard-01` e `login-04` do shadcn. Secções trocam dentro do mesmo shell; detalhe e revisão abrem em `Sheet`/`Dialog`.

```
app/
├── login/page.tsx
├── mfa/page.tsx                         (passo 2 do login, input de código)
└── (dashboard)/
    ├── layout.tsx                       (SidebarProvider + AppSidebar + SidebarInset + SiteHeader)
    ├── page.tsx                         (Visão Geral: SectionCards + ChartAreaInteractive + DataTable)
    ├── import/page.tsx                  (UploadDropzone + Dialog de mapeamento + progresso assíncrono)
    ├── flags/page.tsx                   (DataTable + Sheet de revisão, badge de SLA para sanções)
    ├── clients/page.tsx                 (DataTable + Sheet de detalhe: KYC, PEP, beneficiários efectivos)
    ├── reports/page.tsx                 (STR, CTR, relatório anual — Form + DataTable + download)
    └── settings/page.tsx                (utilizadores, regras de limiar, regime de diligência, MFA enrollment)
components/
├── app-sidebar.tsx                      (nav-main: Visão Geral, Importar, Flags, Clientes, Relatórios, Configurações)
├── site-header.tsx
├── section-cards.tsx
├── chart-area-interactive.tsx
├── data-table.tsx
├── upload-dropzone.tsx
├── flag-review-sheet.tsx
├── client-detail-sheet.tsx
├── mfa-enroll-dialog.tsx                (exibe QR code + input de confirmação)
└── login-form.tsx
lib/
├── api-client.ts
└── types.ts
```

**Comandos de scaffold:**
```
npx shadcn@latest init
npx shadcn add dashboard-01
npx shadcn add login-04
npx shadcn add sheet dialog form calendar popover badge select
```

---

## 9. Deploy AWS (single-tenant por cliente)

EC2 `t4g.small` por cliente, Docker Compose com cinco serviços:

```
services:
  app:       # Spring Boot, porta 8080
  design system:  # Next.js, porta 3000
  db:        # Postgres 16
  redis:     # Redis 7, sessões/rate-limit/fila de importação
  caddy:     # reverse proxy + TLS automático
```

Segredos (password DB, chave de encriptação AES para `mfa_secret_encrypted`, chave JWT) via variáveis de ambiente Docker, nunca em código. Backup diário da base de dados via cron para S3.

---

## 10. Plano de execução — honestidade sobre o prazo

Com a profundidade legal, Redis, 2FA e configurações agora incluídas, catorze dias cobre o **núcleo legalmente defensável**, não o sistema completo. Faseamento realista:

**Fase 1 — catorze dias, MVP legalmente sólido:**
| Dias | Entrega |
|---|---|
| 1 | Scaffold Spring Boot, Flyway, Docker Compose com Redis |
| 2 | Entidades e migrações (users, clients, beneficial_owners, transactions, flags) |
| 3 | Auth com JWT + MFA/TOTP completo (enroll, confirm, verify, QR code) |
| 4-5 | Ingest (parsing síncrono para volumes pequenos, mapeamento de colunas) |
| 6 | Compliance: validação KYC, regime NORMAL/SIMPLIFIED (Art. 137), PEP (Art. 65/66) |
| 7 | Monitoring: threshold screening + detecção de fraccionamento (Art. 82) |
| 8 | Flags com SLA de 24h para sanções, audit log integrado |
| 9 | Reporting: STR com campos do Art. 98, CTR |
| 10 | Insights |
| 11-12 | Design System Next.js/shadcn completo |
| 13 | Settings (gestão de utilizadores, regras) |
| 14 | Deploy AWS, teste com dados reais de um cliente |

**Fase 2 — após a primeira venda, não bloqueia o lançamento:**
- Fila assíncrona real via Redis para importações grandes (Fase 1 usa processamento síncrono, suficiente para o volume de dois clientes pequenos)
- Sincronização automática da Lista Designada Nacional e listas da ONU (Fase 1 usa carregamento manual da lista)
- Geração automática do relatório anual ao Banco de Moçambique em Excel (Art. 116)
- Módulo de registo de formação de funcionários (Art. 114), relevante para auditoria mas não bloqueia a venda inicial
