---
id: 0001
title: Ratify Account, Organization and CommercialClient decisions
status: done
branch: task/0001-ratify-account-organization-decisions
scope:
  - tasks/0001-ratify-account-organization-decisions.md
  - ROADMAP.md
  - tasks/README.md
  - tasks/0003-account-commercial-client-persistence.md
  - tasks/0004-explicit-policy-scopes-and-security-floors.md
  - tasks/0005-membership-authorization-and-separation-of-duties.md
  - tasks/0006-oidc-context-tokens-and-session-revocation.md
  - tasks/0007-commercial-entitlements-and-quotas.md
  - tasks/0009-ddd-cqrs-schemas-and-outbox.md
  - tasks/0010-account-and-organization-context-ux.md
  - tasks/0011-organization-members-policy-and-settings-ui.md
  - tasks/0012-commercial-administration-ui.md
  - tasks/0013-camel-jobs-and-wso2-integrator.md
  - tasks/0014-public-docs-and-operator-guides.md
  - tasks/0015-architecture-acceptance-and-rollout-gates.md
  - docs/adr/0001-event-sourced-bounded-contexts.md
  - GLOSSARY.md
reserved: []
depends_on: []
---

# 0001 Ratify Account, Organization and CommercialClient decisions

## Outcome

**Closed and approved on 2026-10-04 by the product owner.** This decision record replaces the temporary approval attachment. D1–D7 are closed; D7's earlier “pending” note is superseded by this approval. D6's earlier “CQRS without event sourcing” choice is explicitly superseded by the architecture decision below. No dependent implementation may revive those superseded options without a new decision.

## Ratified decisions

### D1 — Organization policy and floors

- `Organization` owns explicit operational policy and the effective security/regulatory floors. Platform-maintained floors are versioned and carry source and effective dates. An Organization may keep or strengthen a security floor, never weaken it; new Organizations start with platform defaults. `CommercialClient` has no policy, policy template, or floor.
- Security floors include MFA (`auth.min_acr`), session idle timeout (30-minute platform maximum), absolute session duration (8-hour platform maximum), configured IdP allow-list, external membership disabled by default, and a 90-day maximum default term for approved external memberships.
- Regulatory floors include maximum 12-month review intervals for KYC risk, PEP, enhanced due diligence, risk assessment and training; at least 10-year retention for identity, transaction, report and finding records according to their approved retention trigger; a three-month maximum for temporary accounts; and simplified due-diligence limits of MT 30,000 monthly and MT 240,000 annually where that regime applies.
- An authorized simplified-regime exemption is explicit, scoped to eligible floors, time-bounded, referenced, audited and subject to step-up. Without a valid exemption, the floor applies. Platform floor changes are previewed, versioned, effective-dated and communicated to affected Organization owners/admins and OCOS.
- Organization policy changes require `OWNER`, recent step-up, impact preview, audit and notification. An authorization-version change takes effect on the next request, with a five-minute upper bound. Profile/security recovery remains possible through the IdP profile session.

### D2a — Organization roles and privileged actions

- Organization roles are `OWNER`, `ADMIN`, `OCOS`, `COMPLIANCE_OFFICER` and `AUDITOR_READONLY`. There are no CommercialClient membership roles.
- `OWNER` manages owners/admins, Organization authentication requirements and Organization suspension/closure. At least one `OWNER` must remain; two are recommended. `ADMIN` manages other members, operational policy and integrations, but does not gain compliance-data access solely from that role.
- `OCOS`, `COMPLIANCE_OFFICER` and `AUDITOR_READONLY` grant compliance access. `OWNER`/`ADMIN` need a separate compliance role to read compliance records. `COMPLIANCE_OFFICER` is an operational product role; statutory OCOS duties belong to `OCOS`.
- `AUDITOR_READONLY` is exclusive and cannot be combined with a write role. Ownership transfer requires explicit acceptance by the new owner, step-up by both parties, and expires after seven days. Changes to `OWNER`/`ADMIN` require step-up within five minutes, audit and notification to all owners.
- CommercialClient administration and Organization creation belong to a separate platform back-office identity/permission/audit boundary. Back-office access does not imply compliance access; no commercial customer role is defined.

