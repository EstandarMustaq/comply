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

The baseline has `User` with a required `organization_id` and global role, a minimal `Organization`, and no Account, organization/commercial membership, or CommercialClient model. Current Flyway history ends at V24 at review time. `V18` created/backfilled a single default Organization; it must remain immutable. Target migration creates one CommercialClient per existing Organization and must not merge Organizations.

## Objective

Add append-only persistence and entity/repository foundations for `Account`, `OrganizationMembership`, `CommercialClient`, and `CommercialClientMembership`; make each Organization belong to exactly one CommercialClient. Migrate existing users to Accounts and Organization memberships with deterministic identity/role mapping. Preserve current data and provide a compatibility/roll-forward plan for staged deployment.

## Acceptance criteria

- [ ] New migration(s) follow the actual next available Flyway version and enforce required foreign keys, unique constraints, nullability and indexes; no committed migration is edited, renamed or deleted.
- [ ] Every pre-existing Organization receives exactly one deterministic CommercialClient; no two Organizations are grouped automatically. New-customer onboarding creates the CommercialClient with its first Organization, while later Organizations are attached only through authorized commercial administration.
- [ ] Each legacy User maps to one Account and an active OrganizationMembership preserving its Organization and role; commercial membership/admin assignment is deterministic, auditable and based on the decision record, not guessed.
- [ ] Account without membership remains representable; Account identity is independent of Organization and CommercialClient. External identity/factor/session records have appropriate separate lifecycles and no plaintext secrets.
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
