# Architecture

## Product shape

Financial Compliance Intelligent is a web application composed of a Next.js design system app, a Spring Boot GRC engine, PostgreSQL, Redis, and an edge proxy for production.

## Runtime components

- **Design System:** Next.js UI, API client, role-aware workflows, and product language.
- **GRC Engine:** Spring Boot REST API, authentication, MFA, tenant access, domain workflows, screening, monitoring, reporting, and audit.
- **PostgreSQL:** durable domain state and Flyway-managed schema.
- **Redis:** token revocation, rate limits, and replay protection.
- **Edge:** Caddy in production for TLS termination and routing.

## Domain boundaries

Authentication owns sessions and MFA. Customers/KYC owns profiles and beneficial ownership. Ingestion owns file lifecycle and transaction creation. Monitoring owns rules and signals. Screening owns versioned restricted-party data. Reporting owns immutable report state and exports. Audit owns activity history. Tenancy owns organization boundaries.

## Security and tenancy

The authenticated principal is the source of organization scope. Request payloads must not select a tenant. Repositories and services must use organization-aware lookups for customer, import, signal, transaction, and report data.

## Data flow

1. A user authenticates and completes MFA when required.
2. The design system app sends an upload or customer action with the access token.
3. The GRC engine validates input and organization scope.
4. Transactions are persisted and evaluated by screening and monitoring services.
5. Signals and reports are created with audit evidence.
6. Authorized operators review, submit, or export the resulting workflow.

## Operational principles

- Fail closed for security-critical initialization and screening data.
- Make workflow transitions explicit and idempotent.
- Keep product responses actionable and free of legal citations.
- Keep costs visible through resource limits, labels, metrics, and operational budgets.