### D2b — OCOS authority and confidential reporting

- Each Organization has exactly one active `OCOS` coordinator. A sole OCOS may also be the coordinator. OCOS designations and temporary substitutes retain appointment/qualification references and effective dates; an external member or internal auditor cannot be OCOS/substitute.
- Staff submit an internal suspicion report to an OCOS. The OCOS decides whether a STR is submitted and records the decision and reason; the coordinator sees and is notified of Organization STRs and may submit or escalate. CTR submission follows OCOS authority.
- STR content is restricted to OCOS and explicitly named need-to-know recipients who accept the confidentiality duty. Owners, admins and auditors receive only non-identifying aggregate status by default. Access to report content is audited. A two-person maker-checker requirement is not imposed; it must not block a one-person compliance operation.
- GIFiM credentials remain outside Complyz. Until an approved official integration exists, Complyz prepares the submission material for OCOS action. List updates notify the OCOS; decisions not to apply a restrictive measure are recorded with reasons.

### D3 — Organization entitlements and quotas

- Entitlements and quota enforcement belong to each `Organization` and are configured per Organization through its owning context. `CommercialClient` holds only back-office commercial reference metadata (identifier, name, contract reference and status); it neither groups/manages Organizations nor supplies entitlement defaults. Its fields and `organization.commercial_client_id` are not read to authorize requests or derive Organization entitlements; Organization changes are explicit commands through the owning context.
- Member seats, uncontracted modules and optional extra capacity are hard limits. Transaction-ingestion and monitored KYC-client volume are soft limits with overage handling; they do not block screening/monitoring of ingested data.
- Regulatory reports/submission, reads, exports, retention, audit and screening/monitoring of already ingested data are never blocked by quota exhaustion. Usage alerts at 80%/100% go to Organization owners/admins and the platform back office. After contract expiry, a 30-day read/export grace period applies, with no new capacity.

### D4 — Identity provider and sessions

- Use WSO2 Identity Server 7.3. Use Asgardeo when customer data-residency requirements permit; otherwise self-host WSO2 Identity Server. Preserve the SaaS-to-self-hosted exit path. The product owner is the initial operational owner until reassigned.
- IdP B2B Organizations are not the Complyz membership source of truth. Accounts live in the IdP root organization; Complyz owns Accounts' Organization memberships.
- The IdP issues the profile token; the API issues a five-minute Organization-scoped operational context token after checking membership, Organization state and effective policy. These are the only human token classes; there is no CommercialClient token or `commercial_client_id` authorization claim.
- Initial sessions use 30-minute idle and eight-hour absolute limits, adjustable only within D1 floors. Authorization version is checked on every protected request, forced logout uses a `jti` deny-list, and IdP deactivation ends sessions through back-channel logout within 60 seconds. Local password/MFA flows migrate to the IdP.

### D5 — Membership across Organizations

- An Account may hold active memberships in Organizations with different `CommercialClient` references. Membership is created by an Organization `OWNER`/`ADMIN` invitation with IdP-verified email; self-join is disabled. The reference is used only for platform back-office commercial control and never grants Organization or compliance access.
- External membership is explicitly marked at invitation, requires MFA, defaults to a 90-day term with explicit renewal, and is denied unless the Organization permits it under the platform floor. Record external access history for sign-in, context switches, data-class reads and exports, with access restricted to the member, Organization owners/admins and auditors.
- Cross-Organization consolidated compliance views are excluded from v1. Context switching is explicit and always re-authorizes the selected Organization membership.

### D6 — Context ownership, event sourcing and contracts

