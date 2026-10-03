---
applyTo: "apps/ui/**"
---

# Complyz UI baseline

## Tokens and brand

- Treat `apps/ui/app/globals.css` as the source of truth for color and typography tokens. Prefer existing semantic utilities such as `bg-background`, `text-foreground`, `border-border`, `text-muted-foreground`, and `bg-primary`; do not add raw color values to components.
- Use the existing Lausanne typeface and spacing/radius conventions. Change global tokens only for a deliberate system-wide need, and verify foreground/background contrast in both themes.
- Keep the sage/teal brand accent restrained: use semantic brand tokens for selected navigation, primary actions, and meaningful emphasis. Keep surfaces and structure neutral; reserve destructive colors for destructive or critical states.

## Compact operational density

- Treat product screens as Operate surfaces: prioritize task completion and scanability over marketing-style expression. Optimize for frequent scanning of operational data with full-width tables and work areas, concise labels, aligned numeric values, clear column hierarchy, and compact spacing from the existing scale.
- Keep body text legible and controls easy to use. Compact visual density does not justify shrinking interactive targets, removing focus indicators, or relying on color alone to communicate status.
- Avoid unnecessary nested cards, oversized marketing-style spacing, and ornamental gradients. Use emphasis to clarify priority, state, or action.

## Shell composition

- `components/shell/app-shell-client.tsx` owns the shared top bar, module rail, sidebar, and content composition. Render route content inside its existing content slot; do not create a second application shell or duplicate global navigation.
- Add or change module and sidebar navigation in `components/shell/nav-config.tsx`. Reuse the existing `AppShell*` primitives from `components/shell/app-shell.tsx` when shell-level behavior actually needs to change.
- Keep route pages in `app/`, product components in `components/`, shared primitives in `components/ui/`, and API access in `lib/api-client.ts`. Prefer server components and isolate client state to required interactions.

## Interaction, copy, and contracts

- Preserve keyboard and screen-reader access, responsive behavior, and truthful loading, empty, error, and success states. Keep focus visible and status meaning available without color.
- Write user-facing copy in direct, operational Mozambican Portuguese (`pt-MZ`). Keep machine enum values stable and resolve display text through localized label helpers. Do not expose internal acronyms, implementation details, legal citations, or legal deadlines in product copy.
- Coordinate endpoint or DTO changes with `packages/api-clients`, `lib/types.ts`, `lib/api-client.ts`, and contract tests.
- Before UI work, load the matching skills listed in root `AGENTS.md`: `ui-implementation` for implementation; `impeccable` and `ui-ux-pro-max` for visual or UX decisions; `vercel-composition-patterns` for component API architecture; `vercel-react-best-practices` for React/Next.js implementation and performance; and `web-design-guidelines` for interface/accessibility reviews. These skills inform the work but do not override the existing Complyz design system or the user's brief.
- For Next.js-specific behavior, consult installed guides under `apps/ui/node_modules/next/dist/docs/`. Validate UI changes with `pnpm --filter @complyz/web run check` and the relevant Vitest or Playwright selector; use `pnpm check` for workspace-wide changes.
