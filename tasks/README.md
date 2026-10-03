# Engineering task briefs

Task briefs capture bounded implementation work that needs explicit ownership, acceptance criteria, or coordination. The Account / Organization / CommercialClient backlog below is planning work only; `draft` and `blocked` do not mean implemented.

## Workflow

1. Assign the next unused numeric ID and create `NNNN-short-slug.md` from `_template.md`.
2. List paths the task may change and paths reserved for other concurrent tasks. In-progress scopes must not overlap.
3. Define acceptance criteria that can be verified with repository commands and record required checks.
4. Use a separate branch/worktree for parallel tasks. Do not stage, commit, or revert changes owned by another contributor.
5. Update the task status and work log as the task progresses.
6. Remove completed task briefs only when their history is no longer useful and the owner agrees.

## Status values

`draft`, `ready`, `in-progress`, `review`, `done`, `blocked`.

## Account / Organization / CommercialClient roadmap

Authoritative roadmap and full requirement-to-task traceability: [`../ROADMAP.md`](../ROADMAP.md). All briefs below were created 2026-10-03 from the reviewed target specification. They describe work not yet implemented. Task 0001 is blocked on explicit product/security/architecture approval; implementation tasks remain drafts until that gate is cleared. Execute serially where migration versions, API contract files, shared auth/UI files, or test suites overlap.

| ID   | Task                                                                                                           | Status  | Depends on                   |
| ---- | -------------------------------------------------------------------------------------------------------------- | ------- | ---------------------------- |
| 0001 | [Ratify Account, Organization and CommercialClient decisions](0001-ratify-account-organization-decisions.md)   | blocked | —                            |
| 0002 | [Contain cross-Organization access](0002-contain-cross-organization-access.md)                                 | draft   | —                            |
| 0003 | [Account and CommercialClient persistence/backfill](0003-account-commercial-client-persistence.md)             | draft   | 0001, 0002                   |
| 0004 | [Explicit policy scopes and security floors](0004-explicit-policy-scopes-and-security-floors.md)               | draft   | 0001, 0003                   |
| 0005 | [Membership authorization and separation of duties](0005-membership-authorization-and-separation-of-duties.md) | draft   | 0001, 0003, 0004             |
| 0006 | [OIDC, context tokens and session revocation](0006-oidc-context-tokens-and-session-revocation.md)              | draft   | 0001, 0003, 0004, 0005       |
| 0007 | [Commercial entitlements and quotas](0007-commercial-entitlements-and-quotas.md)                               | draft   | 0001, 0003, 0006             |
| 0008 | [KYC client types and INDIVIDUAL migration](0008-kyc-client-types-and-individual-migration.md)                 | draft   | 0001, 0003, 0007             |
| 0009 | [DDD, CQRS, schemas and outbox](0009-ddd-cqrs-schemas-and-outbox.md)                                           | draft   | 0001, 0003, 0005, 0007, 0008 |
| 0010 | [Account and Organization context UX](0010-account-and-organization-context-ux.md)                             | draft   | 0005, 0006, 0009             |
| 0011 | [Organization members, policy and settings UI](0011-organization-members-policy-and-settings-ui.md)            | draft   | 0004, 0005, 0006, 0009, 0010 |
| 0012 | [Commercial administration UI](0012-commercial-administration-ui.md)                                           | draft   | 0006, 0007, 0009, 0010, 0011 |
| 0013 | [Camel jobs and WSO2 Integrator](0013-camel-jobs-and-wso2-integrator.md)                                       | draft   | 0001, 0002, 0006, 0007, 0009 |
| 0014 | [Public docs and operator guides](0014-public-docs-and-operator-guides.md)                                     | draft   | 0001–0013                    |
| 0015 | [Architecture acceptance and rollout gates](0015-architecture-acceptance-and-rollout-gates.md)                 | draft   | 0002–0014                    |

`0002` is an immediate containment task and may proceed while 0001 decisions are being approved. The dependency graph, scope rationale, baseline findings, release gates and complete requirement-to-task coverage matrix are in `ROADMAP.md`.
