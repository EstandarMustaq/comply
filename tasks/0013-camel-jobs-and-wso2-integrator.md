---
id: 0013
title: Camel jobs and WSO2 Integrator boundary
status: draft
branch: task/0013-camel-jobs-and-wso2-integrator
scope:
  - apps/api/src/main/java/com/complyz/**/integration/**
  - apps/api/src/main/java/com/complyz/**/outbox/**
  - apps/api/src/main/java/com/complyz/ingest/**
  - apps/api/src/main/resources/**
  - apps/api/src/test/**
  - packages/contracts/**
  - docker-compose.yml
  - docs/integrations/**
reserved:
  - apps/api/src/main/java/com/complyz/auth/**
  - apps/ui/app/**
  - docs/**/*.mdx
depends_on:
  - 0001
  - 0002
  - 0006
  - 0007
  - 0009
---

# 0013 Camel jobs and WSO2 Integrator boundary

## Context

The approved Context Map assigns Apache Camel to internal asynchronous workflows (event publication, CSV/XLSX ingestion, sanctions updates and regulatory exports) and WSO2 Integrator to external institution mediation (core banking, mobile money, legacy REST/SOAP/files). WSO2 Integrator is distinct from WSO2 Identity Server and remains conditional on an approved external use case.

## Objective

Make asynchronous/internal workflows tenant-aware and reliable with business-named, versioned event contracts, idempotent consumers, bounded retries/backoff and dead-letter handling. Implement the task 0009 Context Map and adapter boundaries; deploy/configure WSO2 Integrator only for an approved first external use case.

## Acceptance criteria

- [ ] Internal jobs carry trusted Organization and integration identity end-to-end; no job accepts an unvalidated tenant ID or accesses a global customer-data repository. Only the owning bounded context may change the aggregate.
- [ ] Outbox, ingestion, sanctions refresh and export routes use the versioned event envelope/catalog, idempotent processing, bounded retry/backoff, dead-letter/quarantine and replay controls; duplicate delivery, ordering, partial failure and replay safety are tested.
- [ ] CSV/XLSX ingestion validates against the versioned JSON Schema catalog before mutation; validation errors identify safe actionable issues without leaking other tenants' data. Unknown/incompatible versions fail closed or enter a controlled quarantine.
- [ ] Integrations use separate client-credentials identities per Organization and integration, least scopes, rotation/expiry and audit; a human Account cannot be used as an integration principal. Protected external resource requests enforce task 0006's mandatory `Authorization` and request-body `Content-Type` headers.
- [ ] WSO2 Integrator deployment/adapters are gated on a named approved external use case, protocol/ownership decision and operational support plan. The IdP remains WSO2 Identity Server; no duplicated auth responsibilities.
- [ ] Metrics/alerts cover queue age, retries, DLQ, schema failure, tenant identity loss, token-expiry/auth failure and export delivery. Operator runbook defines redrive authority, duplicate protection and tenant verification.
- [ ] Tests prove tenant context and data isolation through each enabled route; `pnpm api:test`, `pnpm operations:test` and `pnpm compose:config` pass.

## Verification

```bash
pnpm api:test
pnpm operations:test
pnpm compose:config
```

If no external mediation use case is approved, validate the adapter boundary and explicitly defer deployment/configuration of WSO2 Integrator; do not mark it as delivered.

## Operational considerations

Never replay a message into a different Organization context or bypass the aggregate-owning context. DLQ payloads can contain regulated data and need least-privilege access, retention and redaction. Define contract versions, idempotency keys, ordering, retry caps and recovery behavior before enabling production consumers.

## Work log

- 2026-10-03: Brief created; Camel/Integrator are target architecture, not verified runtime dependencies in this planning change.
- 2026-10-04: Aligned with ratified task 0001 and 0009: context-owned event streams, business-named versioned contracts, schema catalog and resource-server headers.
