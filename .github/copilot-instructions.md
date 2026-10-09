# Copilot instructions

Read the repository-wide [`AGENTS.md`](../AGENTS.md) for canonical engineering rules. For changes in `apps/ui`, follow [the UI-specific instructions](instructions/ui.instructions.md) and consult the installed Next.js guides. The instructions below highlight the architecture and conventions Copilot most often needs across layers.

## Repository model

Comply is a pnpm monorepo. pnpm workspaces and Turborepo coordinate the Next.js web application in `apps/ui` and shared packages under `packages/`. The Spring Boot API is a Gradle subproject, included as `:api` from `apps/api`; the root `scripts/workspace.mjs` orchestrates Gradle, Turbo, and Docker Compose commands. Local PostgreSQL and Redis services and application containers are defined in the root `docker-compose.yml`.

The web app calls the API through `apps/ui/lib/api-client.ts`; UI domain types are in `apps/ui/lib/types.ts`. API client types and route contracts are kept in `packages/api-clients`, generated from Springdoc by `pnpm api:openapi`. Unit and contract tests for the web client live in `packages/ui/tests`, with their runners configured from `apps/ui`.

The API uses module boundaries under `com.comply`: controllers handle HTTP, services hold business operations, repositories query persistence, and entities map the database. PostgreSQL schema changes are managed by Flyway in `apps/api/src/main/resources/db/migration`.

## Build, test, and lint

Use Node.js `26.5.0`, pnpm `12.8.1`, Java `21`, and Docker Compose v2. Run workspace commands from the repository root:

```bash
corepack enable
pnpm install --frozen-lockfile
pnpm dev                       # Starts PostgreSQL, Redis, API, and web app
pnpm check                     # Turbo lint/typecheck and root Prettier check
pnpm build                     # API bootJar and Turbo workspace build
pnpm test                      # API tests and all configured UI test suites
pnpm test:unit
pnpm test:contract             # Requires an API with OpenAPI available
pnpm test:e2e                  # Playwright; install Chromium if needed
pnpm lint
pnpm compose:config
pnpm hooks:install
pnpm hooks:test
pnpm operations:test
pnpm api:test                  # All API tests
```

Run one test without running its entire suite:

```bash
# One API test class; use a fully qualified JUnit class name.
./gradlew :api:test --tests 'com.comply.reporting.service.ReportExportServiceTest'

# One Vitest unit test; paths are relative to packages/ui.
pnpm --filter @comply/ui exec vitest run \
  --config ../../apps/ui/vitest.config.mts \
  --project unit tests/unit/api-client.test.ts

# One Playwright file and browser project.
pnpm --filter @comply/ui exec playwright test \
  --config ../../apps/ui/playwright.config.ts \
  --project desktop tests/e2e/shell.spec.ts
```

## Cross-layer conventions

- **Organization is the tenant boundary.** Derive the organization from the authenticated `User` with `TenantAccessService`; never authorize from an organization ID supplied by a request. Scope each customer-data query, mutation, report, aggregate, export, and indirect relationship to that organization. Reject missing organization context. Add tests proving one organization cannot access another organization's records. Treat shared/global records as explicit exceptions, not implicit fallbacks.
- API changes often cross the Spring/OpenAPI/UI boundary. When a DTO, route, response field, or nullability changes, regenerate or update `packages/api-clients`, update the UI types and API client, and adjust `packages/ui/tests/contract/api-contract.test.ts` in the same change.
- Flyway migrations that have been applied are immutable. Add the next versioned `V<n>__description.sql`; do not edit, rename, or remove an existing migration. Keep JPA nullability, repository behavior, and the migration schema aligned.
- Preserve the established module flow: controller → service → repository. Put business and transaction logic in services rather than duplicating it in controllers.
- Write product UI, user-facing API messages, exports and public Mintlify docs in clear, direct, confident English. Keep stable API identifiers separate from display labels; do not expose internal acronyms, implementation details or unsupported claims in user-facing copy. See `docs/standards/product-language.md` and use the `check-product-language` skill when changing product text.
- For UI work, follow the repository and UI-specific instructions. Keep the existing separation between App Router pages, product components, `components/ui` primitives, shell components, and `lib/api-client.ts`. Retain accessible loading, empty, and error states and the established high-density operational layout.
- Keep shared package dependencies on the `workspace:*` protocol. Root package scripts and `scripts/*.mjs` are the supported orchestration entry points; consult `package.json` and the relevant script before inventing a command.
- Reusable task playbooks are maintained in `.agents/skills/`.
- Apply `.agents/skills/product-quality/SKILL.md` automatically to product changes; review customer workflows, tenant boundaries, security, and operational impacts across the whole system.
