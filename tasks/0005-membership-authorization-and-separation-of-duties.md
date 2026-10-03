---
id: 0005
title: Membership authorization and separation of duties
status: draft
branch: task/0005-membership-authorization-and-separation-of-duties
scope:
  - apps/api/src/main/java/com/complyz/users/**
  - apps/api/src/main/java/com/complyz/tenancy/**
  - apps/api/src/main/java/com/complyz/**/access/**
  - apps/api/src/main/java/com/complyz/**/membership/**
  - apps/api/src/main/resources/db/migration/V*__*.sql
  - apps/api/src/test/**
  - packages/api-clients/**
  - apps/ui/lib/types.ts
  - apps/ui/lib/api-client.ts
  - packages/ui/tests/contract/**
reserved:
  - apps/api/src/main/java/com/complyz/auth/**
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

The target puts role/state on Organization and CommercialClient memberships, not on Account. Commercial administration must be independent of compliance access. The proposed matrix includes an exclusive auditor, a last-OWNER invariant, accepted ownership transfer, external-member rules and maker-checker reporting; task 0001 must approve the exact matrix first.

## Objective

Implement membership lifecycle, server-derived operational/commercial context authorization and API management routes. Apply the approved role matrix in services, not only controllers; retain fail-closed Organization scoping in every compliance module. A selected/requested ID is context input, never authorization proof.

## Acceptance criteria

- [ ] Membership APIs support authorized invite, verify/accept, role change, suspend/remove, expiry/renewal and external-member access history per the approved policy; self-join is rejected if the approved decision keeps it disabled.
- [ ] Operational access requires an active membership for the selected Organization; `commercial_client_id` is derived from that Organization on the server. Same CommercialClient membership alone never grants another Organization's compliance access.
- [ ] Commercial roles authorize contract/Organization administration only; compliance roles are explicit Organization memberships. Account profile routes require no tenant role and expose only the account holder's data. Platform/support access has separate identity, permission and audit controls.
- [ ] Any consolidated view is explicitly permissioned, limited to the authorized Organization memberships within one CommercialClient, and never includes all Organizations by default; cross-CommercialClient consolidation is denied.
- [ ] `OWNER`, `ADMIN`, `OCOS`, `COMPLIANCE_OFFICER`, `AUDITOR_READONLY`, `COMMERCIAL_OWNER` and `COMMERCIAL_ADMIN` behavior matches the approved matrix. Auditor is exclusive; at least one Organization OWNER remains; ownership transfer requires acceptance and step-up.
- [ ] Sensitive role changes are audited and notify the approved recipients. STR/CTR maker and checker are distinct members and satisfy the approved regulatory approver matrix; no self-approval succeeds.
- [ ] All create/list/detail/update/delete/invite endpoints validate actor and target membership in one Organization/CommercialClient; IDOR, cross-tenant and missing-context tests deny access.
- [ ] OpenAPI/generated client/types/contract tests are synchronized; `pnpm api:test`, `pnpm check` and relevant UI contract tests pass.

## Verification

```bash
pnpm api:test
pnpm check
pnpm test:contract
```

The contract test's remote OpenAPI assertion must be run with `COMPLYZ_OPENAPI_URL` set for release; local route/unit tests must cover authorization independently.

## Operational considerations

Role and membership changes affect active authorization immediately through task 0006's version mechanism; until that mechanism exists, this work must not advertise immediate revocation. Use transactions/locking for last-owner and transfer invariants to prevent concurrent demotion races. Never combine commercial admin permission with implicit data access.

## Work log

- 2026-10-03: Brief created; role details remain subject to task 0001 approval.
