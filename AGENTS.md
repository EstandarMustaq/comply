# Repository instructions

Complyz is a multi-tenant financial operations platform for financial institutions. This file is the canonical engineering guidance for AI agents and contributors. Keep instructions in English; product UI and public documentation remain in Mozambican Portuguese (`pt-MZ`).

## Workspace

- Node.js `26.5.0`, pnpm `12.8.1`, Java `21`.
- `apps/ui` is the Next.js App Router application. Shared TypeScript packages live in `packages/`; use `workspace:*` dependencies.
- `apps/api` is the Spring Boot Gradle project, included as `:api` by the root `settings.gradle.kts`.
- pnpm/Turborepo manage the JS/TS workspace. Root `scripts/*.mjs` orchestrate Gradle, Turbo, and Docker Compose.
- `packages/api-clients` contains Springdoc-generated API types and the route contract. Unit/contract/browser suites are in `packages/ui/tests`; runner configuration is in `apps/ui`.

## Commands

Run from the repository root:

```bash
pnpm install --frozen-lockfile
pnpm dev
pnpm check
pnpm build
pnpm test
pnpm test:unit
pnpm test:contract
pnpm test:e2e
pnpm api:test
pnpm compose:config
pnpm hooks:install
pnpm hooks:test
pnpm operations:test
```

Run one API test class with `./gradlew :api:test --tests 'com.complyz.reporting.service.ReportExportServiceTest'`. Run one UI unit test with `pnpm --filter @complyz/ui exec vitest run --config ../../apps/ui/vitest.config.mts --project unit tests/unit/api-client.test.ts`. Run one browser test with `pnpm --filter @complyz/ui exec playwright test --config ../../apps/ui/playwright.config.ts --project desktop tests/e2e/shell.spec.ts`.

## Architecture and boundaries

- Keep API flow controller → service → repository. Put business rules and transaction boundaries in services; use DTOs for external request/response contracts.
- `Organization` is the tenant boundary. Derive it from the authenticated `User` through `TenantAccessService`; never treat a request-supplied organization ID as authorization. Scope every customer-related read, write, aggregate, report/export, cache key, background job, and indirect relation by organization. Fail closed when tenant context is missing. Explicitly identify intentionally global data. Add cross-organization denial tests.
- API contract changes must be coordinated across Spring controllers/DTOs, `packages/api-clients`, `apps/ui/lib/types.ts`, `apps/ui/lib/api-client.ts`, and `packages/ui/tests/contract/api-contract.test.ts` when applicable. Regenerate generated types with `pnpm api:openapi`; do not hand-edit generated output unless the generator itself cannot represent the contract.
- Flyway migrations are append-only. Add the next `V<n>__description.sql`; never edit, rename, or delete an existing migration. Keep migration, JPA nullability, and repository behavior aligned.
- Keep authentication, authorization, tenant scoping, validation, and audit logging server-side. Do not log credentials, tokens, MFA secrets, or unnecessary personal data.

## Product and UI

- UI copy, API messages shown to users, and public Mintlify documentation use direct, operational `pt-MZ`. Engineering instructions and internal code documentation use English.
- Keep machine identifiers separate from display labels. Do not expose internal acronyms, implementation details, external citations, or legal deadlines in product-facing text. Follow `docs/standards/product-language.md` and run `pnpm product-language:check` after product-copy changes.
- Complyz is an operational product: keep workflows clear, explainable, and traceable. Prioritize readable data density, accessible interactions, honest loading/empty/error states, restrained brand and destructive colors, and full-width analytical views.
- For frontend work, follow this guide and `.github/instructions/ui.instructions.md`. Consult the installed Next.js guides under `apps/ui/node_modules/next/dist/docs/` before framework-specific changes. Next.js agent-file generation is disabled in `apps/ui/next.config.mjs`; do not add app-local agent instruction files.
- Keep route pages in the App Router, product components outside `components/ui/`, shared primitives in `components/ui/`, shell behavior in `components/shell/`, and API calls in `lib/api-client.ts`.
- Preserve the established high-density operations layout, neutral structural palette, restrained sage and red usage, accessible loading/empty/error states, and full-width data views. Use server components by default; add client boundaries only for interaction/state.
- Avoid synchronous state updates in `useEffect`; prefer event callbacks or asynchronous work. Any `useSearchParams()` usage must have a suitable Suspense boundary. Keep React Compiler exceptions narrowly scoped to the affected call.

## Agent entry points

- GitHub Copilot: `.github/copilot-instructions.md` and `.github/instructions/`.
- Claude: `CLAUDE.md`.
- Cursor: `.cursor/rules/`.
- Shared repeatable workflows: `.agents/skills/`.
- Git hooks: install with `pnpm hooks:install`; implementation is in `scripts/git-hooks.mjs`.
- Apply `.agents/skills/product-quality/SKILL.md` automatically to product changes; scale its full-system review to the risk, especially for tenant data, customer workflows, and production operations.
