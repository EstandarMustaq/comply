---
name: add-ui-page
description: Build a Complyz Next.js route or workflow using the existing shell, API client, accessible states, and Mozambican Portuguese product voice.
---

# Add or change a UI page

1. Read root `AGENTS.md` and `.github/instructions/ui.instructions.md`. For framework-specific behavior, consult the installed Next.js guides under `apps/ui/node_modules/next/dist/docs/`.
2. Place routes in `apps/ui/app/`; add navigation through the existing shell navigation configuration. Keep server components by default and isolate interactive client state.
3. Call the backend through `apps/ui/lib/api-client.ts`; model API values in `lib/types.ts`. Use label helpers for localized display without changing stable enums.
4. Keep all user-facing copy direct, operational `pt-MZ`. Preserve responsive high-density data presentation, keyboard/screen-reader access, and loading/empty/error states.
5. Reuse product components from `components/` and primitives from `components/ui/`; avoid putting domain logic into primitives.
6. Test affected Vitest suites and browser flows. Run `pnpm --filter @complyz/web run check`; run `pnpm check` for shared or cross-workspace changes.
