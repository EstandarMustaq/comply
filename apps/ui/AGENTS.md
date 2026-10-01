# AGENTS.md

Mandatory rules for any coding agent working on this repository. Read thoroughly before writing the first line of code.

## Core Principle

Cohesive, integrated, and well-aligned systems where no single layer oversteps the boundary of another. Each component fulfills its designated role and never duplicates or masks another. This applies equally to code architecture, user interface design, and product copy.

The single most critical operational consequence: **the technical and legal foundation sustains the product; it never speaks for it.** Statutory laws, article numbers, regulatory acronyms, software libraries, and database infrastructure details belong in internal documentation and backend code—never on user-facing screens. Users must interact with the language of their everyday operational workflow, not the language of statutes or internal software dependencies.

## Forbidden Interface Elements

The following must **never** appear on any user-facing screen:

- Full statutory law titles, article numbers, regulatory notices, or executive decrees (e.g., _Decreto 53/2023_, _Capítulo XIII_)
- Regulatory acronyms (_AML_, _CFT_, _KYC_, _STR_, _CTR_, _UBO_) in navigation, headings, badges, or running UI text
- Software library or infrastructure dependency names (e.g., _OpenCSV_, _Apache POI_, _PostgreSQL triggers_)
- Statutory legal text copied directly as menu names, page titles, or section labels
- Educational explanations of the underlying legal obligations or architectural choices that caused a screen or field to exist

Formal legal citations and statutory references are strictly permitted only within exportable regulatory reports generated for official supervisory submission, where they serve an actual legal compliance purpose.

### Vocabulary Equivalence Table

| Forbidden on Interface                   | Approved Fincomp Expression (PT-MZ)              |
| :--------------------------------------- | :----------------------------------------------- |
| KYC / KYC Due Diligence                  | Identificação e documentação / Diligência        |
| AML / CFT / BC Prevention                | Conformidade / Triagem e monitorização           |
| STR (Suspicious Transaction Report)      | Comunicações de operações suspeitas              |
| CTR (Currency Transaction Report)        | Comunicações de transacções em numerário         |
| Legal Threshold Rules                    | Limiares de monitorização / Regras de triagem    |
| Forensic Audit Trail / Postgres Triggers | Registo de actividade permanente                 |
| Processed by OpenCSV / Apache POI        | Ficheiros CSV (.csv) / Folhas de cálculo (.xlsx) |
| PEP (in running text to user)            | Pessoa com exposição política                    |

## Engineering & Code Standards

**Pre-submission verification commands:**

```bash
pnpm run check     # typecheck + lint + format:check
pnpm run build     # production Turbopack compilation
```

Nothing is committed or submitted if either `pnpm run check` or `pnpm run build` fails. The TypeScript configuration enforces `strict`, `noUncheckedIndexedAccess`, and `noImplicitOverride` by design to catch array index hazards and class overrides before production.

**Monorepo Architecture:**

- `app/(dashboard)/` — Next.js App Router authenticated screen routes, organized one directory per domain screen
- `components/shell/` — App shell layout, modular rail navigation, user profile trigger, and navigation configuration
- `components/ui/` — Base shadcn/ui primitives (do not modify without explicit, documented design justification)
- `components/` — Fincomp business domain components (data tables, review drawers, metric cards, upload zones)
- `lib/` — Strongly-typed HTTP API client, core domain schemas, environment validators, and utility helpers
- `packages/eslint-config` — Shared ESLint flat configurations (`@fincomp/eslint-config`)
- `packages/typescript-config` — Shared TypeScript configurations (`@fincomp/typescript-config`)

**Conventions:**

- New domain-specific components must be placed in `components/`, never inside `components/ui/`
- Workspace package dependencies must use the pnpm workspace protocol: `"@fincomp/eslint-config": "workspace:*"`
- Unused variables are treated as compile errors; prefix with `_` only when required by a strict function signature
- `any` is strictly prohibited. Error handling must use `catch (err: unknown)` refined via `err instanceof Error`
- Never reassign variables during component render; derive values using `useMemo`
- Declare helper functions before invoking them inside sibling functions within the same component

**Lifecycle & Hook Patterns (React 19 / React Compiler):**

- **No Cascading Renders**: Never call synchronous `setState` inside the body of a `useEffect`. For initial client-side asynchronous data fetching on mount, wrap the invocation in `queueMicrotask`:
  ```tsx
  useEffect(() => {
    queueMicrotask(() => {
      loadData()
    })
  }, [loadData])
  ```
- **TanStack Table v8**: Because TanStack Table v8 returns non-memoizable function references that trigger React Compiler warnings, suppress the warning with an explicit inline comment:
  ```tsx
  // eslint-disable-next-line react-hooks/incompatible-library -- TanStack Table v8 API cannot be memoized by React Compiler
  const table = useReactTable({ ... })
  ```
- **Modal State Resets**: Move state resets for sheets and dialogs into event callbacks (`onOpenChange`), rather than reactive effects observing open/close props.

## User Interface Guidelines

**Color Palette.** The shadcn neutral scale governs layout structure: backgrounds, card surfaces, typography, and borders. Fincomp's proprietary sage palette (derived from `#536760`) is reserved for primary actions, active navigation states, and key data highlights. If brand color is used everywhere, it signifies nothing.

**Red Restraint.** Red is strictly reserved for destructive actions (e.g., account termination) and maximum-severity compliance alerts (e.g., sanctions watchlist matches). On warning cards and status badges, always use borders and text—never solid filled red blocks. Solid red backgrounds cause visual fatigue and disrupt attention hierarchy during prolonged operational use.

**Full Viewport Width.** Never impose arbitrary `max-w-*` constraints on data tables or central analytical views. Fincomp tables contain dense multi-column operational data; compliance officers require the entire screen for accurate auditing.

**Shell Layout.** An anchored viewport height (`h-screen`), with fixed header and sidebar navigation rails. Scrolling is confined strictly to the active module's content container.

**Data States.** Every table or view fetching asynchronous data must provide an accessible loading state (skeletons or spinners) and an honest, contextual empty state written in the product's voice (never leave blank, unguided regions).

## Pre-Delivery Verification Checklist

1. `pnpm run check` passes with **0 errors and 0 warnings** (TypeScript, ESLint, Prettier).
2. `pnpm run build` passes with all static routes successfully generated via Turbopack.
3. No screen or component exposes statutory names, article numbers, regulatory acronyms (_KYC_, _AML_, _STR_, _CTR_), or internal tool dependencies (_OpenCSV_, _Apache POI_, _PostgreSQL triggers_).
4. No solid-filled red badges or warning containers exist outside destructive action buttons.
5. Accessible loading and empty states are present across all data-fetching views.

<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->
