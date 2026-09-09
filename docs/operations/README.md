# Operations

## Workspace commands

Use `./scripts/workspace.sh help` to list the unified commands. The root command delegates to the current `package/compliance-engine/` and `app/application/` directories and is ready to switch to `package/compliance-engine` and `app/application` after submodule migration.

## Validation

- Backend: Maven tests and package.
- Frontend: TypeScript check and production build.
- Configuration: Docker Compose config validation.
- Scripts: `bash -n` for shell scripts.
- Repository: secret scan and generated-artifact checks.

## Environment

Use `.env.example` as the root contract. Production secrets must be injected by the deployment environment; local defaults are for development only.

## Recovery and cost controls

Backups are encrypted and validated before publication. PostgreSQL, Redis, imports, and edge services have resource limits in production Compose. Metrics and health probes are exposed through the compliance engine management endpoints.
