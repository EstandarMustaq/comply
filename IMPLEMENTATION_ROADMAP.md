# FinComp — Plano de Implementação da Revisão

**Data:** 2026-09-09  
**Documento de origem:** `review.md`  
**Commit-base:** `fcd45dd chore: establish versioned project baseline`  
**Objetivo:** transformar as conclusões da revisão em uma sequência controlada de branches, commits verificáveis e entregas incrementais.

## 1. Estado inicial confirmado

O repositório agora possui histórico versionado com um commit-base. O commit inclui o estado existente do projeto, a documentação disponível e os testes/scripts já presentes, sem incluir `.env` ou `frontend/.env.local`.

O sistema está em estado **parcial de implementação**. Nenhuma entrega deve ser considerada pronta para produção antes da conclusão do bloco P0 e da validação dos critérios de aceitação correspondentes.

## 2. Regras de execução e versionamento

Cada bloco de trabalho deve seguir este ciclo:

1. Criar branch a partir de `main` atualizado.
2. Implementar somente o escopo do bloco.
3. Adicionar ou atualizar testes do comportamento alterado.
4. Executar a validação definida no bloco.
5. Criar um commit follow-up com mensagem específica.
6. Abrir pull request para `main` e fazer revisão antes do merge.
7. Após o merge, preservar o branch de trabalho para manter o histórico, facilitar auditorias, comparações e rollback. O branch só poderá ser removido mediante decisão explícita e documentada.

Branches de documentação podem ser mescladas diretamente após revisão. Branches de código devem ser independentes e pequenos o suficiente para permitir rollback.

### Convenção de branches

```text
docs/implementation-roadmap
fix/p0-bootstrap-secrets
fix/p0-rbac-authorization
fix/p0-token-lifecycle
fix/p0-mfa-hardening
fix/p0-sanctions-blocking
fix/p0-backup-secrets
fix/p0-api-contract
fix/p1-compliance-regime-pep
fix/p1-monitoring-idempotency
fix/p1-import-validation
feat/p1-kyc-beneficial-owners
fix/p1-reporting-state
feat/p1-regulatory-exports
refactor/p2-import-pipeline
ci/p2-quality-gates
ops/p2-observability-finops
feat/p2-multitenancy-controls
ops/p2-sanctions-data-lifecycle
```

### Convenção de commits

Usar Conventional Commits e incluir o trailer:

```text
Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
```

Exemplos:

```text
fix(auth): reject refresh tokens in API authentication
test(compliance): cover overdue and never-reviewed PEPs
ci: add backend, frontend and migration quality gates
```

## 3. Ordem de implementação

### P0 — Bloqueadores antes de clientes reais

| Ordem | Branch | Entrega | Critério de conclusão |
|---:|---|---|---|
| 1 | `fix/p0-bootstrap-secrets` | Remover criação automática com credenciais hardcoded; criar bootstrap seguro, rotação obrigatória e proteção de secrets. | Nenhuma senha de produção no código; inicialização sem credenciais conhecidas; teste de bootstrap e rotação. |
| 2 | `fix/p0-rbac-authorization` | Aplicar autorização por método/endpoint para ADMIN, COMPLIANCE_OFFICER, OCOS e AUDITOR_READONLY. | Matriz RBAC da arquitetura refletida no backend; testes positivos e negativos por papel. |
| 3 | `fix/p0-token-lifecycle` | Separar access/refresh tokens, validar `tokenType` e audience, criar refresh rotation, revogação e reuse detection. | Refresh token não acessa APIs; refresh endpoint protegido por rotação; logout e revogação testados. |
| 4 | `fix/p0-mfa-hardening` | Rate limiting de MFA, limites por usuário/IP/desafio, consumo atômico de TOTP e backup codes. | Brute force bloqueado; replay concorrente rejeitado; desafio invalidado após sucesso. |
| 5 | `fix/p0-sanctions-blocking` | Corrigir candidate retrieval/fuzzy matching, carregamento fail-closed, estado de congelamento, bloqueio de transações e SLA de 24h. | Match positivo, typo e nome reordenado cobertos; sistema não sobe com lista inválida; transação bloqueada após freeze. |
| 6 | `fix/p0-backup-secrets` | Backup fail-closed, criptografia obrigatória, gestão de Object Lock/versionamento e restore drill documentado. | Ausência de chave impede upload; backup criptografado; restore validado em ambiente isolado. |
| 7 | `fix/p0-api-contract` | Alinhar frontend/backend e criar contrato OpenAPI ou DTOs compartilhados. | Login, upload, dashboard, flags, clientes e STR funcionam ponta a ponta. |

**Gate P0:** não liberar produção sem todos os sete branches mesclados, testes automatizados verdes e revisão manual dos fluxos regulatórios críticos.

### P1 — Correção regulatória e integridade dos dados

| Ordem | Branch | Entrega | Critério de conclusão |
|---:|---|---|---|
| 8 | `fix/p1-compliance-regime-pep` | Implementar limites mensais e anuais, NUIB/pré-requisitos, transição para NORMAL, KYC completeness e PEPs nunca revisados. | Casos de limite, transição, KYC incompleto e `NULL pepLastReviewedAt` cobertos. |
| 9 | `fix/p1-monitoring-idempotency` | Corrigir threshold/structuring por tipo, janela e regra legal; tornar flags e CTRs idempotentes. | Registros não duplicam em retry/reprocessamento; structuring exige operações individuais abaixo do limiar. |
| 10 | `fix/p1-import-validation` | Rejeitar datas inválidas, preservar erros por linha e validar amount/currency/documento. | Nenhuma correção silenciosa para data; lote informa erros acionáveis e não grava evidência adulterada. |
| 11 | `feat/p1-kyc-beneficial-owners` | Criar workflow completo de beneficiários efetivos, aprovação PEP e completude KYC. | CRUD auditado, regras de ownership aplicadas e autorização testada. |
| 12 | `fix/p1-reporting-state` | Tornar STR/CTR máquinas de estado imutáveis, completar campos legais e aplicar unicidade/submissão única. | Segunda submissão é rejeitada; retenção e estado não podem ser sobrescritos; payload legal completo. |
| 13 | `feat/p1-regulatory-exports` | Implementar PDF STR e relatório anual BM em Excel. | Arquivos gerados com dados obrigatórios, formato validado e testes de exportação. |

