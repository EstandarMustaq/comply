---
id: 0002
title: Contain cross-Organization access
status: draft
branch: task/0002-contain-cross-organization-access
scope:
  - apps/api/src/main/java/com/complyz/audit/**
  - apps/api/src/main/java/com/complyz/clients/**
  - apps/api/src/main/java/com/complyz/flags/**
  - apps/api/src/main/java/com/complyz/ingest/**
  - apps/api/src/main/java/com/complyz/insights/**
  - apps/api/src/main/java/com/complyz/monitoring/**
  - apps/api/src/main/java/com/complyz/reporting/**
  - apps/api/src/main/java/com/complyz/sanctions/**
  - apps/api/src/test/java/com/complyz/**
reserved:
  - apps/api/src/main/java/com/complyz/auth/**

  - apps/api/src/main/java/com/complyz/tenancy/**
  - apps/api/src/main/resources/db/migration/**
depends_on: []
---

# 0002 Contain cross-Organization access

## Context

The current single-Organization model has known tenant-sensitive paths. In the reviewed baseline, `FlagService` uses global list/ID lookups, `InsightsService` counts globally and caches under one key, report list/detail/update/submit routes use global repository methods, and `AuditLogController` lists globally. `ClientController` lists KYC records without the current Organization predicate; `KycController` lists/adds/verifies beneficial owners and recalculates by global IDs; `ImportController` lists/details batches globally. User listing loads all rows before filtering in memory. Beneficiary, report, flag and indicator A/B findings have also been reported as reproducible. Existing Organization scoping on annual exports or isolated repository methods does not secure other call paths.

## Objective

Before expanding tenancy, close the known cross-Organization read/write paths in the current model. Derive Organization from the authenticated principal in the service/API boundary, scope direct and indirect relations, and fail closed when context is absent. Audit every endpoint and repository in scope, including import batches, jobs, exports and aggregates. Do not add CommercialClient semantics in this containment task.

## Acceptance criteria

- [ ] A route-to-data-access inventory covers all customer-data operations in the scoped modules, including detail-by-ID, list/filter, mutation, aggregate, report/export, audit and indirect references.
- [ ] Every customer-data query/mutation uses the authenticated Organization; request-supplied Organization IDs never grant access. Missing context is denied.
- [ ] Client and beneficial-owner list/detail/create/verify/recalculate, import-batch list/detail/process, flag and report list/detail/review/escalation/submission routes are Organization-scoped; audit-log queries are scoped to the actor's Organization. User queries filter in persistence rather than loading every tenant's users into memory.
- [ ] Insights/count aggregates and cache keys include Organization identity; no global cache entry can be reused across tenants. Import/jobs/storage paths preserve tenant context or are explicitly proven global.
- [ ] Tests create Organizations A and B with separate records and assert A cannot list, read, mutate, aggregate, export or infer B's data; include indirect relation and missing-context denial.
- [ ] `pnpm api:test` passes; the route inventory records any endpoint intentionally global and its authorization rationale.

## Verification

```bash
pnpm api:test
```

During implementation, add targeted JUnit tests in the affected modules; do not replace denial assertions with only a successful build or route-level mock.

## Operational considerations

Treat the reported findings as P0 until proven otherwise. Use compatible, append-only changes; do not alter an existing Flyway migration. Review cache eviction and jobs for stale cross-tenant values. If deployed data or logs indicate unauthorized access, follow the incident process; a code fix alone does not establish whether disclosure occurred.

## Work log

- 2026-10-03: Brief created after source review; no remediation has been implemented by this planning change.
