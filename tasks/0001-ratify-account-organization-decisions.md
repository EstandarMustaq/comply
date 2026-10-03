---
id: 0001
title: Ratify Account, Organization and CommercialClient decisions
status: blocked
branch: task/0001-ratify-account-organization-decisions
scope:
  - docs/architecture/account-organization-commercial-client-decisions.md
reserved:
  - ROADMAP.md
  - tasks/**
  - docs/**/*.mdx
depends_on: []
---

# 0001 Ratify Account, Organization and CommercialClient decisions

## Context

The target model closes the technical identity name (`Account`) and tenant semantics, but several product/security decisions remain proposals. The domain overview treats cross-CommercialClient membership as undecided, while the detailed Account-membership decision says it is permitted. These are approval gates, not defaults an implementation task may silently choose.

## Objective

Create an approved decision record in `docs/architecture/account-organization-commercial-client-decisions.md`. Record approver/owner, date, rationale, alternatives, impact and status for each decision; resolve the conflicting statements about cross-CommercialClient membership. Keep the record explicit where a decision remains blocked. Do not implement product behavior in this task.

Decisions to ratify or revise:

1. Commercial `PolicyFloor` allow-list, monotonic order, impact preview, step-up, notification/audit, and effective-access latency.
2. Organization and CommercialClient roles, read-only exclusivity, last-`OWNER`, accepted ownership transfer and step-up rules, and the regulatory maker-checker approval matrix.
3. Contract entitlements, module/quota defaults and allocations, limited operations, regulatory exemptions and alert recipients.
4. WSO2 Identity Server 7.3 versus Asgardeo hosting, residency, session defaults and operational ownership.
5. Whether Accounts may join multiple CommercialClients. Ratify or revise the proposed external-member defaults explicitly: OWNER/ADMIN invitation, verified e-mail, no self-join, `external` marking, mandatory MFA, 90-day default term with explicit renewal, and `membership.external.allowed` disabled by default; also decide access-history and within-CommercialClient consolidation rules.
6. Which target architecture choices are accepted: DDD boundaries, CQRS-light/no event sourcing, JSON Schema/AJV, Camel internal use, and WSO2 Integrator external boundary.

## Acceptance criteria

- [ ] Decision record preserves the already-closed Account naming and enterprise tenant requirements; it does not reopen them as implementation choices.
- [ ] Every proposed choice above is marked `approved`, `revised`, or `blocked`; no unowned or implicit default is presented as approved.
- [ ] Cross-CommercialClient membership conflict is resolved explicitly, with external membership defaults and permitted consolidation scope.
- [ ] Decision record identifies any changed requirements and the affected roadmap/task briefs that must be updated before implementation.
- [ ] `pnpm exec prettier --check ROADMAP.md tasks/README.md tasks/0001-ratify-account-organization-decisions.md` passes after the decision record is incorporated.

## Verification

```bash
pnpm exec prettier --check ROADMAP.md tasks/README.md tasks/0001-ratify-account-organization-decisions.md
```

Approval itself is an organizational gate and cannot be established by a passing repository command.

## Operational considerations

Do not start dependent security, authorization, entitlement or identity implementation while a decision that changes its authorization semantics is blocked. No external IdP, billing or integration environment should be procured/configured by this task.

## Work log

- 2026-10-03: Brief created from the proposed decisions in the reviewed target specification; awaiting named approvals.
