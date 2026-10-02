---
name: sync-api-contract
description: Keep Spring endpoints, generated OpenAPI client types, the typed UI API client, and contract tests synchronized.
---

# Synchronize the API contract

1. Identify changed controller routes, DTOs, validation, response fields, and nullability.
2. Generate the client from a running API with OpenAPI enabled using `pnpm api:openapi`; do not manually drift generated output.
3. Update `apps/ui/lib/types.ts` and `apps/ui/lib/api-client.ts` to match actual backend values. Keep display localization in label helpers, not wire values.
4. Update `packages/ui/tests/contract/api-contract.test.ts` and add focused tests for changed mapping or error behavior.
5. Update public API documentation in `docs/` in Portuguese when the documented interface changes.
6. Run `pnpm --filter @complyz/api-clients run typecheck`, `pnpm test:contract`, and relevant API/UI tests.
