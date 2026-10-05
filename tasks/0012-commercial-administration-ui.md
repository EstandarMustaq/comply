---
id: 0012
title: Commercial administration UI
status: draft
branch: task/0012-commercial-administration-ui
scope:
  - apps/ui/app/**/commercial/**
  - apps/ui/components/commercial/**
  - apps/ui/lib/api-client.ts
  - apps/ui/lib/types.ts
  - packages/ui/tests/**
reserved:
  - apps/ui/app/**/account/**
  - apps/ui/app/**/organizations/**
  - apps/api/src/main/java/com/complyz/**/reporting/**
  - docs/**/*.mdx
depends_on:
  - 0006
  - 0007
  - 0009
  - 0010
  - 0011
---

# 0012 Commercial administration UI

## Context

CommercialClient is not represented in the baseline. The approved model makes it a segregated platform back-office namespace reference with identifier, name, contract reference and status. It does not group or manage Organizations and has no memberships, customer roles, policy, floors, entitlement defaults or compliance access. Contract/entitlement and platform back-office APIs must be ready first.

## Objective

Build a platform back-office experience for authorized platform operators to manage CommercialClient reference metadata and, in a separate Organization workflow, create or administer Organization records. Show the commercial reference and per-Organization entitlement/consumption without exposing compliance records or presenting Organizations as a CommercialClient-managed group. Organization policy and entitlements are changed only through their owning Organization context; this interface cannot directly mutate those aggregates.

## Acceptance criteria

- [ ] Platform back-office identity and permissions are explicit, separately audited and resolved server-side; no customer `COMMERCIAL_OWNER` or `COMMERCIAL_ADMIN` role exists. CommercialClient association grants no Organization or compliance access.
- [ ] CommercialClient screens expose only identifier, name, contract reference and status to authorized platform operators. Organization records and their `commercial_client_id` references are handled in a separate Organization workflow; there are no grouping, bulk-management or hierarchy operations. Changing CommercialClient metadata or status never cascades to Organization access, lifecycle, policy, floors or entitlements; those changes use their owning context APIs.
- [ ] Organization entitlements are shown per Organization with source, status, term and 80%/100% warnings; they are not presented as inherited from CommercialClient. Contract-reference actions cannot exceed server-side permissions, and UI never implies regulatory operations are disabled by quota exhaustion.
- [ ] Organization creation follows the approved lifecycle: platform back office creates the tenant and invites its first OWNER; the operator receives no compliance membership or access.
- [ ] Every platform action produces an auditable outcome. Tests prove platform/compliance separation, deny direct aggregate writes from the back office, and prove Organization authorization and lifecycle are independent of CommercialClient reference values.
- [ ] The accessible operational layout includes loading, empty, error, denied, limit-reached and success states, with clear, direct English copy. Tests prove commercial/operational separation and multi-Organization allocation; `pnpm check` and `pnpm product-language:check` pass.

## Verification

```bash
pnpm check
pnpm product-language:check
pnpm test:unit
pnpm test:e2e
```

## Operational considerations

Contract and quota display must match Organization-owned server accounting and approved contract data; never treat a client-side counter as enforcement. Platform operators must not see sensitive Organization membership or compliance data unless a separately approved, least-privilege support permission explicitly permits the specific operation.

## Work log

- 2026-10-03: Brief created; commercial workflows and permissions are target work, not present in the reviewed shell.
- 2026-10-04: Aligned with ratified task 0001: CommercialClient is a back-office reference only; no membership, role, hierarchy, policy floor or direct Organization aggregate write.
