---
id: 0007
title: Organization entitlements and quotas
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

# 0007 Organization entitlements and quotas

## Context

The approved model stores each contract entitlement and enforced quota on the Organization. `CommercialClient` is a segregated back-office reference containing identifier, name, contract reference and status; it neither groups/manages Organizations nor supplies entitlement defaults. Organization entitlements are configured independently through their owning context. Regulatory duties continue after quota exhaustion.

## Objective

Persist and enforce Organization-owned contract entitlements and quota decisions on the server. Enforce limits on the owning command path with concurrency-safe aggregate versions and event appends; expose accurate status and auditable 80%/100% alerts without blocking compliance obligations.

## Acceptance criteria

- [ ] `Entitlement` records each Organization's modules, hard/soft limits and effective term. CommercialClient stores only its back-office reference metadata; an Organization's entitlement is assigned and changed through the owning Organization context, never inherited or defaulted from CommercialClient. Changing CommercialClient name, contract reference or status has no automatic effect on Organization entitlements, quotas, lifecycle or authorization.
- [ ] Quota checks run in the owning context, are command-side and atomic with the aggregate event append; optimistic concurrency prevents races and idempotent retries do not double-count. Rebuilt projections/counters agree with the event stream.
- [ ] Hard limits block member seats, uncontracted modules and optional extra capacity. Soft limits apply to transaction-ingestion and monitored KYC-client volume with overage handling; they do not stop screening/monitoring of already ingested data. Regulatory reports/submission, reads, exports, retention and audit are never blocked by quota exhaustion.
- [ ] 80% and 100% alerts reach Organization `OWNER`/`ADMIN` and platform back office, are deduplicated/idempotent and audited. Contract expiry enters a 30-day read/export grace period without new capacity.
- [ ] Platform back-office APIs require their separate identity and permission boundary. They reveal no Organization compliance records and cannot administer Organization aggregates by direct write; changes are issued through the owning context.
- [ ] Tests cover term boundaries, per-Organization entitlement assignments, hard/soft limits, races, retry, alert thresholds, exhausted exceptions and platform/compliance denial. API contract types/tests are synchronized; `pnpm api:test` passes.

## Verification

```bash
pnpm api:test
pnpm check
```

Use database-backed optimistic-concurrency/event-store tests for quota behavior; mocks cannot prove atomic enforcement, replay consistency or race handling.

## Operational considerations

Quota semantics affect contractual obligations. Migration/backfill of legacy Organizations requires a documented entitlement mapping and no inferred charge/billing event. Provide monitoring for rejected commands, projection drift, event append failures, alert failures and reconciliation; define a safe repair path that records a compensating domain event rather than editing event history.

## Work log

- 2026-10-03: Brief created from proposed entitlement rules.
- 2026-10-04: Aligned with task 0001: Organization-owned entitlements, no CommercialClient-derived defaults, hard/soft limits and compliance-safe grace/exhaustion behavior.
