# Event-sourced bounded contexts and business-owned contracts

**Status:** accepted (2026-10-04). Comply uses Identity & Access, Organization and Compliance bounded contexts; each context alone changes its aggregates. Aggregate event streams are the write-side source of truth and business-named, versioned event contracts integrate contexts through an explicit Context Map. A versioned JSON Schema catalog validates the shared envelope and event payloads in Java and TypeScript. This supersedes the earlier CQRS-without-event-sourcing proposal; public HTTP API versions, event-schema versions and OpenAPI document revisions remain separate version axes. `Organization` is the client institution represented in the product and the tenant. `CommercialClient` is a segregated platform-back-office namespace reference containing its identifier, name, contract reference and status, referenced by `organization.commercial_client_id`; it is not a parent/group/manager of Organizations, and neither its fields nor the Organization reference are read by authorization or used to derive Organization entitlements.

## Consequences

- Cross-context writes and shared mutable aggregate tables are prohibited; other contexts use published contracts and rebuildable projections.
- `CommercialClient` is commercial reference data, not an Organization hierarchy or grouping mechanism. It has no members, roles, tokens, policies or authorization behavior; tenant identity and access remain Organization-scoped.
- Producers and consumers must enforce event ordering/concurrency, idempotent delivery, replay safety, schema compatibility/upcasting, tenant attribution, retention and PII minimization.
- Protected resource-server endpoints require an appropriate bearer `Authorization` header; requests with bodies require their declared `Content-Type`. Tenant identity comes from validated context, never a caller-supplied tenant header.