**Gate P1:** todas as regras regulatórias devem possuir testes de unidade e integração, além de evidência de auditoria para cada transição crítica.

### P2 — Maturidade operacional, SaaS e FinOps

| Ordem | Branch | Entrega | Critério de conclusão |
|---:|---|---|---|
| 14 | `refactor/p2-import-pipeline` | Armazenamento durável, streaming, limites, TTL, processamento assíncrono, ownership, locks e idempotency keys. | Upload grande não causa OOM; lote abandonado expira; retry não duplica dados. |
| 15 | `ci/p2-quality-gates` | CI/CD com build, testes, Flyway limpo, contract tests, SAST, dependency/secret scanning e SBOM. | Pull request bloqueado quando qualquer gate falha. |
| 16 | `ops/p2-observability-finops` | Health/readiness checks, métricas, alertas, logs estruturados, limites de recursos, budgets, tags e lifecycle policies. | Incidentes e custos básicos são observáveis; backup, banco e Redis possuem limites e retenção. |
| 17 | `feat/p2-multitenancy-controls` | Isolamento por instituição/tenant, quotas, usage metering, billing hooks e políticas de retenção. | Nenhuma consulta cruza tenant; quotas e consumo são auditáveis. |
| 18 | `ops/p2-sanctions-data-lifecycle` | Versionamento, origem, checksum, validade e atualização controlada das listas de sanções. | Cada lista é rastreável, atualizável e revertível sem operação manual não auditada. |

## 4. Dependências entre entregas

```text
bootstrap-secrets
  -> rbac-authorization
  -> token-lifecycle
  -> mfa-hardening
  -> sanctions-blocking
  -> backup-secrets
  -> api-contract
  -> compliance-regime-pep
  -> monitoring-idempotency
  -> import-validation
  -> kyc-beneficial-owners
  -> reporting-state
  -> regulatory-exports
  -> import-pipeline
  -> quality-gates
  -> observability-finops
  -> multitenancy-controls
  -> sanctions-data-lifecycle
```

Na prática, `rbac-authorization`, `token-lifecycle`, `mfa-hardening`, `sanctions-blocking` e `backup-secrets` podem ser desenvolvidos em paralelo depois do branch de bootstrap, mas devem ser integrados e validados antes do gate P0.

## 5. Estratégia de testes por camada

### Backend

- Testes unitários para serviços de autenticação, MFA, sanções, KYC, PEP, threshold e structuring.
- Testes de integração com PostgreSQL, Redis e Flyway.
- Testes de autorização por endpoint e papel.
- Testes de concorrência para replay, backup codes, confirmação de importação e submissão de relatórios.
- Testes de idempotência para retry e reprocessamento.

### Frontend e contrato

- Testes de contrato para cada chamada em `app/application/lib/api-client.ts`.
- Testes de fluxo para login/MFA, upload, mapeamento, revisão de flag, cliente e STR.
- Validação de DTOs e erros HTTP reais, não apenas build estático.

### Operação

- `docker compose config` para configurações.
- Build backend e frontend.
- Smoke tests com ambiente limpo.
- Restore de backup em banco temporário.
- Verificação de permissões de arquivos e ausência de secrets rastreados.

## 6. Critérios de release

Uma release somente pode ser promovida quando:

1. O branch contém apenas o escopo planejado.
2. O commit possui testes e documentação do comportamento alterado.
3. Build, testes e migrações passam em ambiente limpo.
4. Não existem secrets no Git ou artefatos de build rastreados.
5. As mudanças de segurança possuem teste de negação, não apenas caminho feliz.
6. Regras regulatórias possuem base legal e evidência de auditoria.
7. O rollback da migração e do código foi avaliado.
8. O pull request foi revisado antes do merge.

## 7. Estado de execução

- [x] Criar histórico inicial do projeto.
- [x] Excluir arquivos locais de ambiente do versionamento.
- [x] Registrar o roadmap baseado em `review.md`.
- [x] Implementar e mesclar o bloco P0.
- [x] Implementar e mesclar o bloco P1.
- [x] Implementar e mesclar o bloco P2.
- [ ] Executar homologação regulatória final.
- [ ] Autorizar produção.

Próxima fase — Documentação de produto e modularização:

- [ ] Documentar todos os endpoints, contratos, comportamento e arquitetura em `docs/`.
- [ ] Consolidar `README.md`, `ROADMAP.md` e a linguagem Financial Compliance Intelligent.
- [ ] Aplicar o bloqueio de referências legais em UI, backend e respostas de produto.
- [ ] Unificar comandos de workspace na raiz.
- [x] Migrar `package/compliance-engine/` e `app/application/` para submódulos Git com histórico local preservado e URLs configuradas; [ ] publicar os commits nos remotos e validar clone externo.

Observação: os blocos de implementação P0, P1 e P2 foram executados em branches específicas, validados e mesclados em `main`, preservando os branches de trabalho. O estado atual da entrega é "funcional para evolução operacional/cumprimento, mas ainda pendente de homologação regulatória final e autorização de produção".
