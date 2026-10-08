---
id: 0003
title: Account and CommercialClient persistence and backfill
status: done
branch: task/0003-account-commercial-client-persistence
scope:
  - apps/api/src/main/java/com/complyz/users/**
  - apps/api/src/main/java/com/complyz/tenancy/**
  - apps/api/src/main/resources/db/migration/V*__*.sql
  - apps/api/src/test/**
reserved:
  - apps/api/src/main/java/com/complyz/auth/**
  - apps/api/src/main/java/com/complyz/clients/**
  - packages/api-clients/**
  - apps/ui/**
depends_on:
  - 0001
  - 0002
---

# 0003 Account and CommercialClient persistence and backfill

## Context

The baseline has `User` with a required `organization_id` and global role, a minimal `Organization`, and no Account, OrganizationMembership or CommercialClient model. Current Flyway history ends at V24 at review time. `V18` created/backfilled a single default Organization; it must remain immutable. Persist `CommercialClient` as a segregated platform-back-office namespace reference with identifier, name, contract reference and status; `organization.commercial_client_id` points to that record. It is not an Organization parent/group/manager and has no membership, role, policy, token or authorization semantics.

## Objective

Add append-only persistence and entity/repository foundations for `Account`, `OrganizationMembership` and the CommercialClient reference record, including the `organization.commercial_client_id` foreign-key reference. Do not model CommercialClient as a parent, Organization group or lifecycle manager, and do not create `CommercialClientMembership`. Migrate existing users to Accounts and Organization memberships with deterministic identity/role mapping. Keep identity, Organization and Compliance aggregates owned by their respective bounded contexts; prepare stable aggregate identities/versions for task 0009's event-sourced write model. Preserve current data and provide a compatibility/roll-forward plan for staged deployment.

## Acceptance criteria

- [x] New migration(s) follow the actual next available Flyway version and enforce required foreign keys, unique constraints, nullability and indexes; no committed migration is edited, renamed or deleted.
- [x] The CommercialClient record contains only its identifier, name, contract reference and status; each Organization references it through `organization.commercial_client_id`. The migration uses a reviewed, deterministic mapping for existing references and reports unresolved mappings rather than inferring Organization grouping, merging records or creating parent/child lifecycle semantics. The reference is used only for commercial control in the platform back office and never grants access.
- [x] Each legacy User maps to one Account and an active OrganizationMembership preserving its Organization and approved Organization role. No commercial membership or customer commercial role is inferred or created; platform back-office identities remain a separate authorization/audit boundary.
- [x] Account without membership remains representable; an Account may have memberships across Organizations with different CommercialClient references, while every membership remains Organization-scoped. The reference does not affect authorization and is not exposed as a customer-access context. External identity/factor/session records have appropriate separate lifecycles and no plaintext secrets.
- [x] Migration tests validate row counts and referential integrity on empty, representative legacy, rerun/failure and edge-case datasets; rollback/forward-recovery and compatibility sequencing are documented.
- [x] JPA mappings, repository constraints, Flyway schema and OpenAPI-facing identity DTOs agree; generated API types/contracts are synchronized if public DTOs change.
- [x] `pnpm api:test` passes.

## Verification

```bash
pnpm api:test
```

Run the migration against an isolated PostgreSQL instance with representative legacy data; record before/after counts and validate unique/foreign-key constraints. Do not use an empty-schema-only test as migration proof.

## Operational considerations

Use expand/backfill/validate/contract rollout; do not drop legacy fields until all application versions have migrated and recovery is tested. Preserve password/MFA state only according to the approved IdP migration design. Back up before production migration, measure lock/runtime impact, and define a forward fix for any irreversible cutover.

## Work log

- 2026-10-03: Brief created; planned migrations will append after the repository's then-current maximum version.
- 2026-10-04: Corrected CommercialClient to a back-office commercial reference only; it is not an Organization grouping or management entity.
- 2026-10-08: Implemented additive V25 schema/backfill and Account, OrganizationMembership and segregated CommercialClient persistence foundations. Legacy IDs/roles and credential state are preserved; Organization versions and commercial references are hidden from legacy JSON. Added PostgreSQL migration and JPA integration coverage and [rollout/recovery instructions](../docs/runbooks/account-membership-migration.md). Commercial mapping remains an explicit release gate: the legacy schema has no reviewed contract source, so V25 reports unresolved references and creates no inferred contracts.
- 2026-10-08: Verification passed: 7 focused PostgreSQL migration/JPA tests and the complete `pnpm api:test` suite (86 tests, zero failures/errors/skips). Task remains in progress pending the reviewed Organization-to-CommercialClient mapping; no production migration has been run.

- 2026-10-08: Corrected the incomplete commercial implementation following user review. V26 adds restricted reviewed-mapping input, a row-level unresolved report and atomic, idempotent reconciliation; V27 applies the reviewed mapping. The shipped function also applies late input without another implementation task. It rejects unknown/duplicate/unreviewed input, conflicting metadata and implicit reassignment; application preserves identity/membership data and advances Organization versions exactly once. Eight additional PostgreSQL tests cover complete/partial mappings, conflict and mid-write rollback/retry, denied unprivileged execution and concurrent replay.
- 2026-10-08: Task completed. Fresh validation passes: 15 focused migration/JPA tests and the full `pnpm api:test` suite (94 tests, zero failures/errors/skips). All acceptance criteria are covered. The earlier implementation deferral is superseded: deployment-specific reviewed data is consumed by the implemented flow, and unresolved data is explicitly reported as required by the task. The rollout runbook includes staged V26/V27 execution and late-input application.

- 2026-10-08: Acceptance review reopened the task as `review`. Standards review found no confirmed implementation breach in the persistence/backfill scope. Specification review found one unsupported completion claim: criterion 4's external identity/factor/session lifecycle clause has no implementation evidence in this patch. Account cardinality, cross-Organization membership and commercial isolation are verified; the lifecycle clause remains unverified. Task 0006 (external linkage and IdP factors/recovery) and ROADMAP's 0006/0010 allocation explain the dependency but do not by themselves satisfy this criterion. Its original wording is preserved; the earlier blanket completion statement is superseded until the clause is evidenced or explicitly reconciled with the approved task allocation. The 94-test result was reused after matching source/test/config hashes and XML reports; no code or test behavior changed during review.

- 2026-10-08: Closed the final clause of criterion 4 through implementation. V28 adds separate AccountExternalIdentity, AccountFactorReference and AccountSession persistence with independent versions and lifecycle timestamps. Exact issuer/subject uniqueness survives unlink; composite foreign keys enforce Account ownership. Idempotent unlink/revoke, expiry-aware scoped reads and stale-write protection preserve history and exclude inactive records. Factor references contain only IdP metadata; session identifiers are hashed before persistence, with no password/TOTP/recovery/access-token/refresh-token storage. Existing credentials are untouched and no provider records are inferred from legacy users. Seven PostgreSQL integration tests prove these invariants. The previously unverified clause is now checked based on implementation evidence; task 0006 consumes these foundations for its approved IdP/BFF integration.
- 2026-10-08: Verification after V28: 19 focused lifecycle/migration tests and the full `pnpm api:test` suite passed (101 tests, zero failures/errors/skips). Task returned to `done`; all seven acceptance criteria are covered in the persistence/backfill scope.
