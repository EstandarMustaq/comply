---
id: 0006
title: OIDC, context tokens and session revocation
status: draft
branch: task/0006-oidc-context-tokens-and-session-revocation
scope:
  - apps/api/src/main/java/com/complyz/auth/**
  - apps/api/src/main/java/com/complyz/tenancy/**
  - apps/api/src/main/resources/**
  - apps/api/src/test/java/com/complyz/auth/**
  - apps/ui/app/api/auth/**
  - apps/ui/lib/auth/**
  - apps/ui/lib/api-client.ts
  - packages/api-clients/**
  - packages/ui/tests/**
reserved:
  - apps/ui/app/**/account/**
  - apps/ui/app/**/organizations/**
  - docs/**/*.mdx
depends_on:
  - 0001
  - 0003
  - 0004
  - 0005
---

# 0006 OIDC, context tokens and session revocation

## Context

The baseline uses local password login, symmetric HMAC JWTs, refresh tokens and application-managed TOTP; there is no OIDC, JWKS or BFF-held IdP session. The target selects WSO2 Identity Server 7.3, but hosting/residency and session/revocation details remain approval-gated. The API's current JWT service may be adapted for Complyz context tokens; it is not an OAuth/OIDC authorization server.

## Objective

Federate human authentication through the approved IdP and Next.js BFF. Keep IdP and refresh tokens server-side; issue distinct, short-lived operational and commercial context tokens only after validating identity, membership, Organization state, effective policy and authentication evidence. Implement revocation and step-up semantics with fail-closed behavior.

## Acceptance criteria

- [ ] BFF uses Authorization Code + PKCE S256, exact registered redirect URI matching, `state`, `nonce` and authorization-response issuer validation; implicit and password grants are disabled. Browser receives only a Secure/HttpOnly/SameSite session cookie with CSRF protection.
- [ ] IdP access/refresh tokens remain in the BFF; refresh rotation detects reuse and revokes the token family. External identity is linked to Account by unique `(issuer, subject)`. WSO2 B2B Organizations are not used for Complyz memberships; Accounts reside in the IdP root organization and Complyz remains the membership source of truth. Logout and back-channel logout are tested; IdP deactivation ends the session within the approved ≤60-second target.
- [ ] API validates IdP tokens against exact `iss`/`aud`, fixed RS256 or ES256, `typ`, time claims and trusted JWKS; rejects `none`, token-supplied `jku`/`x5u`/`jwk`, refreshes unknown `kid` at most once per minute, and supports overlapping key rotation. API publishes its own JWKS for authorized resource servers.
- [ ] `POST /v1/context/token` validates requested context server-side and issues distinct five-minute operational/commercial `at+jwt` tokens. Operational token requires `organization_id`; commercial token carries only `commercial_client_id` and commercial scopes. Commercial ID is derived, never trusted from UI input. Existing `/api/...` routes are versioned or compatibly migrated according to an approved customer/API transition plan.
- [ ] Tokens carry the approved stable `sub`, `iss`, `aud`, `exp`, `iat`, `jti`, membership authorization `ver`, and `acr`/`amr`/`auth_time`; types and audiences cannot be confused between profile, operational and commercial tokens.
- [ ] Every request compares authorization version to the current Redis value and denies stale tokens after membership removal/suspension, role or policy change, Organization suspension, or CommercialClient floor change; changes increment the version atomically. Forced logout uses a bounded `jti` deny-list. Redis unavailable behavior is explicit and fails closed for protected customer operations.
- [ ] API compares `acr`/`amr`/`auth_time` with effective Organization policy and returns the approved RFC 9470 step-up challenge; the BFF retries with required `acr_values` and `max_age`. TOTP enrollment/confirmation, attempt limits, replay/window handling, protected secret, recovery and reauthentication are owned by IdP and verified end-to-end; API does not infer MFA from login alone.
- [ ] Sensitive changes, Organization switch, membership revocation, token refresh/reuse, invalid claims, unknown key and logout are tested. OpenAPI/client/contract updates are synchronized; `pnpm api:test` and `pnpm check` pass.

## Verification

```bash
pnpm api:test
pnpm check
pnpm test:contract
```

Run IdP/BFF integration tests against a non-production WSO2/approved IdP environment; mocked JWKS tests alone do not validate IdP configuration, redirect registration, adaptive ACR or logout delivery.

## Operational considerations

Provision separate keys and IdP clients per environment; document JWKS cache/rotation and Redis outage recovery. Do not cut over existing local credentials until account matching, MFA migration/reset, recovery and rollback/forward plan are approved. Do not log tokens, codes, secrets or unnecessary claims. Record actual logout latency evidence. Session defaults such as 30-minute idle and 8-hour absolute lifetime remain subject to task 0001 approval and approved floors.

## Work log

- 2026-10-03: Brief created from the target token profile; provider and session decisions remain approval-gated.
