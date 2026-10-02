# Complyz web app

The Complyz web app gives compliance and risk teams one place to review customer
profiles, transaction alerts, incoming data, investigations, reports, and team
activity. Its screens are designed around the decisions operators make each
day, with clear status, relevant context, and a record of what changed.

## Run locally

From the repository root, install the shared dependencies and start the app:

```bash
pnpm install --frozen-lockfile
pnpm --filter @complyz/web dev
```

The app runs at `http://localhost:3000` and connects to the API at
`http://localhost:8080/api`. Copy `.env.example` to `.env.local` in this
directory to use different local service addresses or authentication settings.

## Verify changes

Run the checks and tests from the root workspace:

```bash
pnpm check
pnpm test:unit
pnpm test:contract
pnpm test:e2e
pnpm --filter @complyz/web build
```

The root workspace owns dependency versions and shared settings. Reusable
interface utilities and tests are maintained in `packages/ui`; API types are
generated from the service contract in `packages/api-clients`.
