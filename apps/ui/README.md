# Intelligent Financial Compliance

Build the interface for regulated finance teams who know what they’re doing. The Comply web app brings customer profiles, transaction activity, alerts, investigations and reports into one operational workspace, with the current state and next action up front.

## Run locally

From the repository root, install the shared dependencies and start the app:

```bash
pnpm install --frozen-lockfile
pnpm --filter @comply/web dev
```

The app runs at `http://localhost:3000` and connects to the API at
`http://localhost:8080/api/v1`. Copy `.env.example` to `.env.local` in this
directory to use different local service addresses or authentication settings.

## Verify changes

Run the checks and tests from the root workspace:

```bash
pnpm check
pnpm test:unit
pnpm test:contract
pnpm storybook:build
pnpm --filter @comply/web build
```

The root workspace owns dependency versions and shared settings. Reusable UI
utilities and tests live in `packages/ui`. Generated API types live in
`packages/api-clients` and follow the service contract.
