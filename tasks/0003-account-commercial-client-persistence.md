---
id: 0003
title: Account and CommercialClient persistence and backfill
status: draft
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

- [ ] New migration(s) follow the actual next available Flyway version and enforce required foreign keys, unique constraints, nullability and indexes; no committed migration is edited, renamed or deleted.
- [ ] The CommercialClient record contains only its identifier, name, contract reference and status; each Organization references it through `organization.commercial_client_id`. The migration uses a reviewed, deterministic mapping for existing references and reports unresolved mappings rather than inferring Organization grouping, merging records or creating parent/child lifecycle semantics. The reference is used only for commercial control in the platform back office and never grants access.
- [ ] Each legacy User maps to one Account and an active OrganizationMembership preserving its Organization and approved Organization role. No commercial membership or customer commercial role is inferred or created; platform back-office identities remain a separate authorization/audit boundary.
- [ ] Account without membership remains representable; an Account may have memberships across Organizations with different CommercialClient references, while every membership remains Organization-scoped. The reference does not affect authorization and is not exposed as a customer-access context. External identity/factor/session records have appropriate separate lifecycles and no plaintext secrets.
- [ ] Migration tests validate row counts and referential integrity on empty, representative legacy, rerun/failure and edge-case datasets; rollback/forward-recovery and compatibility sequencing are documented.
- [ ] JPA mappings, repository constraints, Flyway schema and OpenAPI-facing identity DTOs agree; generated API types/contracts are synchronized if public DTOs change.
- [ ] `pnpm api:test` passes.

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
