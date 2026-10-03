---
id: 0011
title: Organization member, policy and settings UI
status: draft
branch: task/0011-organization-members-policy-and-settings-ui
scope:
  - apps/ui/app/**/organizations/**
  - apps/ui/components/organizations/**
  - apps/ui/components/settings/**
  - apps/ui/lib/api-client.ts
  - apps/ui/lib/types.ts
  - packages/ui/tests/**
  - docs/guides/organization-and-access.mdx
reserved:
  - apps/ui/app/**/account/**
  - apps/ui/app/**/commercial/**
  - apps/api/src/main/java/com/complyz/**
depends_on:
  - 0004
  - 0005
  - 0006
  - 0009
  - 0010
---

# 0011 Organization member, policy and settings UI

## Context

The current settings area exposes threshold rules and audit logs but not membership administration, effective policy provenance, authentication requirements, integrations or explicit Organization profile. Organization context and membership APIs must be available before these workflows are surfaced.

## Objective

Implement Organization-scoped admin workflows for members, role lifecycle and explicit policy/settings. Make access boundaries and policy origin legible; provide previews for template application/propagation and floor effects without implying commercial administrators can see compliance data.

## Acceptance criteria

- [ ] Member list/detail/invite/accept/change-role/suspend/remove/renewal workflows call authorized APIs and display role, state and external-access term; denied actions are not rendered as success or silently ignored.
- [ ] UI enforces, and explains server outcomes for, last-OWNER, exclusive AUDITOR_READONLY, accepted ownership transfer, distinct maker/checker and step-up requirements. Client-side rules are usability only; the API remains authoritative.
- [ ] Organization profile and settings expose effective value, origin, last editor/time and distinction between explicit Organization values, copied templates and CommercialClient security floors.
- [ ] Template application/propagation and sensitive policy changes display before/after and affected scope before confirmation; successful changes show audit outcome; canceled or failed changes do not imply success.
- [ ] No implicit inheritance is represented. Compliance thresholds/rules remain Organization-local. SSO/auth/session/integration settings, Organization notifications and active-module status are exposed only to approved roles and reflect effective policy.
- [ ] Accessible dense tables/forms provide loading, empty, error, forbidden, confirmation and success states; current Organization is visible and all actions remain context-bound.
- [ ] Update guide in direct operational `pt-MZ`; unit/E2E/accessibility tests cover role-denied, cross-Organization switch, floor/copy provenance and concurrent last-owner failure. `pnpm check` and `pnpm product-language:check` pass.

## Verification

```bash
pnpm check
pnpm product-language:check
pnpm test:unit
pnpm test:e2e
```

## Operational considerations

Ownership transfer, policy propagation and member removal are high-impact actions. Require server-confirmed completion, clear audit references, step-up UX and recoverable failure states. Never make a browser-only permission check the control plane.

## Work log

- 2026-10-03: Brief created; current Organization access guide is minimal and does not describe the target as implemented.
