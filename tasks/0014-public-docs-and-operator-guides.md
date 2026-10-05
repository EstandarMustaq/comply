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

The current Mintlify API guide documents the public `/api/v1` routes. The Organization/access guide is minimal. Customer-facing product and public documentation use Complyz's clear, direct English voice; internal engineering docs use concise technical English. Customer documentation covers Account, Organizations, permissions, policy, SSO/MFA, compliance and API; restricted operator documentation may describe CommercialClient as a back-office commercial reference, never as an Organization hierarchy or customer access context. Documentation must not present proposed or untested functionality as available.

## Objective

Update customer-facing Mintlify documentation, API auth/version/header and integration guidance, and operator procedures to the release state proven by prior tasks. Identify implemented versus preview/deferred features and document access boundaries, recovery, quotas, event-contract compatibility and support actions honestly.

## Acceptance criteria

- [ ] Mintlify navigation covers only available customer workflows and follows the journey: Account/profile → Organizations → users/permissions → policy/settings → authentication/SSO/MFA → compliance → API/compatibility. CommercialClient reference details belong only in restricted operator documentation.
- [ ] API authentication examples exactly match the shipped OIDC/BFF/profile-and-Organization-context-token flow, `/api/v1` version policy, mandatory resource-server headers, scopes, cookie/token handling, errors, PKCE and context selection; obsolete local login/refresh instructions are removed only after the approved compatibility/migration window.
- [ ] Customer documentation identifies Organization as the client institution and tenant. It uses the canonical `Organization` term and never presents CommercialClient as a customer concept. Restricted operator documentation defines CommercialClient as the identifier/name/contract-reference/status record referenced by `organization.commercial_client_id`, with no Organization grouping/management or authorization semantics; no guide instructs users to submit tenant IDs for authorization.
- [ ] Public docs state only capabilities present and tested in the release, distinguish availability/preview/deferred items, explain compatible API and event-contract evolution/deprecation when relevant, and contain no internal implementation acronyms/citations or unapproved security promises.
- [ ] Operator guides cover migration/backfill/recovery, IdP/JWKS rotation, revocation/logout, quota reconciliation/exemptions, event-store backup/replay/schema evolution, outbox/DLQ, external client credentials and cross-tenant incident response as applicable to shipped features.
- [ ] User-facing copy uses clear, direct, confident English, with accessible terminology consistent with product-language standards. `pnpm product-language:check` and `pnpm exec prettier --check ROADMAP.md README.md docs` pass.

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
- 2026-10-04: Aligned with ratified task 0001: Organization-only operational authorization, platform back-office commercial administration, versioned API/event contracts and approved context ownership.
