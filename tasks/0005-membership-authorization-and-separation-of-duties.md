---
id: 0005
title: Membership authorization and separation of duties
status: draft
branch: task/0005-membership-authorization-and-separation-of-duties
scope:
  - apps/api/src/main/java/com/comply/users/**
  - apps/api/src/main/java/com/comply/tenancy/**
  - apps/api/src/main/java/com/comply/**/access/**
  - apps/api/src/main/java/com/comply/**/membership/**
  - apps/api/src/main/resources/db/migration/V*__*.sql
  - apps/api/src/test/**
  - packages/api-clients/**
  - apps/ui/lib/types.ts
  - apps/ui/lib/api-client.ts
  - packages/ui/tests/contract/**
reserved:
  - apps/api/src/main/java/com/comply/auth/**
  - apps/ui/app/**
  - apps/ui/components/**
  - docs/**/*.mdx
depends_on:
  - 0001
  - 0003
  - 0004
---

# 0005 Membership authorization and separation of duties

## Context

The approved model puts Organization role/state on OrganizationMembership, not Account. CommercialClient has no memberships or customer roles; a separate platform back-office identity/permission/audit boundary administers commercial records. The approved matrix includes exclusive read-only access, a last-OWNER invariant, accepted ownership transfer, external-member rules and OCOS-led reporting without a mandatory two-person maker-checker.

## Objective

Implement Organization membership lifecycle, server-derived Organization context authorization and Organization administration routes. Apply the approved role matrix in services, not only controllers; retain fail-closed Organization scoping in every compliance module. A selected/requested ID is context input, never authorization proof. Implement OCOS designation, coordinator and unit-scoped confidentiality rules in the Organization-owned aggregate; platform back-office access stays separate.

## Acceptance criteria

- [ ] Membership APIs support authorized invite, verify/accept, role change, suspend/remove, expiry/renewal and external-member access history per the approved policy; self-join is rejected if the approved decision keeps it disabled. The legacy `POST /api/v1/settings/users` direct account-creation route remains unavailable until this invitation flow can create/attach accounts without exposing global email existence.
- [ ] Operational access requires an active membership in the selected Organization. Authorization never reads `organization.commercial_client_id` or CommercialClient metadata/status; an Account may belong to Organizations with different CommercialClient references, but each request is authorized independently by its Organization membership. The reference is available only to platform back-office commercial control.
- [ ] Account profile routes expose only the account holder's data. Platform/back-office roles authorize platform administration through a separate identity and audit boundary; no customer-facing CommercialClient role or implicit compliance access exists.
- [ ] Cross-Organization consolidated compliance views are excluded from v1 regardless of CommercialClient reference values. Organization selection, reference equality and request-supplied IDs never grant access.
- [ ] `OWNER`, `ADMIN`, `OCOS`, `COMPLIANCE_OFFICER` and `AUDITOR_READONLY` behavior matches task 0001. Auditor is exclusive; at least one Organization OWNER remains; ownership transfer requires acceptance and step-up.
- [ ] Organization units support OCOS designations, exactly one active Organization-wide coordinator, temporary substitutes with the same qualifications, and time-bounded need-to-know access. External members and auditors cannot be OCOS/substitutes. Report content reads are audited; owners/admins/auditors see only non-identifying status by default.
- [ ] Sensitive role changes are audited and notify approved recipients. Internal suspicious-activity reports route to the unit's OCOS; OCOS decides/submits, the coordinator coordinates/submits/escalates, and CTR follows OCOS authority. Do not require a second-person maker-checker.
- [ ] All membership and administration routes validate actor/target Organization in the owning context; IDOR, cross-tenant and missing-context tests deny access.
- [ ] OpenAPI/generated client/types/contract tests are synchronized; `pnpm api:test`, `pnpm check` and relevant UI contract tests pass.

## Verification

```bash
pnpm api:test
pnpm check
pnpm test:contract
```

The contract test's remote OpenAPI assertion must be run with `COMPLY_OPENAPI_URL` set for release; local route/unit tests must cover authorization independently.

## Operational considerations

Role and membership changes affect active authorization through task 0006's version mechanism. Use optimistic concurrency/transactions for last-owner, coordinator and transfer invariants. Only the owning context may append changes to its aggregates; never combine platform commercial administration with implicit Organization or compliance access. Keep account creation and Organization membership invitation distinct, and do not restore direct password-based account creation as a substitute for verified membership acceptance.

## Work log

- 2026-10-03: Brief created from proposed membership rules.
- 2026-10-04: Aligned with ratified task 0001: no CommercialClient memberships/roles, hierarchy or authorization; cross-Organization consolidated views are excluded; OCOS owns report decisions without mandatory maker-checker.
