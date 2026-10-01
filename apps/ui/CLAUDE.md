# CLAUDE.md

Technical engineering standards, product voice guidelines, and architectural conventions for **Fincomp**.

---

## 1. Project Overview & Product Vision

Fincomp is a high-density transactional monitoring and compliance platform purpose-built for microfinance and microcredit institutions. The system provides real-time transaction screening, proactive anomaly detection, customer profile risk rating, and regulatory submission archiving.

- **Core Mission**: Compliance operations transform from reactive fire-fighting into a straightforward, systematic daily operational routine.
- **Target Audience**: Compliance and risk management officers within financial institutions. They understand operational compliance thoroughly and require dense, highly readable data with single-click decisions.
- **Product Tone**: Professional, direct, action-oriented Mozambican Portuguese (`pt-MZ`). Avoid patronizing instructional tone, artificial enthusiasm, and unnecessary legalistic jargon.

---

## 2. Essential Commands

All commands are executed using `pnpm` from the monorepo root:

```bash
# Monorepo dependency installation
pnpm install

# Local development server with Turbopack
pnpm dev

# Strict full verification cycle (typecheck + lint + prettier check)
pnpm run check

# Granular verification commands
pnpm run typecheck    # tsc --noEmit
pnpm run lint         # eslint .
pnpm run format:check # prettier --check "**/*.{ts,tsx,js,mjs,json,css,md}"
pnpm run format       # prettier --write "**/*.{ts,tsx,js,mjs,json,css,md}"

# Production build with Turbopack (standalone output)
pnpm run build

# Production server start
pnpm start
```

---

## 3. Engineering Architecture & Code Conventions

### Monorepo & TypeScript

- Strict configuration enabled: `noUncheckedIndexedAccess`, `noImplicitOverride`, and `strict`.
- Internal monorepo packages configured via pnpm workspaces:
  - `@fincomp/eslint-config` (`packages/eslint-config`)
  - `@fincomp/typescript-config` (`packages/typescript-config`)
- Inter-package dependencies must use the `"workspace:*"` protocol.
- Strict prohibition of `any`. Exception handling must always use `catch (err: unknown)` refined through `err instanceof Error`.
- Unused variables are treated as build errors; prefix with `_` only when required by function signatures.

### React 19 & Next.js 16 (App Router)

- **No Cascading Renders**: Avoid synchronous `setState` in `useEffect` mounting bodies. For client-side data fetching on component mount, decouple invocation via `queueMicrotask`:
  ```tsx
  useEffect(() => {
    queueMicrotask(() => {
      loadData()
    })
  }, [loadData])
  ```
- **Component Layer Separation**:
  - `components/ui/` — Base shadcn/ui primitives. Do not modify without explicit architectural rationale.
  - `components/` — Domain-specific Fincomp components (tables, drawers, cards, upload dropzones).
  - `components/shell/` — App shell layout (rail navigation, retractable sidebar, user profile menu).
- **TanStack Table v8**: When calling `useReactTable`, include the documented suppression comment for the React Compiler:
  ```tsx
  // eslint-disable-next-line react-hooks/incompatible-library -- TanStack Table v8 API cannot be memoized by React Compiler
  const table = useReactTable({ ... })
  ```
- **Suspense Boundaries**: Any component or page invoking `useSearchParams()` must be wrapped in a `<Suspense fallback={...}>` boundary to guarantee error-free static pre-rendering.

---

## 4. Design System & Interface Principles

- **Semantic Color Usage**:
  - The shadcn neutral scale governs structural chrome: backgrounds, card surfaces, borders, and secondary text.
  - The proprietary Fincomp Sage palette (derived from `#536760` / `hsl(165, 11%, 36%)`) is reserved exclusively for primary actions, active navigation states, and key data indicators.
  - Red is strictly reserved for destructive operations and urgent alerts (e.g., sanctions watchlist hits). Status badges and notification cards must use borders and text—never solid filled red blocks.
- **Unconstrained Table Widths**:
  - Do not apply arbitrary `max-w-*` constraints to data tables or analytical content areas. Operational users require full-screen width to inspect dense, multi-column records.
- **Persistent Viewport Shell**:
  - Layout is fixed to the viewport (`h-screen`) with sticky headers and sidebar rail navigation. Only the active content area scrolls.

---

## 5. Product Language & Voice Guidelines

The golden rule of product copy: **the technical and legal foundation sustains the product; it never speaks for it.**

### Forbidden on User Interfaces:

1. Regulatory acronyms (_KYC_, _AML_, _CFT_, _STR_, _CTR_, _UBO_) in navigation, headings, badges, or running UI text.
2. Statutory legal citations (e.g., law names, article numbers, decree references like _Decreto 53/2023_ or _Cap. XIII_).
3. Software infrastructure or internal library names (e.g., _OpenCSV_, _Apache POI_, _PostgreSQL triggers_).
4. Explanations of statutory obligations that motivated an interface element to exist.

### Approved Terminology Equivalence (UI Copy in PT-MZ):

| Forbidden on Interface             | Approved Fincomp Expression (PT-MZ)              |
| :--------------------------------- | :----------------------------------------------- |
| KYC / Diligência KYC               | Identificação e documentação / Diligência        |
| AML / CFT / Prevenção de BC        | Conformidade / Triagem e monitorização           |
| Relatório STR / STR Report         | Comunicações de operações suspeitas              |
| Relatório CTR / CTR Report         | Comunicações de transacções em numerário         |
| Regras de Limiares Legais          | Limiares de monitorização / Regras de triagem    |
| Trilha Forense / Postgres Triggers | Registo de actividade permanente                 |
| OpenCSV / Apache POI               | Ficheiros CSV (.csv) / Folhas de cálculo (.xlsx) |
| PEP                                | Pessoa com exposição política                    |

The brand is written strictly as **Fincomp**, Official domain: `tryfincomp.com`.
