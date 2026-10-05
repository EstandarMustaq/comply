# Repository instructions

Complyz is intelligent financial compliance for finance teams. This file is the canonical engineering guide for AI agents and contributors. Keep all product and documentation language in clear, direct, confident English; use `docs/standards/product-language.md` as the voice source of truth.

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

- UI copy, user-facing API messages, exports and public Mintlify documentation use Complyz's clear, direct, confident English voice. Engineering instructions and internal code documentation use concise technical English.
- Keep machine identifiers separate from display labels. Do not expose internal acronyms, implementation details or unsupported legal claims in product-facing text. Follow `docs/standards/product-language.md` and run `pnpm product-language:check` after product-copy changes.
- Complyz is an operational product: keep workflows clear, explainable, and traceable. Prioritize readable data density, accessible interactions, honest loading/empty/error states, restrained brand and destructive colors, and full-width analytical views.
- For frontend work, follow `.github/instructions/ui.instructions.md`. It is the baseline for design tokens, brand color, compact density, and shell composition. Consult installed Next.js guides under `apps/ui/node_modules/next/dist/docs/` before framework-specific changes. Next.js agent-file generation is disabled in `apps/ui/next.config.mjs`; do not add app-local agent instruction files.
- Keep route pages in the App Router, product components outside `components/ui/`, shared primitives in `components/ui/`, shell behavior in `components/shell/`, and API calls in `lib/api-client.ts`.
- Preserve the established high-density operations layout, neutral structural palette, restrained sage and red usage, accessible loading/empty/error states, and full-width data views. Use server components by default; add client boundaries only for interaction/state.
- Avoid synchronous state updates in `useEffect`; prefer event callbacks or asynchronous work. Any `useSearchParams()` usage must have a suitable Suspense boundary. Keep React Compiler exceptions narrowly scoped to the affected call.

## Automatic skill routing

Load the matching project skill before work without waiting for the user to name it. Load only applicable skills; combine them when a change crosses boundaries. Complyz instructions and the existing product design take precedence over generic external recommendations.

- Any customer-facing product change—including UI, data, integrations, jobs, and customer workflows: `.agents/skills/product-quality/SKILL.md`.
- Spring endpoint work: `.agents/skills/add-api-endpoint/SKILL.md`; contract or DTO changes also load `.agents/skills/sync-api-contract/SKILL.md`. Flyway changes: `.agents/skills/add-flyway-migration/SKILL.md`.
- Product-facing wording: `.agents/skills/check-product-language/SKILL.md`.
- UI routes, workflows, navigation, or components: `.agents/skills/ui-implementation/SKILL.md`. Visual design or UX work: also load `.agents/skills/impeccable/SKILL.md` and `.agents/skills/ui-ux-pro-max/SKILL.md`; UI/accessibility reviews: `.agents/skills/web-design-guidelines/SKILL.md`.
- React component architecture: `.agents/skills/vercel-composition-patterns/SKILL.md`; React/Next.js implementation, refactoring, data fetching, or performance: `.agents/skills/vercel-react-best-practices/SKILL.md`; complex TypeScript type design: `.agents/skills/typescript-advanced-types/SKILL.md`.
- Task briefs: `.agents/skills/write-task-brief/SKILL.md`. Production release requests: `.agents/skills/deploy-production/SKILL.md` before proposing or performing release steps; execution still requires explicit human authorization.
- Domain terminology, glossary entries, or durable architecture decisions: `.agents/skills/domain-modeling/SKILL.md`. Module interface, seam, depth, or testability design: `.agents/skills/codebase-design/SKILL.md`.
- Test-first work requested by the user, including red-green-refactor or integration-test design: `.agents/skills/tdd/SKILL.md`. Consult `codebase-design` when the public seam itself is uncertain.
- Plan or decision stress-testing when the user asks to be grilled: `.agents/skills/grilling/SKILL.md`. A requested codebase architecture exploration/report: `.agents/skills/improve-codebase-architecture/SKILL.md`.
- Explicit session handoff requests: `.agents/skills/handoff/SKILL.md`. Requests to prepare an async discovery questionnaire: `.agents/skills/to-questionnaire/SKILL.md`.
- Explicit branch, diff, or pull-request review requests: `.agents/skills/code-review/SKILL.md`. For security reviews, also load the global `review-security` skill. Invoke CodeRabbit, Bugbot, or another hosted reviewer only when the user explicitly requests and authorizes it; follow `product-quality` for the data-sharing guardrail.
- Creating or editing agent instructions or skills: `.agents/skills/writing-for-agents/SKILL.md`; use its `SKILL-MECHANICS.md` when changing skill invocation or routing.
- When available, use the global `frontend-design` skill for greenfield visual direction and `tailwind-design-system` for token/theme system changes. Keep both subordinate to the established Complyz UI baseline.

## Agent entry points

- GitHub Copilot: `.github/copilot-instructions.md` and `.github/instructions/`.
- Claude: `CLAUDE.md`.
- Cursor: `.cursor/rules/`.
- Shared repeatable workflows and installed skills: `.agents/skills/`.
- Git hooks: install with `pnpm hooks:install`; implementation is in `scripts/git-hooks.mjs`.
