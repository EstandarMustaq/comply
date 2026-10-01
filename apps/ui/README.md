# Fincomp Design System

Modern, high-density transactional monitoring and compliance user interface purpose-built for microfinance and microcredit institutions. Fincomp equips compliance and risk management officers with real-time operational visibility over customer accounts, automated transaction screening, proactive anomaly detection, and structured regulatory filing preparation.

---

## Technology Stack

- **Framework**: [Next.js 16](https://nextjs.org/) (App Router & Turbopack)
- **UI Library**: [React 19](https://react.dev/)
- **Styling & Primitives**: [Tailwind CSS v3](https://tailwindcss.com/) & [shadcn/ui](https://ui.shadcn.com/)
- **Data Visualization**: [Recharts v3](https://recharts.org/)
- **Data Tables**: [TanStack Table v8](https://tanstack.com/table)
- **Monorepo Management**: [pnpm](https://pnpm.io/) Workspaces
- **Type Safety & Quality**: TypeScript 5.9 (strict mode), ESLint 9 (Flat Config), Prettier

---

## Repository Structure

```text
├── app/                          # Next.js App Router root
│   ├── (dashboard)/              # Authenticated domain routes
│   │   ├── clients/              # Customer due diligence and profile management
│   │   ├── flags/                # Operational transaction alerts and reviews
│   │   ├── import/               # Operational spreadsheet and transaction ingestion
│   │   ├── insights/             # Analytical compliance and portfolio metrics
│   │   ├── reports/              # Formal regulatory communications and archive
│   │   └── settings/             # Screening rules, threshold limits, and audit trail
│   ├── globals.css               # Design tokens, CSS variables, and layout resets
│   └── layout.tsx                # Root layout shell
├── components/                   # Domain-specific Fincomp components
│   ├── shell/                    # App shell, navigation rail, and site header
│   └── ui/                       # Base shadcn/ui primitives
├── lib/                          # Strongly-typed API client, schemas, and utilities
│   ├── api-client.ts             # Backend HTTP client service
│   ├── env.ts                    # Environment variable validation
│   ├── types.ts                  # Core domain and schema type definitions
│   └── utils.ts                  # Utility helpers (cn class merging)
├── packages/                     # Monorepo shared packages
│   ├── eslint-config/            # Shared ESLint configuration (@fincomp/eslint-config)
│   └── typescript-config/        # Shared TypeScript configuration (@fincomp/typescript-config)
├── Dockerfile                    # Multi-stage production container build
├── pnpm-workspace.yaml           # Monorepo workspace configuration
└── tailwind.config.ts            # Tailwind CSS theme tokens and Sage palette
```

---

## Prerequisites

- **Node.js**: `24.x`
- **pnpm**: `11.20.x`
- **Backend Service**: Running Fincomp GRC Engine API (or mock environment)

---

## Quick Start

### 1. Install Monorepo Dependencies

```bash
git clone https://github.com/fincomp/design-system.git
cd design-system

# Install all workspace dependencies
pnpm install --frozen-lockfile
```

### 2. Configure Environment Variables

Create a local environment file `.env.local` in the project root:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_APP_NAME=Fincomp
```

### 3. Start Local Development Server

```bash
pnpm dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

---

## Available Scripts

| Command                  | Description                                                         |
| :----------------------- | :------------------------------------------------------------------ |
| `pnpm dev`               | Starts the local development server with Turbopack                  |
| `pnpm run check`         | Executes typecheck, lint strict and format check                    |
| `pnpm run typecheck`     | Validates TypeScript types across the project (`tsc --noEmit`)      |
| `pnpm run lint`          | Runs static code analysis with ESLint                               |
| `pnpm run format`        | Automatically formats codebase with Prettier                        |
| `pnpm run format:check`  | Verifies code formatting compliance without modifying files         |
| `pnpm run build`         | Compiles optimized standalone production build with Turbopack       |
| `pnpm start`             | Launches production Node.js standalone server                       |
| `pnpm run test:unit`     | Validates the typed API client in jsdom                             |
| `pnpm run test:contract` | Verifies routes against the GRC Engine OpenAPI document             |
| `pnpm run test:e2e`      | Runs Chromium navigation, accessibility and visual regression gates |

---

## Design System & Visual Principles

- **Neutral Scale**: Layout chrome (navigation rails, surfaces, borders, and secondary text) is anchored to the shadcn neutral palette.
- **Sage Brand Accent**: Fincomp's brand tone (`#536760` / `hsl(165, 11%, 36%)`) is reserved for primary actions, active navigation states, and operational data highlights.
- **Red Restraint**: Red is strictly confined to destructive operations and critical alerts (e.g., sanctions watchlist hits). Status indicators use outline borders and text—never solid filled red blocks.
- **Standard Radius**: Globally centralized to `--radius: 0.5rem` (8px) in `app/globals.css`.

---

## Docker Containerization

The repository provides an optimized multi-stage `Dockerfile` with standalone Next.js compilation and a non-root system user:

```bash
# Build the production Docker image
docker build -t fincomp-app .

# Run container
docker run -p 3000:3000 \
  -e NEXT_PUBLIC_API_URL=http://api.example.com/api \
  fincomp-app
```

## API Contract

The typed client and route catalog live in `lib/api-client.ts`. Set
`FINCOMP_OPENAPI_URL` to the GRC Engine `/v3/api-docs` document and run
`pnpm run test:contract` to verify the integration. The workspace quality gate
starts the GRC Engine with the `contract` profile before this test.

---

## License

Proprietary and confidential. All rights reserved.
