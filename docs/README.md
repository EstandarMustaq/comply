# FinComp documentation

FinComp is a **Financial Compliance Intelligent** product: a clear, auditable workspace for financial risk operations, customer due diligence, transaction monitoring, investigation, and reporting workflows.

## Documentation map

- [Product language](product-language.md) — product concept, terminology, and wording boundaries.
- [API catalog](api/README.md) — REST endpoints, contracts, behavior, authorization, errors, and side effects.
- [Architecture](architecture/README.md) — runtime topology, domain boundaries, data flow, security, tenancy, and operations.
- [Operations](operations/README.md) — local development, configuration, quality gates, observability, backup, and FinOps.
- [Traceability](traceability.md) — internal requirement and legal traceability. This is engineering documentation only and is not product/UI copy.
- [Submodule migration](submodule-migration.md) — controlled migration to independently versioned component repositories.
- [Archived engineering documents](archive/README.md) — historical plans, reviews, and implementation records.
- [Workspace configuration](../fincomphub-workspace.yaml) — declarative component and command configuration.

## Product-language boundary

Product surfaces use direct operational language. Legal and regulatory references belong only in internal traceability documentation and must not be emitted by the API, displayed in the UI, or embedded in generated product-facing messages.
