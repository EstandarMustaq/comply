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

The current settings area exposes threshold rules and audit logs but not membership administration, effective policy provenance, authentication requirements, integrations or explicit Organization profile. Organization context and membership APIs must be available before these workflows are surfaced. Ratified policy is Organization-owned with platform-maintained floors; CommercialClient has no policy, template, floor or customer role.

## Objective

Implement Organization-scoped workflows for members, role lifecycle and explicit policy/settings. Make access boundaries and policy origin legible; provide previews for platform floor changes and eligible regulatory exemptions without implying CommercialClient provides policy or access to compliance data.

## Acceptance criteria

- [ ] Member list/detail/invite/accept/change-role/suspend/remove/renewal workflows call authorized APIs and display role, state and external-access term; denied actions are not rendered as success or silently ignored. Do not expose the legacy direct-user-creation route; use the verified Organization membership invitation contract from task 0005.
- [ ] UI enforces, and explains server outcomes for, last-OWNER, exclusive AUDITOR_READONLY, accepted ownership transfer, OCOS authority and step-up requirements. Do not require a second-person STR/CTR maker-checker. Client-side rules are usability only; the API remains authoritative.
- [ ] Organization profile and settings expose effective value, origin, last editor/time and distinction between explicit Organization values, platform defaults and platform-maintained security/regulatory floors. No CommercialClient policy or floor is displayed as an authority.
- [ ] Sensitive Organization policy changes and platform floor effects display before/after values and affected scope before confirmation; eligible exemption issuance/expiry is explicit. Successful changes show the audit outcome; canceled or failed changes do not imply success. Do not implement CommercialClient policy-template propagation.
- [ ] No implicit inheritance is represented. Compliance thresholds/rules remain Organization-local and cannot weaken platform regulatory floors. SSO/auth/session/integration settings, Organization notifications and active-module status are exposed only to approved Organization roles and reflect effective policy.
      Keyboard/screen-reader accessible dense tables/forms provide loading, empty, error, forbidden, confirmation and success states; current Organization is visible and all actions remain context-bound. External-member history and unit-scoped OCOS/need-to-know controls follow the approved visibility rules.
- [ ] Update guide in direct operational `pt-MZ`; unit/E2E/accessibility tests cover role-denied, cross-Organization switch, floor/copy provenance and concurrent last-owner failure. `pnpm check` and `pnpm product-language:check` pass.

## Verification

```bash
pnpm check
pnpm product-language:check
pnpm test:unit
pnpm test:e2e
```

## Operational considerations

Ownership transfer, floor changes, exemption management and member removal are high-impact actions. Require server-confirmed completion, clear audit references, step-up UX and recoverable failure states. Never make a browser-only permission check the control plane; only the owning Organization context can change its aggregate. Global settings are not editable from Organization administration; task 0002 deliberately denies their legacy writes pending a separate platform authority.

## Work log

- 2026-10-03: Brief created; current Organization access guide is minimal and does not describe the target as implemented.
- 2026-10-04: Aligned with ratified task 0001: platform floors and defaults, no CommercialClient policy/templates, and OCOS-led report authority.
