# Complyz API

The API connects the Complyz workspace to its operational data. It manages
customer profiles, access, transaction imports, monitoring alerts, restricted
party screening, investigations, reports, and activity history.

## Run and test

Use the commands at the repository root:

```bash
pnpm api:run
pnpm api:test
pnpm api:build
```

`pnpm dev` starts the API and web app with local PostgreSQL and Redis services.
The API listens on port `8080`; its resources are served under `/api`.

## Data and access

Every customer and operational record is scoped to the signed-in organization.
Access is based on the operator's role. Important changes are recorded, and
submitted reports remain unchanged.

Runtime settings are provided through environment variables and Spring
profiles. Keep secrets and local environment files out of source control. The
[API catalog](../../docs/api/index.mdx) describes the available operations and
their behavior.
