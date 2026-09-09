# Financial Compliance Intelligent — roadmap

## Delivered

- P0 security controls: bootstrap secrets, RBAC, token lifecycle, MFA hardening, screening blocking, backup protection, and API contract alignment.
- P1 compliance workflows: regime limits, monitoring idempotency, import validation, KYC/beneficial owners, report state integrity, and exports.
- P2 platform maturity: durable import storage, CI quality gates, observability/FinOps baseline, tenant controls, and versioned screening data lifecycle.

## Current phase

The product has a functional implementation baseline for controlled evolution. It is not automatically authorized for production: final operational acceptance, security review, data migration rehearsal, and release approval remain required.

## Next milestones

1. Complete API and architecture documentation under `docs/`.
2. Complete product-language boundary and prohibited-word checks.
3. Establish independent component repositories and migrate to Git submodules.
4. Add contract-completeness and clean-checkout gates to CI.
5. Run final operational and regulatory acceptance with production-like data controls.

## Release gates

- Tests, builds, migrations, Compose validation, and secret scans pass.
- No cross-tenant data access is possible.
- Product surfaces contain no external legal citations.
- Backup restore and screening-data rollback are rehearsed.
- Component repositories and root workspace commands are reproducible from a clean checkout.
