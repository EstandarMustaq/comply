# Operations

## Workspace commands

Use `./scripts/workspace.sh help` to list the unified commands. The root command delegates to the `package/compliance-engine/` and `app/application/` submodules.

## Validation

- Backend: Maven tests and package.
- Application: dependency install, ESLint, TypeScript check, and production build.
- Configuration: Docker Compose config validation.
- Scripts: `bash -n` for shell scripts.
- Repository: secret scan and generated-artifact checks.

## GitHub Actions

The workspace separates verification, publication, and deployment:

- `quality-gates.yml` runs on pull requests and pushes to `main`. It checks the recursive submodules, Java/Maven, pnpm/ESLint/TypeScript, Compose files, shell scripts, product language, workspace manifest, secrets, and diff integrity.
- `publish-maven.yml` publishes the compliance engine to GitHub Packages only when manually dispatched or when a `v*` tag is pushed.
- `publish-containers.yml` publishes immutable SHA-tagged compliance-engine and application images to GHCR after successful `main` quality gates, or by explicit manual dispatch.
- `deploy-ecs.yml` is a manually dispatched, GitHub-environment-protected deployment. It uses AWS OIDC and requires the ECS cluster, service, task definition, image tag, AWS region, and deploy role to be configured by the repository administrators.

The workflows initialize the SSH-configured submodules through HTTPS on GitHub-hosted runners. This keeps the repository URLs aligned with `.gitmodules` while avoiding a dependency on runner SSH keys.

## Environment

Use `.env.example` as the root contract. Production secrets must be injected by the deployment environment; local defaults are for development only.

## Recovery and cost controls

Backups are encrypted and validated before publication. PostgreSQL, Redis, imports, and edge services have resource limits in production Compose. Metrics and health probes are exposed through the compliance engine management endpoints.
