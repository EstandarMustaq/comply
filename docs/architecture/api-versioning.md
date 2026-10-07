# API versioning policy

## Current contract

- The public API currently exposes the `v1` major contract at `/api/v1`.
- The version prefix is applied centrally by `ApiVersioningConfiguration`; controller mappings contain resource paths only.
- OpenAPI keeps the production host in `servers.url` and the full versioned route in each `paths` entry. Do not put `/api/v1` in both locations.
- `info.version` identifies the OpenAPI document revision. It is not the public API major version.
- Do not publish a new API major version for additive, backward-compatible changes. Do not add minor or patch versions to the URI.

## Compatibility within a major version

Treat each published major version as a stable client contract. Within `v1`, preserve existing routes, request and response fields, field meanings, validation guarantees, defaults, and authorization behavior. Additive operations and optional response fields may be introduced when existing clients can ignore them safely. Clients should tolerate unknown additive response fields and enum values where the contract allows them.

Treat removing or renaming an operation or field, changing the meaning of existing data, narrowing accepted input, changing defaults, or changing authorization in a way that blocks existing valid consumers as potentially breaking. Assess each proposed change against the actual generated contract and consumer behavior before classifying it.

## Breaking changes and retirement

1. Introduce a new major route such as `/api/v2` only when a breaking change cannot be delivered compatibly. Keep the old major available while consumers migrate; never silently redirect an old major to a different contract.
2. Publish the replacement contract and a migration guide before deprecating the old one. Maintain a consumer inventory and notify affected consumers. Use aggregate HTTP route/status telemetry to monitor adoption and failure rates; if consumer attribution is needed but unavailable, establish a privacy-reviewed mechanism before setting a sunset date. Do not log credentials or unnecessary personal data.
3. Announce a deprecation and a sunset date with an agreed migration window. For affected responses, use the HTTP `Deprecation` and `Sunset` headers where applicable, alongside the public notice and migration instructions. These headers communicate lifecycle state; they do not replace consumer communication, usage monitoring, or security controls.
4. Remove the old route only after the published sunset date, migration review, and release approval. Maintain equivalent authorization and tenant isolation across every supported major version.
5. No current `v1` route is being retired. Do not emit deprecation or sunset headers until a specific route or version has an approved retirement plan.

## Contract verification

- Keep the route prefix in one Spring MVC configuration and keep authorization matchers aligned with that prefix.
- Generate API client types from Springdoc using `pnpm api:openapi` when the running API is available. Verify that each documented path contains exactly one major prefix and that the `servers` URL does not duplicate it.
- Update `packages/api-clients`, UI contract tests, and public API documentation together when the contract changes.
