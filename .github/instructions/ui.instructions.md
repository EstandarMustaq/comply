---
applyTo: "apps/ui/**"
---

# Web application rules

- Read root `AGENTS.md` before changing UI code. Consult installed Next.js guides under `apps/ui/node_modules/next/dist/docs/` for framework-specific behavior.
- Keep user-facing UI copy in professional, direct Mozambican Portuguese (`pt-MZ`). Engineering guidance and internal technical docs use English. Do not expose machine enums, internal acronyms, implementation details, legal citations, or legal deadlines as product copy.
- Use App Router pages for routes, `components/` for product components, `components/ui/` for primitives, `components/shell/` for navigation/layout, and `lib/api-client.ts` for API access.
- Prefer server components; isolate client components to required state and event handling. Preserve accessible loading, empty, and error states and the established dense, full-width operational data views.
- Resolve enum and status display through localized label helpers; do not rename stable API values to translate them.
- Coordinate API contract changes with `packages/api-clients`, `lib/types.ts`, `lib/api-client.ts`, and contract tests.
- Run `pnpm --filter @complyz/web run check` and the relevant `@complyz/ui` Vitest or Playwright selector. Use `pnpm check` for workspace-wide changes.
