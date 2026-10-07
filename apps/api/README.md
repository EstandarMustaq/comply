# Intelligent Financial Compliance

Connect systems to Complyz's current finance workflows through a versioned API. The API serves customer profiles, transaction imports, alerts, investigations, reports and activity history for regulated-finance operations.

## Run and test

Use the commands at the repository root:

```bash
pnpm api:run
pnpm api:test
pnpm api:build
```

`pnpm dev` starts the API and web app with local PostgreSQL and Redis services.
The API listens on port `8080`; version 1 resources are served under `/api/v1`.

## Data and access

Every customer and operational record belongs to the signed-in `Organization`.
The API checks the operator's access on every request. It records important
changes and locks submitted reports against edits.

Runtime settings are provided through environment variables and Spring
profiles. Keep secrets and local environment files out of source control. The
[API catalog](../../docs/api/index.mdx) describes the available operations and
their behavior.
