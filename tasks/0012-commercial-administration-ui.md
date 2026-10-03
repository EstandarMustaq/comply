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

CommercialClient is not represented in the baseline. The target reserves contract, modules, quotas, billing contacts, Organization creation, templates and security floors for commercial administration, with no inherent access to compliance data. Entitlement and authorization APIs must be ready first.

## Objective

Build a separate CommercialClient administration experience for approved commercial roles. Show contract scope and consumption, manage Organizations and allocations, and make security-floor/template actions explicit, previewed and auditable without exposing Organization compliance records.

## Acceptance criteria

- [ ] Commercial context is explicit and resolved server-side; screen data is limited to the active CommercialClient and permitted administration actions.
- [ ] Approved `COMMERCIAL_OWNER` and `COMMERCIAL_ADMIN` capabilities are distinct; a commercial role cannot list/query/export compliance records, including for Organizations under the same contract.
- [ ] Contract/modules/quotas/term, billing contacts and Organization allocations are displayed with source, status and 80%/100% warnings; approved commercial roles can manage only permitted billing metadata, actions cannot exceed server-side permissions, and UI never implies regulatory operations are disabled on quota exhaustion.
- [ ] Organization creation follows approved lifecycle: CommercialClient action creates the tenant and invites its first OWNER; creator receives no compliance membership unless separately invited/authorized.
- [ ] Policy-template version/copy and PolicyFloor change workflows show the impact preview and require appropriate authorization/step-up; changes produce confirmed audit status.
- [ ] Accessible operational layout includes loading, empty, error, denied, limit-reached and success states, and direct `pt-MZ` wording. Tests prove commercial/operational separation and multi-Organization allocation; `pnpm check` and `pnpm product-language:check` pass.

## Verification

```bash
pnpm check
pnpm product-language:check
pnpm test:unit
pnpm test:e2e
```

## Operational considerations

Contract and quota display must match server accounting and approved contract data; never treat a client-side counter as enforcement. Avoid exposing sensitive Organization membership or compliance metadata to commercial support roles unless an approved scope grants it.

## Work log

- 2026-10-03: Brief created; commercial workflows and permissions are target work, not present in the reviewed shell.
