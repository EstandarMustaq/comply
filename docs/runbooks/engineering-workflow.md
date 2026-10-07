# Engineering workflow

This guide documents the repository's supported checks for contributors. Product UI and public documentation use Complyz's clear, direct, confident English voice. Engineering materials use concise technical English.

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

- `pre-commit` checks staged whitespace, rejects environment/private-key files and high-confidence credential patterns, and prevents edits to existing Flyway migrations or reuse of migration version numbers.
- `commit-msg` enforces Conventional Commit subjects such as `fix(api): scope reports by organization`.
- `pre-push` determines changed paths from pushed refs and runs API tests, workspace checks/unit tests, and Compose validation as applicable. Central build/workflow/script changes trigger both API and web checks.

If a check fails, fix the underlying issue and rerun it; do not bypass the hook. Use the same root scripts locally and in CI.

Operational safety checks for the backup and deployment command wrappers run
in `pnpm operations:test`. They use fake local executables and never connect to
AWS, Docker, or production services.

## Architectural release checks

- Keep customer data scoped to the authenticated `Organization`; test cross-organization access denial, including related records, aggregation and exports.
- Add Flyway migrations as new versions only. Existing migration files are immutable after they may have been applied.
- For API contract changes, regenerate OpenAPI-derived client types with `pnpm api:openapi`, update UI types/client and contract assertions, and run the relevant API and UI tests.
- Keep product copy in clear, direct, confident English. Use the `check-product-language` skill and review changes against `docs/standards/product-language.md`.

## CI troubleshooting

### UI builds and release tags

The Quality Gates API job tests and packages the API with Java 21. The web job
uses `pnpm exec turbo run build` for the UI and shared packages. The root
`pnpm build` command also packages the API and requires Java.

Manual UI releases from a branch publish the commit SHA tag. Releases from a
`ui-v*` Git tag also publish that release tag. Release tags must use letters,
digits, underscores, periods, or hyphens and fit within Docker's 128-character
limit. The workflow validates tags before building.

### Table dependency upgrades

`apps/ui/components/data-table.tsx` uses the TanStack Table v9 compatibility API
from `@tanstack/react-table/legacy`. Its column and row types come from that same
entry point. Run the table component tests, workspace checks, and UI build when
updating the dependency.

### Dependabot reports `NoChangeError`

Compare the package manifest, the installed version in `pnpm-lock.yaml`, and any
`pnpm-workspace.yaml` overrides. An override can force a proposed update back to
the existing version. Keep the intended dependency version in its owning manifest
and regenerate the lockfile after changing an override.

### Container publishing reports `permission_denied: write_package`

The publishing workflows request `packages: write`. The target GHCR package must
also grant this repository access:

1. Open the affected package's settings. For the UI image, use
   [complyz-ui package settings](https://github.com/users/EstandarMustaq/packages/container/complyz-ui/settings).
2. Under **Manage Actions access**, grant `EstandarMustaq/complyz` **Write** access.
   A package administrator must make this change.
3. Rerun the failed publishing job after access is granted. It publishes the
   revision selected by the workflow.

Check the failure stage before changing build configuration. A completed image
build followed by a rejected registry push indicates a publishing-access problem.
Published images include the repository source URL and commit revision as OCI
labels. Existing package access must still be configured in GitHub.

### GitHub AI security scan reports a quota error

When the scan reports an exhausted monthly quota, an account administrator must
check the GitHub Copilot allowance. Rerun the scan when quota is available. A quota
failure means that the scan did not produce a security assessment for that run.
The `dynamic/agents/github-advanced-security` workflow is managed by GitHub; its
quota is controlled outside the repository's workflow files.
