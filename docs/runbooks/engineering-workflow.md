# Engineering workflow

This guide documents the repository's supported checks for contributors. Product-facing UI and public docs remain in Mozambican Portuguese; engineering materials use English.

## Toolchain and workspace

Use Node.js `26.5.0`, pnpm `12.8.1`, Java `21`, and Docker Compose v2. Run package scripts from the repository root. pnpm/Turborepo manage the web application and TypeScript packages; the root Gradle build includes `apps/api` as `:api`.

Gradle's API build output belongs in `apps/api/build/`. `apps/api/bin/` is
disposable IDE/compiler output (compiled classes and copied resources); it is
ignored and must never be treated as source or packaged application input.

```bash
corepack enable
pnpm install --frozen-lockfile
pnpm dev
```

`pnpm check` runs workspace lint/typecheck and formatting checks. `pnpm build` builds the API and JS/TS workspace. `pnpm test` runs the API suite and configured UI tests; `pnpm test:unit`, `pnpm test:contract`, and `pnpm test:e2e` select UI test layers. Use `pnpm api:test` for the full API suite and `pnpm compose:config` to validate Compose profiles.

For focused runs:

```bash
./gradlew :api:test --tests 'com.complyz.reporting.service.ReportExportServiceTest'
pnpm --filter @complyz/ui exec vitest run --config ../../apps/ui/vitest.config.mts --project unit tests/unit/api-client.test.ts
pnpm --filter @complyz/ui exec playwright test --config ../../apps/ui/playwright.config.ts --project desktop tests/e2e/shell.spec.ts
```

## Git hooks

Enable repository hooks in a worktree with:

```bash
pnpm hooks:install
pnpm hooks:check
pnpm hooks:test
pnpm operations:test
```

The installer configures only that worktree's `core.hooksPath` to `.githooks` and is safe to repeat. CI skips local hook installation. `pnpm hooks:test` exercises hook behavior inside a disposable temporary Git repository. Hooks do not stage or rewrite files.

- `pre-commit` checks staged whitespace, rejects environment/private-key files and high-confidence credential patterns, prevents edits to existing Flyway migrations, checks new migration version numbers, and runs the product-language check when product surfaces change.
- `commit-msg` enforces Conventional Commit subjects such as `fix(api): scope reports by organization`.
- `pre-push` determines changed paths from pushed refs and runs API tests, workspace checks/unit tests, product-language validation, and Compose validation as applicable. Central build/workflow/script changes trigger both API and web checks.

If a check fails, fix the underlying issue and rerun it; do not bypass the hook. Use the same root scripts locally and in CI.

Operational safety checks for the backup and deployment command wrappers run
in `pnpm operations:test`. They use fake local executables and never connect to
AWS, Docker, or production services.

## Architectural release checks

- Keep customer data scoped to the authenticated `Organization`; test cross-organization access denial, including related records, aggregation and exports.
- Add Flyway migrations as new versions only. Existing migration files are immutable after they may have been applied.
- For API contract changes, regenerate OpenAPI-derived client types with `pnpm api:openapi`, update UI types/client and contract assertions, and run the relevant API and UI tests.
- Keep product copy in direct Mozambican Portuguese and run `pnpm product-language:check` for product-facing text changes.
