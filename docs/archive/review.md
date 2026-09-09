# Revisão Completa do Backend

**Data:** 2026-09-06  
**Escopo:** Backend, arquitetura, plano de implementação, fases funcionais, segurança, conformidade, configuração, scripts, práticas SaaS/micro-SaaS e FinOps.  
**Alterações realizadas:** Nenhuma alteração em código, configuração ou scripts.

## Base da revisão

A revisão completa do backend foi concluída sem alterar código, arquivos ou configuração. O estado atual está em `main`, porém o repositório não possui commits; todos os arquivos estão sem histórico versionado. Portanto, é possível confirmar o que existe hoje, mas não verificar evolução histórica ou aderência a commits anteriores.

## Estado de implementação por fase

| Fase | Estado | Conclusão |
|---|---|---|
| 1. Infraestrutura, PostgreSQL, Redis e Flyway | **Parcial** | Compose, Spring Boot, Redis e 12 migrações existem; faltam testes de migração, integração e CI. |
| 2. JWT, MFA, criptografia, rate limiting e RBAC | **Parcial / inseguro** | Existem JWT, TOTP, AES-GCM, códigos de recuperação e Redis, mas há credenciais padrão, falhas de autorização, confusão entre access/refresh token e MFA sem proteção suficiente. |
| 3. Importação CSV/XLSX | **Parcial** | Parsers e mapeamento existem, mas o processamento é síncrono, mantido em memória, sem ownership, idempotência, streaming ou limpeza de uploads abandonados. |
| 4. KYC, regime simplificado, beneficiários e PEP | **Parcial** | Há campos, validação básica e job de PEP; faltam transição legal de regime, limites anuais, NUIB, beneficiários efetivos e KYC completo. |
| 5. Threshold e structuring monitoring | **Parcial / incorreto** | Serviços existem, mas agregam tipos indevidos, geram duplicidades e não implementam corretamente o conceito de structuring abaixo do limite. |
| 6. Sanções, SLA de 24h e auditoria | **Parcial / inseguro** | Há seed, tentativa de fuzzy matching e trigger de auditoria, mas não existe bloqueio efetivo de ativos, sincronização de listas ou fail-closed. |
| 7. STR, CTR, relatório anual e insights | **Parcial** | Entidades e endpoints existem; faltam PDF, Excel anual, campos legais completos, controle de estado imutável e restrições de submissão. |
| 8. Frontend Next.js/shadcn | **Compilável, mas desconectado** | O frontend compila, porém diversas rotas, verbos, campos e respostas não correspondem ao backend. |
| 9. AWS, backups e operação | **Parcial / não produtivo** | Existem Dockerfiles, Caddy, Compose e scripts, mas backup seguro, Object Lock, restore testing, health checks, limites e gestão adequada de secrets não estão concluídos. |
| CI/CD e regressão automatizada | **Ausente** | Não foram encontrados workflows de CI/CD, testes backend, testes de regras de compliance ou testes de contrato. |

## Vulnerabilidades e inconformidades prioritárias

