# Financial Compliance Intelligent

Financial Compliance Intelligent is an auditable workspace for customer profiles, transaction monitoring, restricted-party screening, investigations, reporting, and operational insight.

## Repository layout

- `package/compliance-engine/` — Spring Boot compliance engine (migration target: `package/compliance-engine`).
- `app/application/` — Next.js application (migration target: `app/application`).
- `docs/` — product, API, architecture, operations, and internal traceability documentation.
- `scripts/workspace.sh` — unified workspace command surface.

## Quick start

1. Copy `.env.example` to a local environment file and set secrets as needed.
2. Run `./scripts/workspace.sh install`.
3. Run `./scripts/workspace.sh dev`.
4. Open the application at `http://localhost:3000`.

## Common commands

```text
./scripts/workspace.sh help
./scripts/workspace.sh test
./scripts/workspace.sh build
./scripts/workspace.sh compose-config
./scripts/workspace.sh quality
```

## Documentation

Start with [`docs/README.md`](docs/README.md), [`docs/api/README.md`](docs/api/README.md), and [`ROADMAP.md`](ROADMAP.md).

## Product language

User-facing language is operational and product-owned. External legal references are internal traceability material only and are not returned by the API or shown in the application.
