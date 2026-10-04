---
id: 0002
title: Contain cross-Organization access
status: done
branch: task/0002-contain-cross-organization-access
scope:
  - apps/api/src/main/java/com/complyz/audit/**
  - apps/api/src/main/java/com/complyz/config/ApiVersioningConfiguration.java
  - apps/api/src/main/java/com/complyz/clients/**
  - apps/api/src/main/java/com/complyz/flags/**
  - apps/api/src/main/java/com/complyz/ingest/**
  - apps/api/src/main/java/com/complyz/insights/**
  - apps/api/src/main/java/com/complyz/monitoring/**
  - apps/api/src/main/java/com/complyz/reporting/**
  - apps/api/src/main/java/com/complyz/sanctions/**
  - apps/api/src/main/java/com/complyz/settings/controller/SettingsController.java
  - apps/api/src/main/java/com/complyz/settings/entity/**
  - apps/api/src/main/java/com/complyz/users/**
  - apps/api/src/test/java/com/complyz/**
  - packages/api-clients/**
  - packages/ui/tests/contract/**
  - apps/api/build.gradle.kts
  - docs/architecture/organization-data-access-inventory.md
  - ROADMAP.md
  - tasks/0002-contain-cross-organization-access.md
  - tasks/0004-explicit-policy-scopes-and-security-floors.md
  - tasks/0005-membership-authorization-and-separation-of-duties.md
  - tasks/0011-organization-members-policy-and-settings-ui.md
  - tasks/README.md
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

- [x] A route-to-data-access inventory covers all customer-data operations in the scoped modules, including detail-by-ID, list/filter, mutation, aggregate, report/export, audit and indirect references.
- [x] Every customer-data query/mutation uses the authenticated Organization; request-supplied Organization IDs never grant access. Missing context is denied.
- [x] Client and beneficial-owner list/detail/create/verify/recalculate, import-batch list/detail/process, flag and report list/detail/review/escalation/submission routes are Organization-scoped; audit-log queries are scoped to the actor's Organization. User queries filter in persistence rather than loading every tenant's users into memory.
- [x] Insights/count aggregates and cache keys include Organization identity; no global cache entry can be reused across tenants. Import/jobs/storage paths preserve tenant context or are explicitly proven global.
- [x] Tests create Organizations A and B with separate records and assert A cannot list, read, mutate, aggregate, export or infer B's data; include indirect relation and missing-context denial. Rows with inconsistent cross-Organization uploader/reviewer relations are not serialized to the caller.
- [x] Controllers delegate reads through services; no controller in the audited paths accesses a repository directly.
- [x] User creation does not reveal whether an email exists across Organizations; the legacy direct-create endpoint returns a uniform unavailable response until an invitation/membership flow is implemented.
- [x] Global threshold/system-config writes are denied to every current Organization role until separate platform authority and audit are implemented.
- [x] `pnpm api:test` passes; the route inventory records any endpoint intentionally global and its authorization rationale.

## Verification

```bash
pnpm api:test
```

During implementation, add targeted JUnit tests in the affected modules; do not replace denial assertions with only a successful build or route-level mock.

## Operational considerations

Treat the reported findings as P0 until proven otherwise. Use compatible, append-only changes; do not alter an existing Flyway migration. Review cache eviction and jobs for stale cross-tenant values. If deployed data or logs indicate unauthorized access, follow the incident process; a code fix alone does not establish whether disclosure occurred.

Threshold/system configuration remains global reference state, but writes fail closed for all current Organization roles until a separate platform authority and audit boundary is implemented. Direct user creation likewise stays unavailable until the approved Organization-membership invitation flow replaces it. The route inventory records these controls and their boundaries. This task's `done` status means the known containment findings and targeted verification closed; it is not a release security certification, and task 0015 must still re-audit jobs, storage, cache, indirect relations and every customer-data path.

## Work log

- 2026-10-03: Brief created after source review; no remediation had been implemented by that planning change.
- 2026-10-03: Began task 0002 on `task/0002-contain-cross-organization-access` from local `main`. Scoped flag list/detail/review/escalation, client and beneficial-owner KYC operations, report list/detail/update/submission/export, audit-log reads, import-batch list/detail/process, insights aggregates/cache keys, user listing/reset, and customer-linked transaction sums/duplicate checks. Added focused service/controller tests and `docs/architecture/organization-data-access-inventory.md`.
- 2026-10-03: Focused unit/controller tests passed for flag, insight, KYC, due-diligence, PEP batch, structuring, report export/controller, imports, audit, and user paths.
- 2026-10-03: Added `OrganizationScopedRepositoryIntegrationTest`, using Testcontainers PostgreSQL 16 and the real Flyway migrations. The A/B persistence tests cover reads, writes denied through scoped services/controllers, aggregates, annual export content, and indirect/inconsistent flag ownership. A red persistence assertion exposed cross-Organization influence in the flag duplicate counters; both counters now exclude records with a related client/transaction Organization mismatch.
- 2026-10-03: `pnpm api:test` passes with the PostgreSQL integration test. A serialization regression assertion showed shared `SystemConfig` responses could expose an updater's cross-Organization user metadata; `SystemConfig.updatedBy` is now excluded from JSON and the integration test verifies that no user or Organization metadata is serialized.
- 2026-10-03: Re-ran `pnpm api:test` after the serialization fix; it passes. The first OpenAPI generation attempt was blocked by unresolved Springdoc component references, so generated client artifacts were not changed at that point.
- 2026-10-04: Versioned the public API routes under `/api/v1`, renamed the response metadata type to `ApiResponseMetadata`, and replaced entity serialization for system configuration with `SystemConfigResponse` so `updatedBy` is not part of the JSON or OpenAPI contract. Regenerated OpenAPI from the running candidate: all 40 operations across 35 paths are versioned and every local `$ref` resolves.
- 2026-10-04: Closed review findings: routed audit/import reads through services; excluded batches/flags/reports with cross-Organization user associations while retaining batches without an uploader; disabled direct user creation with a uniform response to remove global email enumeration; denied Organization-role writes to global settings; and added a method-security test proving both `ADMIN` and `OCOS` are denied. PostgreSQL A/B tests cover foreign uploader/reviewer records. `pnpm api:test` passes; status is `done`, subject to the separate full-product release audit in task 0015.
- 2026-10-04: Restricted API route prefixing to `com.complyz` controllers so Springdoc remains at `/v3/api-docs`; documented the direct-user-creation `501` response and regenerated the OpenAPI client from Springdoc (40 operations across 35 versioned resource paths).
- 2026-10-04: Final verification after the API/config changes: `pnpm api:test`, `pnpm --filter @complyz/api-clients run typecheck`, `pnpm product-language:check`, focused Prettier checks and `git diff --check` pass. `pnpm test:contract` completes with its sole OpenAPI-backed test skipped because `COMPLYZ_OPENAPI_URL` is unset. `pnpm check`'s UI lint/typecheck passed, but its repository-wide Prettier phase reports pre-existing formatting issues under `.worktrees/task-0002-client-ingest`; those unrelated worktree files were left untouched.
