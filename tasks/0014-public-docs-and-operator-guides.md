---
id: 0014
title: Public documentation and operator guides
status: draft
branch: task/0014-public-docs-and-operator-guides
scope:
  - docs/**/*.mdx
  - docs/docs.json
  - README.md
  - apps/api/src/main/resources/**
  - ROADMAP.md
reserved:
  - apps/ui/app/**
  - apps/ui/components/**
  - apps/api/src/main/java/com/complyz/**
  - packages/ui/tests/**
depends_on:
  - 0001
  - 0002
  - 0003
  - 0004
  - 0005
  - 0006
  - 0007
  - 0008
  - 0009
  - 0010
  - 0011
  - 0012
  - 0013
---

# 0014 Public documentation and operator guides

## Context

The current Mintlify API guide documents local `/api/auth/login` and `/api/auth/refresh`, and the Organization/access guide is minimal. Public text is `pt-MZ`; internal task/architecture docs are English. The target's navigation includes Account, Organizations, permissions, policy, SSO/MFA, CommercialClient, compliance and API, but documentation must not present proposed or untested functionality as available.

## Objective

Update customer-facing Mintlify documentation, API auth and integration guidance, and operator procedures to the release state proven by prior tasks. Identify implemented versus preview/deferred features and document access boundaries, recovery, quotas and support actions honestly.

## Acceptance criteria

- [ ] Mintlify navigation covers only available workflows and follows the requested journey: Account/profile → Organizations → users/permissions → policy/settings → authentication/SSO/MFA → CommercialClient → compliance → API/compatibility.
- [ ] API authentication examples exactly match the shipped OIDC/BFF/context-token flow, endpoint version, scopes, cookie/token handling, errors, PKCE and context selection; obsolete local login/refresh instructions are removed only after old clients are safely supported or migration is complete.
- [ ] Organization, CommercialClient, KYC Client and OAuth client are distinctly named; docs state that same-CommercialClient membership is not compliance permission and do not instruct users to submit tenant IDs for authorization.
- [ ] Public docs state only capabilities present and tested in the release, distinguish availability/preview/deferred items, and contain no internal implementation acronyms/citations or unapproved security promises.
- [ ] Operator guides cover migration/backfill/recovery, IdP/JWKS rotation, revocation/logout, quota reconciliation/exemptions, policy propagation, outbox/DLQ, external client credentials and cross-tenant incident response as applicable to shipped features.
- [ ] User-facing copy is direct, operational `pt-MZ`, with accessible terminology consistent with product-language standards. `pnpm product-language:check` and `pnpm exec prettier --check ROADMAP.md README.md docs` pass.

## Verification

```bash
pnpm product-language:check
pnpm exec prettier --check ROADMAP.md README.md docs
pnpm api:openapi
```

OpenAPI generation must be run against the candidate API and checked into/reconciled with generated client artifacts when the public contract changed; this documentation task does not replace contract tests.

## Operational considerations

Coordinate API deprecation/compatibility windows with customers before removing old public instructions or flows. Protect internal runbooks and operational identifiers; public Mintlify docs must not reveal secrets, sensitive endpoints or unsupported recovery promises.

## Work log

- 2026-10-03: Brief created; current documentation is not evidence that target OIDC or commercial workflows exist.
