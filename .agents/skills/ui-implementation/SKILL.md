---
name: ui-implementation
description: Implement or change Comply Next.js routes, pages, product components, and user workflows in apps/ui. Use for UI implementation, shell integration, navigation, forms, tables, and interaction changes.
---

# Implement a Comply UI

1. Read root `AGENTS.md` and `.github/instructions/ui.instructions.md`. For Next.js-specific behavior, consult the installed guides under `apps/ui/node_modules/next/dist/docs/`.
2. Keep routes in `apps/ui/app/`, product components in `components/`, and primitives in `components/ui/`. Reuse the existing `AppShellClient`; add or change navigation in `components/shell/nav-config.tsx` rather than creating another shell.
3. Use server components by default and isolate client state to interactions. Call the API through `apps/ui/lib/api-client.ts`; update `lib/types.ts` and contract tests when the API contract changes. Localize labels without changing stable API values.
4. Follow the token, restrained sage/teal brand, compact-density, and shell-composition rules in the UI instructions. Preserve accessible focus and target sizes, responsive behavior, and honest loading, empty, error, and success states.
5. Write user-facing text in Comply's clear, direct, confident English voice. Reuse existing product components and UI primitives; keep domain behavior out of primitives.
6. Run the relevant Vitest or Playwright tests and `pnpm --filter @comply/web run check`. Run `pnpm check` for shared or cross-workspace changes.
