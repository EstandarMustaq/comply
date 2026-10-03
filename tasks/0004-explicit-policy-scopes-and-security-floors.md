---
id: 0004
title: Explicit policy scopes and security floors
status: draft
branch: task/0004-explicit-policy-scopes-and-security-floors
scope:
  - apps/api/src/main/java/com/complyz/**/policy/**
  - apps/api/src/main/java/com/complyz/tenancy/**
  - apps/api/src/main/resources/db/migration/V*__*.sql
  - apps/api/src/test/**
  - apps/ui/lib/types.ts
  - packages/api-clients/**
  - packages/ui/tests/contract/**
reserved:
  - apps/api/src/main/java/com/complyz/auth/**
  - apps/ui/app/**
  - apps/ui/components/**
  - docs/**/*.mdx
depends_on:
  - 0001
  - 0003
---

# 0004 Explicit policy scopes and security floors

## Context

The target separates personal Account preferences, explicit Organization policy and CommercialClient contract/templates/security floors. The baseline has threshold settings and system configuration but no persisted three-level policy model, provenance or effective-value semantics. Proposed floors and precedence rules require task 0001 approval.

## Objective

Implement explicit, versioned policy values and their origins for Account, Organization and CommercialClient. Template application copies values into the Organization; it never creates silent inheritance. Implement the approved closed `PolicyFloor` allow-list with monotonic stricter-only evaluation, preview and audit.

## Acceptance criteria

- [ ] Policy storage distinguishes Account preferences, Organization compliance/authentication policy, CommercialClient settings/templates and security floors; each value records scope, version, origin and actor/change time as applicable.
- [ ] Applying a template copies an immutable version into explicit Organization values and records `template:<id>@<version>` provenance; later template edits do not silently change existing Organizations.
- [ ] Template propagation is a separate authorized command with before/after preview, impact count, audit and approved notifications; no background implicit propagation exists.
- [ ] The approved closed floor-key set is validated; the proposed keys are `mfa.required`, `auth.min_acr`, `session.idle_timeout`, `session.max_duration`, `sso.allowed_idps`, `membership.external.allowed` and `membership.external.max_duration`. The Organization can strengthen but cannot weaken a floor; effective values expose `floor:commercial_client` provenance, and unknown keys fail closed.
- [ ] Changing a floor requires approved step-up, a preview of affected Organizations, audit and notification to those Organizations' `OWNER`/`ADMIN` roles. Sensitive Organization policy changes require the approved role, validation, audit and authorization-version invalidation; regulatory thresholds remain Organization-local.
- [ ] Tests cover precedence, monotonic comparison for every allowed key, rejected weakening/unknown key, copy semantics, preview/propagation and cross-Organization access denial.
- [ ] Spring/OpenAPI types, generated `packages/api-clients`, typed UI types/client and contract tests remain synchronized; `pnpm api:test` passes.

## Verification

```bash
pnpm api:test
pnpm check
```

## Operational considerations

Schema changes are append-only and must serialize with other migration work. Do not backfill global settings into all tenants until an explicit mapping and safe defaults have been reviewed. Policy change and authorization-version bump must be atomic before active sessions can rely on the new effective policy.

## Work log

- 2026-10-03: Brief created; floor allow-list and policy decisions are blocked on task 0001.
