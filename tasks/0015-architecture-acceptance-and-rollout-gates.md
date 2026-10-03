---
id: 0015
title: Full architecture acceptance and rollout gates
status: draft
branch: task/0015-architecture-acceptance-and-rollout-gates
scope:
  - apps/api/src/test/**
  - packages/ui/tests/**
  - docs/runbooks/**
  - docs/architecture/**
  - ROADMAP.md
  - tasks/README.md
reserved:
  - apps/api/src/main/resources/db/migration/**
  - apps/ui/app/**
  - apps/ui/components/**
  - docs/**/*.mdx
depends_on:
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
  - 0014
---

# 0015 Full architecture acceptance and rollout gates

## Context

A build or a controller test does not prove that tenant isolation, revocation, migration, UI context, async processing or documentation is correct. The reviewed baseline already has cross-Organization exposure findings. The target explicitly requires two CommercialClients, multiple Organizations, simultaneous contexts, authorized-only consolidation, policy/floor/quota and maker-checker scenarios.

## Objective

Perform the final full-system architecture and coverage review. Execute an end-to-end acceptance matrix over API, persistence, UI, cache, audit, export and asynchronous paths; rehearse migration and recovery; resolve findings through their owning task before approving release.

## Acceptance criteria

- [ ] Every row in the requirement-to-task matrix in `ROADMAP.md` is linked to completed evidence, an explicitly approved deferral, or a recorded decision; no requirement is silently omitted.
- [ ] Integration scenario uses C1 with A1/A2 and C2 with B1 plus Accounts with different, external and absent memberships. Assert cross-CommercialClient denial, same-CommercialClient cross-Organization denial, explicit authorized consolidation only, and no access granted by selected IDs.
- [ ] Test list/detail/mutation/aggregate/cache/export/audit/import/job/storage/indirect-relation behavior; missing Organization, suspended/removed membership, Organization suspension, stale token version, role change, policy/floor change, logout and IdP back-channel logout.
- [ ] Test role/SoD matrix, exclusive auditor, last OWNER under concurrent changes, accepted/step-up ownership transfer, external membership expiry/MFA, maker-checker, commercial role isolation, template copy/preview/propagation and floor monotonicity.
- [ ] Test token `iss`/`aud`/`typ`/algorithm/JWKS rotation/unknown key/PKCE/step-up/refresh reuse; quota race and regulatory exceptions; outbox duplicate/retry/DLQ; KYC person migration and no auto-merge of Organizations.
- [ ] Rehearse append-only migration from a production-like copy and record row counts, consistency checks, runtime/locks, backup restore or forward-recovery, compatibility window and rollback decision. Do not edit or delete existing migrations.
- [ ] Browser flows verify Organization switching clears all tenant caches, Account with zero memberships sees only self-service, member/policy/commercial actions expose honest accessible states, and keyboard/mobile/accessibility tests pass.
- [ ] All severity-1/high-confidence isolation/authentication/data-loss findings are fixed before release; remaining findings have severity, impact, owner, mitigation and explicit risk acceptance. Docs accurately match test evidence.
- [ ] Final validation passes: `pnpm api:test`, `pnpm check`, `pnpm test:unit`, `pnpm test:contract` against the candidate API, `pnpm test:e2e`, `pnpm product-language:check`, `pnpm operations:test` and `pnpm compose:config`.

## Verification

```bash
pnpm api:test
pnpm check
pnpm test:unit
pnpm test:contract
pnpm test:e2e
pnpm product-language:check
pnpm operations:test
pnpm compose:config
```

Record environment prerequisites and skipped checks; a skipped remote OpenAPI test or unavailable browser/IdP/database integration is not a pass. Review changed-file diff and migration immutability before release.

## Operational considerations

This is a release gate, not a substitute for incident response, independent security review, penetration testing, privacy/legal approval, production IdP readiness or customer migration approvals. Do not declare production-ready if those controls/evidence are unavailable. Retain a tested restore and forward-fix plan and monitor authorization denials, revocation latency, quota counters, DLQ and tenant-context loss after rollout.

## Work log

- 2026-10-03: Brief created; complete test matrix is prospective and no target feature is asserted to pass today.