- The bounded contexts are Identity & Access, Organization, and Compliance. Each context is the sole authority allowed to change its own aggregates. Identity & Access owns Account/profile aggregates; Organization owns the Organization aggregate, memberships and Organization policy/entitlements; Compliance owns KYC, transaction, monitoring, flag and report aggregates. `CommercialClient` is a segregated platform-back-office namespace reference (identifier, name, contract reference and status) referenced by `organization.commercial_client_id`; it is not a parent/group/manager of Organizations, and no authorization decision reads it.
- Aggregate state is event-sourced. Contexts expose commands only for aggregates they own; other contexts integrate through published, versioned, business-named event contracts and explicit Context Map relationships, not direct writes into another context's tables or aggregates. Read models are projections and may be rebuilt from the event history.
- Event names express business facts, not technical CRUD/database actions. Define a versioned event envelope and per-context event catalog, both validated by versioned JSON Schema at producers and consumers (AJV in TypeScript and an equivalent validator in Java). Keep event-contract schema versions distinct from public HTTP API major versions and OpenAPI document `info.version`.
- The public REST API keeps major versions in the URI (`/api/v1`); additive compatible changes stay within a major version. OpenAPI represents the production host in `servers.url` and the full versioned URI in `paths`, never both. Protected resource servers require the appropriate `Authorization` header; requests with bodies require the declared `Content-Type`. A caller-supplied Organization header is never authorization evidence.
- Apache Camel handles internal asynchronous jobs. WSO2 Integrator is introduced only for an approved external mediation use case. The identity provider remains separate from integration mediation.

### D7 — Organization units and OCOS operations

- D7 is **approved**: an Organization may define representation units, with a headquarters unit by default. OCOS designations are attached to a unit; the OCOS coordinator covers the Organization. A single OCOS may fill both roles.
- Internal reports route to the unit's OCOS. The OCOS can grant time-bounded need-to-know access after confidentiality acceptance. The GIFiM registration reference belongs to the designation; its password stays outside the product. List updates notify the OCOS and decisions not to apply restrictive measures are justified and recorded.

## Acceptance criteria

- [x] All decisions D1–D7 are approved and have a single consistent outcome.
- [x] Membership across Organizations with different CommercialClient references is allowed; that reference itself grants no access.
- [x] The obsolete D6 “no event sourcing”, CommercialClient memberships/roles, CommercialClient floors/policy templates, CommercialClient context token and mandatory maker-checker proposals are removed from the implementation plan.
- [x] The roadmap and dependent briefs reflect the approved decisions and preserve Organization tenant isolation.
- [x] A durable ADR and domain glossary capture the architecture and canonical language; the temporary approval artifact is removed.
- [x] `pnpm exec prettier --check ROADMAP.md tasks/README.md tasks/0001-ratify-account-organization-decisions.md tasks/0003-account-commercial-client-persistence.md tasks/0004-explicit-policy-scopes-and-security-floors.md tasks/0005-membership-authorization-and-separation-of-duties.md tasks/0006-oidc-context-tokens-and-session-revocation.md tasks/0007-commercial-entitlements-and-quotas.md tasks/0009-ddd-cqrs-schemas-and-outbox.md tasks/0010-account-and-organization-context-ux.md tasks/0011-organization-members-policy-and-settings-ui.md tasks/0012-commercial-administration-ui.md tasks/0013-camel-jobs-and-wso2-integrator.md tasks/0014-public-docs-and-operator-guides.md tasks/0015-architecture-acceptance-and-rollout-gates.md docs/adr/0001-event-sourced-bounded-contexts.md GLOSSARY.md` passes.

## Operational considerations

This task closes decisions and updates planning artifacts only. It does not implement event stores, event consumers, APIs, policies, identity or UI behavior. Event-sourced implementation must address optimistic concurrency, replay safety, schema evolution, PII minimization, retention, projection rebuilds, idempotency and forward recovery before production rollout.

## Work log

- 2026-10-04: Owner approval closed D1–D7. D7's conflicting pending note was resolved as approved; the D6 no-event-sourcing proposal was superseded by the accepted event-sourced context architecture. Roadmap and dependent task briefs were aligned; the approval-only root artifact was removed.
- 2026-10-04: Corrected `CommercialClient` to a segregated back-office namespace reference, not an Organization grouping or management entity; removed CommercialClient-derived plan defaults and replaced the former “Corporação” label with canonical `Organization` / pt-MZ “Organização”.