| # | Severidade | Arquivo / linhas | Problema |
|---:|---|---|---|
| 1 | **Crítica** | `backend/src/main/java/com/fincomp/config/DataInitializer.java:44-63` | Usuários `admin` e `ocos` são criados automaticamente com senhas hardcoded conhecidas. Isso permite comprometimento imediato de novas instalações. |
| 2 | **Alta** | `flags/controller/FlagController.java:33-66`; `reporting/controller/ReportController.java:39-129`; `clients/controller/ClientController.java:68-149` | Endpoints estão autenticados, mas não devidamente autorizados por função. Usuários comuns e `AUDITOR_READONLY` podem revisar flags, alterar clientes e submeter STR/CTR. |
| 3 | **Alta** | `frontend/lib/api-client.ts:170-269` | O contrato frontend/backend está incompatível: rotas, métodos HTTP, nomes de campos e formatos de resposta divergem. Fluxos de dashboard, importação, flags, clientes e STR podem falhar em produção. |
| 4 | **Alta** | `auth/service/JwtService.java:58-71,106-116`; `JwtAuthenticationFilter.java:44-60` | Refresh tokens possuem `tokenType=REFRESH`, mas esse claim não é validado no filtro. Um refresh token válido funciona como access token por até sete dias. |
| 5 | **Alta** | `auth/service/MfaVerificationService.java:80-97,126-134` | Verificação MFA não possui rate limiting adequado. O replay TOTP utiliza sequência não atômica `hasKey` + validação + `set`; códigos podem ser reutilizados sob concorrência. |
| 6 | **Alta** | `sanctions/service/SanctionsScreeningService.java:44-52` | O fuzzy matching só é executado depois de uma busca `LIKE` pelo nome completo. Erros de grafia, ordem dos nomes, acentos ou transliterações podem impedir qualquer candidato de chegar ao algoritmo fuzzy. |
| 7 | **Alta** | `sanctions/service/SanctionsScreeningService.java:60-66` | Uma correspondência de sanções apenas cria uma flag e aumenta o risco; não há congelamento, bloqueio de transações ou estado de conta sancionada. |
| 8 | **Alta** | `config/DataInitializer.java:66-83` | Falha ao carregar a lista de sanções é registrada, mas não interrompe a aplicação. O sistema pode operar com tabela vazia ou incompleta, em modo fail-open. |
| 9 | **Alta** | `compliance/service/DueDiligenceRegimeValidator.java:36-57` | Limite anual é declarado mas não utilizado; não há enforcement de NUIB, conta única ou autorização institucional; o cliente não é automaticamente promovido de `SIMPLIFIED` para `NORMAL`. |
| 10 | **Alta** | `compliance/service/PepScreeningService.java:35-40` | PEPs com `pepLastReviewedAt = NULL` nunca entram na consulta de revisão vencida. PEPs nunca revisados podem permanecer indefinidamente sem alerta. |
| 11 | **Alta** | `ingest/service/ImportService.java:90-125,130-140,156-210`; parsers | Uploads inteiros são mantidos em memória e em `ConcurrentHashMap`, sem TTL, limite de linhas, ownership, lock ou idempotência. Há risco de OOM e duplicação por confirmações concorrentes. |
| 12 | **Alta** | `ingest/service/ImportService.java:230-246` | Datas inválidas ou ausentes são substituídas por `LocalDate.now()`. Isso corrompe períodos regulatórios, agregações, retenção, CTR/STR e evidências históricas. |
| 13 | **Alta** | `monitoring/service/ThresholdScreeningService.java:46-81`; `StructuringDetectionService.java:42-66` | Regras agregam tipos de transação inadequados, usam janela temporal potencialmente inclusiva demais, não exigem operações individuais abaixo do threshold e geram flags/CTRs duplicados. |
| 14 | **Alta** | `reporting/controller/ReportController.java:91-129` | STR/CTR podem ser submetidos repetidamente; `submittedAt`, estado e retenção podem ser sobrescritos. Não há transição imutável de estado nem unicidade adequada. |
| 15 | **Alta** | `scripts/backup-s3.sh:14-19,40-52` | Ausência de `BACKUP_ENCRYPTION_PASSPHRASE` gera apenas warning e permite upload de dump plaintext. Não há Object Lock, retenção, versionamento, checksum, restore test ou agendamento configurado. |
| 16 | **Alta** | `.env:1-16` | O `.env` contém secrets de banco, Redis, JWT e MFA com permissões `0644`, permitindo leitura por outros usuários locais. Os valores atuais também devem ser considerados comprometidos caso tenham sido compartilhados. |

## Lacunas funcionais relevantes

- Não foi encontrada implementação de `AnnualRegulatorReportService`.
- Não existe gerador PDF para STR.
- Não existe geração do relatório anual BM em Excel.
- Não existe controller ou fluxo completo de beneficiário efetivo.
- Não existe serviço completo de validação de KYC.
- Não existe sincronização ou atualização versionada das listas de sanções.
- Importação não possui processamento assíncrono real nem endpoint de status persistente.
- Não há testes backend automatizados relevantes; o `mvn test` passa sem fontes de teste backend.
- Não há testes de autorização por papel.
- Não há testes de regras de threshold, structuring, PEP, sanções, KYC ou migrações.
- Não há testes de contrato frontend/backend.
- Não há workflows de CI/CD.
- Compose de produção não possui health checks completos para aplicação, frontend e Caddy.
- O script de deploy trata containers iniciados como prontos, sem readiness real.
- Não há limites explícitos de CPU/memória para produção.
- Não há métricas, alertas, tracing ou observabilidade operacional suficiente.
- Não há coordenação clara entre heap JVM e limite de memória do container.
- Não há controles consistentes de custo AWS, retenção, lifecycle ou orçamento.

