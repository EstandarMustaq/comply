---
name: add-api-endpoint
description: Implement, add, or change Spring API endpoints in Complyz. Use when editing controllers, endpoint request/response behavior, validation, authorization, or audit; preserve module boundaries, Organization tenant isolation, and contract coverage.
---

# Add or change an API endpoint

1. Read root `AGENTS.md` and `.github/instructions/api.instructions.md`. Identify the existing controller, service, repository, DTO, security rule, and tests for the domain.
2. Keep the flow controller → service → repository. Validate input at the boundary and keep business/transaction logic in services.
3. Derive the caller organization from the authenticated principal with `TenantAccessService`. Scope every query and indirect relationship; never accept a request tenant identifier as authorization.
4. Apply the least-privilege role policy in the established security configuration. Audit sensitive state changes without recording secrets or unnecessary personal data.
5. Add tests for normal behavior, validation/authorization failures, and cross-organization access.
6. If the contract changes, update Spring/OpenAPI, `packages/api-clients`, UI types/client, and contract tests in the same change.
7. Run `./gradlew :api:test --tests 'com.complyz.<module>.<TestClass>'`, then `pnpm api:test` and relevant contract checks.
