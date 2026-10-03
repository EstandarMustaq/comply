---
id: 0007
title: Commercial entitlements and quotas
status: draft
branch: task/0007-commercial-entitlements-and-quotas
scope:
  - apps/api/src/main/java/com/complyz/**/commercial/**
  - apps/api/src/main/java/com/complyz/**/entitlement/**
  - apps/api/src/main/java/com/complyz/**/quota/**
  - apps/api/src/main/resources/db/migration/V*__*.sql
  - apps/api/src/test/**
  - packages/api-clients/**
  - apps/ui/lib/types.ts
  - apps/ui/lib/api-client.ts
  - packages/ui/tests/contract/**
reserved:
  - apps/ui/app/**
  - apps/ui/components/**
  - docs/**/*.mdx
depends_on:
  - 0001
  - 0003
  - 0006
---

# 0007 Commercial entitlements and quotas

## Context

The target puts contracts, modules, limits, billing and commercial contacts on CommercialClient, with explicit Organization allocation. Proposed defaults say an Organization initially receives contracted modules and shares CommercialClient quota until allocated otherwise. Regulatory duties must continue after quota exhaustion. Exact commercial behavior awaits task 0001 approval.

## Objective

Persist and enforce contract entitlements and Organization allocations on the server. Enforce quota on the command path in the same transaction as the action with a concurrency-safe counter; expose accurate status and auditable 80%/100% usage alerts without blocking compliance obligations.

## Acceptance criteria

- [ ] `Entitlement` records module, limit and effective term at CommercialClient; `OrganizationEntitlement` records explicit module activation/quota allocation, with defaults only as approved and visible. CommercialClient contract, billing contacts and related commercial metadata are persisted and exposed only to authorized commercial roles.
- [ ] Quota checks are server-side, command-side and atomic with writes; concurrent requests cannot overrun limits through a check-then-insert race. Idempotent retries do not double-count.
- [ ] Exhaustion blocks only approved capacity-expanding operations (e.g. additional ingested records/users). Regulatory report access/submission, exports, retention, reads and audit continue as required by the target and approved decision.
- [ ] 80% and 100% alerts reach approved recipients and are deduplicated/idempotent; limit-reached state is audited. Counters can be reconciled to source data and repaired operationally.
- [ ] Commercial APIs require commercial context and role; they reveal no Organization compliance records or cross-Organization aggregates unless a separate approved and tested permission expressly permits a bounded view.
- [ ] Tests cover term boundaries, shared and allocated limits, races, retry, alert thresholds, exhausted exceptions and commercial/operational denial. API contract types/tests are synchronized; `pnpm api:test` passes.

## Verification

```bash
pnpm api:test
pnpm check
```

Use database-backed concurrency tests for quota behavior; mocks cannot prove transactional enforcement.

## Operational considerations

Quota semantics affect contractual obligations and must not be enabled before approval. Migration/backfill of legacy customers requires a documented entitlement mapping and no inferred charge/billing event. Provide monitoring for rejected commands, counter drift, alert failures and reconciliation; define a safe manual correction path.

## Work log

- 2026-10-03: Brief created; entitlement defaults and billable limits remain subject to task 0001.