## Conformidade com práticas modernas de SaaS/micro-SaaS

A arquitetura atual tem uma base inicial funcional, mas ainda não está pronta para um SaaS regulado multi-tenant:

- Não foi identificada separação explícita por tenant/organização em todas as entidades e consultas.
- Não há política completa de isolamento de dados entre clientes empresariais.
- Não há versionamento formal de API.
- Não há contrato OpenAPI utilizado para gerar clientes ou DTOs.
- Não há idempotency keys para operações de importação, submissão e processamento.
- Não há estratégia clara de retries, dead-letter queue ou processamento assíncrono.
- Não há workflow formal de auditoria para alterações críticas.
- Não há segregação de funções efetivamente aplicada.
- Não há controle completo de lifecycle de dados, retenção e anonimização.
- Não há mecanismo consolidado de feature flags, billing, quotas, usage metering ou limites por plano.
- Não há CI com SAST, dependency scanning, secret scanning, SBOM ou validação de migrações.

## FinOps e eficiência operacional

As principais não conformidades de FinOps são:

- Falta de budgets e alertas de custo.
- Falta de tagging obrigatório por ambiente, tenant, serviço e centro de custo.
- Falta de limites de recursos no Docker Compose.
- Uploads em memória podem aumentar custos por OOM, reinícios e instâncias maiores.
- Falta de lifecycle policies para objetos e backups.
- Falta de política formal de retenção e expiração de artefatos.
- Não há evidência de rightsizing, autoscaling ou revisão de capacidade.
- Backups podem crescer indefinidamente sem retenção técnica aplicada.
- Não há métricas de custo por tenant ou por volume processado.
- Não há análise de custo de logs, armazenamento, Redis ou banco.
- Não há processo documentado para restore drills e validação de custo/tempo de recuperação.

## Roadmap recomendado

### P0 — antes de qualquer cliente real

1. Remover usuários e senhas hardcoded; rotacionar secrets atuais.
2. Aplicar autorização por função em todos os endpoints mutáveis.
3. Separar rigorosamente access token e refresh token.
4. Implementar refresh rotation, revogação e detecção de reuse.
5. Adicionar rate limiting e consumo atômico de MFA.
6. Fazer carregamento de sanções fail-closed.
7. Implementar bloqueio/freeze efetivo após match.
8. Corrigir o contrato frontend/backend.
9. Fazer backup falhar quando não houver criptografia.
10. Corrigir permissões do `.env` e migrar secrets para AWS Secrets Manager/SSM.

### P1 — correção regulatória

1. Corrigir regime simplificado, limites mensais/anuais e transição automática.
2. Incluir PEPs nunca revisados.
3. Corrigir threshold/structuring e tornar geração de CTR idempotente.
4. Rejeitar datas inválidas em vez de corrigi-las silenciosamente.
5. Tornar STR/CTR uma máquina de estados imutável.
6. Completar campos legais, PDF e relatório anual.
7. Implementar beneficiário efetivo e validação KYC.

### P2 — maturidade operacional

1. Migrar importações para armazenamento durável e processamento assíncrono.
2. Adicionar testes de integração, autorização, compliance, migração e contratos.
3. Criar pipeline CI/CD com scanning de dependências e secrets.
4. Implementar health checks, métricas, alertas e restore drills.
5. Adicionar limites de recursos e políticas FinOps.
6. Versionar, auditar e atualizar listas de sanções.
7. Implementar isolamento multi-tenant, quotas, usage metering e controles de custo.

## Conclusão

O projeto possui uma base técnica funcional e compilável, mas está em estado **parcial de implementação**. Os problemas de credenciais padrão, autorização, tokens, sanções, backups, importação e contrato frontend/backend são bloqueadores de produção. A solução ainda não atende integralmente aos requisitos regulatórios, de segurança, SaaS multi-tenant ou FinOps descritos na arquitetura.
