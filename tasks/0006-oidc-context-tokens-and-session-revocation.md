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

The baseline uses local password login, symmetric HMAC JWTs, refresh tokens and application-managed TOTP; there is no OIDC, JWKS or BFF-held IdP session. Task 0001 approves WSO2 Identity Server 7.3, Asgardeo when customer data residency permits and self-hosting otherwise, plus session/revocation targets. The API's current JWT service may be adapted for Organization context tokens; it is not an OAuth/OIDC authorization server.

## Objective

Federate human authentication through the approved IdP and Next.js BFF. Keep IdP and refresh tokens server-side; issue a short-lived Organization-scoped operational context token only after validating identity, membership, Organization state, effective policy and authentication evidence. The IdP profile token and the API operational token are the only human token classes; there is no CommercialClient token. Implement revocation and step-up semantics with fail-closed behavior.

## Acceptance criteria

- [ ] BFF uses Authorization Code + PKCE S256, exact registered redirect URI matching, `state`, `nonce` and authorization-response issuer validation; implicit and password grants are disabled. Browser receives only a Secure/HttpOnly/SameSite session cookie with CSRF protection.
- [ ] IdP access/refresh tokens remain in the BFF; refresh rotation detects reuse and revokes the token family. External identity is linked to Account by unique `(issuer, subject)`. WSO2 B2B Organizations are not used for Complyz memberships; Accounts reside in the IdP root organization and Complyz remains the membership source of truth. Logout and back-channel logout are tested; IdP deactivation ends the session within the approved ≤60-second target.
- [ ] API validates IdP tokens against exact `iss`/`aud`, fixed RS256 or ES256, `typ`, time claims and trusted JWKS; rejects `none`, token-supplied `jku`/`x5u`/`jwk`, refreshes unknown `kid` at most once per minute, and supports overlapping key rotation. API publishes its own JWKS for authorized resource servers, which enforce the mandatory header contract.
- [ ] `POST /api/v1/context/token` validates the requested Organization against the profile identity's active membership and issues one five-minute Organization-scoped operational `at+jwt`. No CommercialClient token, scope or authorization claim is issued. CommercialClient identity is never trusted for access. Public REST operations retain `/api/v1`; additive changes stay compatible and breaking changes use a new major version under the API-version policy.
- [ ] Tokens carry the approved stable `sub`, `iss`, `aud`, `exp`, `iat`, `jti`, membership authorization `ver`, and `acr`/`amr`/`auth_time`; token type and audience cannot be confused between profile and operational tokens.
- [ ] Protected resource-server requests require `Authorization: Bearer` with the token class/audience appropriate to that route. Requests with a body require the declared `Content-Type` (JSON or the documented upload media type); OpenAPI marks the header/body contract and missing or invalid headers fail before domain commands. Tenant authorization comes from the validated context token, never a caller-supplied Organization header.
- [ ] Every request compares authorization version to the current Redis value and denies stale tokens after membership removal/suspension, role or policy change, Organization suspension, or platform floor change; changes increment the version atomically. Forced logout uses a bounded `jti` deny-list. Redis unavailable behavior is explicit and fails closed for protected customer operations.
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

Provision separate keys and IdP clients per environment; document JWKS cache/rotation and Redis outage recovery. Do not cut over existing local credentials until account matching, MFA migration/reset, recovery and rollback/forward plan are approved. Do not log tokens, codes, secrets or unnecessary claims. Record actual logout latency evidence. Use the approved 30-minute idle and eight-hour absolute session defaults, bounded by the effective Organization/platform floors. Ensure every authorization-changing aggregate event invalidates stale context tokens through task 0006's version mechanism.

## Work log

- 2026-10-03: Brief created from the proposed token profile.
- 2026-10-04: Aligned with ratified task 0001: profile plus Organization operational tokens only; added mandatory resource-server header enforcement and public API versioning.
