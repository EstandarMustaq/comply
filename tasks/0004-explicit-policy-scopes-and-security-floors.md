---
id: 0004
title: Explicit policy scopes and security floors
status: draft
branch: task/0004-explicit-policy-scopes-and-security-floors
scope:
  - apps/api/src/main/java/com/comply/**/policy/**
  - apps/api/src/main/java/com/comply/tenancy/**
  - apps/api/src/main/resources/db/migration/V*__*.sql
  - apps/api/src/test/**
  - apps/ui/lib/types.ts
  - packages/api-clients/**
  - packages/ui/tests/contract/**
reserved:
  - apps/api/src/main/java/com/comply/auth/**
  - apps/ui/app/**
  - apps/ui/components/**
  - docs/**/*.mdx
depends_on:
  - 0001
  - 0003
---

# 0004 Explicit policy scopes and security floors

## Context

The approved model separates Account profile preferences from explicit Organization policy and platform-maintained security/regulatory floors. CommercialClient is a back-office reference only and has no policy, template, floor, entitlement default or authorization role. Organization entitlements are assigned independently. The baseline has threshold settings and system configuration but no persisted policy provenance or effective-value semantics.

## Objective

Implement versioned Account preferences and explicit Organization policy values, with platform defaults and a closed `PolicyFloor` catalog. CommercialClient policy/templates are not part of the model. Organizations may strengthen but never weaken applicable platform floors; implement effective-value evaluation, approved regulatory exemptions, preview and audit.

## Acceptance criteria

- [ ] Policy storage distinguishes Account preferences, explicit Organization policy and platform-owned floor/catalog values; each value records scope, version, origin, effective time and actor/change time as applicable. CommercialClient stores no policy or floor.
- [ ] Organization defaults and regulatory floors are explicit, versioned and attributable to the platform. Do not add CommercialClient policy-template inheritance or silent propagation.
- [ ] The closed security-floor set includes `auth.min_acr`, `session.idle_timeout`, `session.max_duration`, `sso.allowed_idps`, `membership.external.allowed` and `membership.external.max_duration`; regulatory floors cover the approved review intervals, retention minima, temporary-account duration and simplified-regime limits in task 0001. Unknown keys fail closed. The Organization may only strengthen a floor; effective values expose `floor:platform` provenance.
- [ ] A valid simplified-regime exemption is explicit, keyed to eligible floors, referenced, time-bounded and auditable; without it the regulatory floor applies. Platform floor changes are versioned/effective-dated, preview affected Organizations and notify owners/admins and OCOS.
- [ ] Sensitive Organization policy changes require `OWNER`, step-up, validation, impact preview, audit, owner/admin notification and an atomic authorization-version change effective on the next request within five minutes.
- [ ] Legacy shared threshold/system-config writes remain denied to all Organization roles until a separately authenticated and audited platform authority is implemented; never expose platform-wide writes through Organization membership.
- [ ] Tests cover every floor's monotonic comparison, defaults/effective values, exemption eligibility/expiry, rejected weakening/unknown keys, audit/notification and cross-Organization access denial.
- [ ] Spring/OpenAPI types, generated `packages/api-clients`, typed UI types/client and contract tests remain synchronized; `pnpm api:test` passes.

## Verification

```bash
pnpm api:test
pnpm check
```

## Operational considerations

Schema changes are append-only and must serialize with other migration work. Do not infer Organization policy or regulatory exemptions during backfill. Policy changes and authorization-version updates must be atomic; event-sourced aggregate ownership and event publication follow task 0009. Task 0002 closes the existing shared-settings write routes for Organization roles; do not re-enable them as a temporary workaround while this task is being implemented.

## Work log

- 2026-10-03: Brief created from proposed policy rules.
- 2026-10-04: Aligned with task 0001: Organization-owned policy and platform floors; CommercialClient is a non-authorizing reference with no policy, template or entitlement defaults.
