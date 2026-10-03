---
id: 0010
title: Account and Organization context UX
status: draft
branch: task/0010-account-and-organization-context-ux
scope:
  - apps/ui/app/**/account/**
  - apps/ui/app/api/auth/**
  - apps/ui/components/account/**
  - apps/ui/components/shell/**
  - apps/ui/components/organization-selector.tsx
  - apps/ui/lib/auth/**
  - apps/ui/lib/api-client.ts
  - apps/ui/lib/types.ts
  - packages/ui/tests/**
reserved:
  - apps/ui/app/**/organizations/**
  - apps/ui/app/**/commercial/**
  - docs/**/*.mdx
depends_on:
  - 0005
  - 0006
  - 0009
---

# 0010 Account and Organization context UX

## Context

The shell currently passes a hard-coded default Organization to `OrganizationSelector`; the browser stores bearer tokens in `localStorage`. There is no Account profile/security page or real multi-Organization selection flow. The BFF/context API must exist before the selector can safely switch context.

## Objective

Deliver a production-backed Account profile/security experience and explicit Organization context switch. Use server-held IdP tokens and the BFF session, request a new Organization-scoped context token after selection, and clear all tenant-derived client state before showing the new context.

## Acceptance criteria

- [ ] Organization options come only from the authenticated Account's active memberships and display understandable role/status; the hard-coded default is removed. Use the approved `pt-MZ` business label “Corporação” for Organization where appropriate while keeping technical identifiers separate. Selection alone never grants access.
- [ ] Switching Organization obtains a newly authorized context token, invalidates/clears tenant-specific query caches, persisted client state, selection-dependent form data and pending actions; failure returns to a safe prior or no-context state without showing stale tenant data.
- [ ] No IdP access/refresh token or long-lived operational token is exposed to or persisted by browser JavaScript; browser auth uses the approved HttpOnly BFF session and CSRF controls.
- [ ] Account screens cover personal profile, language, time zone, notification preferences, factors and recovery status, linked external identities, active sessions and self-service sign-out/revocation; sensitive changes require the approved step-up and are auditable.
- [ ] Account without Organization membership can access self-profile/security only, with an honest empty state for no memberships; it cannot access compliance routes.
- [ ] Keyboard/screen-reader accessible selection and forms include loading, empty, error, denied and success states. Tests cover two Organizations, failed switch, cache clearing, session expiry and account with zero memberships.
- [ ] Product-facing copy is direct `pt-MZ`; Playwright/mobile and accessibility checks pass; `pnpm check` and `pnpm product-language:check` pass.

## Verification

```bash
pnpm check
pnpm product-language:check
pnpm test:unit
pnpm test:e2e
```

## Operational considerations

A context switch is a security boundary, not only a visual selector update. Avoid logging token/context data, and ensure browser back/forward navigation cannot reveal cached data from a previous Organization. BFF cookie/session rotation and CSRF settings must match task 0006.

## Work log

- 2026-10-03: Brief created; current selector is explicitly known to be a default placeholder, not a working membership selector.
