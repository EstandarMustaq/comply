# Operations

## Workspace commands

Use `./scripts/workspace.sh help` to list the unified commands. The root command delegates to the `package/grc-engine/` and `app/design-system/` submodules.

## Validation

- GRC Engine: Maven tests and package.
- Design System: dependency install, ESLint, TypeScript check, and production build.
- Configuration: Docker Compose config validation.
- Scripts: `bash -n` for shell scripts.
- Repository: secret scan and generated-artifact checks.

## GitHub Actions

The workspace separates verification, publication, and deployment:

- `quality-gates.yml` runs on pull requests and pushes to `main`. It checks the recursive submodules, Java/Maven, pnpm/ESLint/TypeScript, Compose files, shell scripts, product language, workspace manifest, secrets, and diff integrity.
- The grc-engine submodule publishes `com.fincomp:grc-engine` to GitHub Packages from its own `publish-maven.yml` when manually dispatched or when a `v*` tag is pushed.
- The design-system submodule publishes its independently versioned container from its own `release.yml` when manually dispatched or when a `v*` tag is pushed.
- The workspace `publish-containers.yml` remains available for an integrated, validated snapshot of both components after the parent Quality Gates workflow.
- `deploy-ecs.yml` is a manually dispatched, GitHub-environment-protected deployment. It uses AWS OIDC and requires the ECS cluster, service, task definition, image tag, AWS region, and deploy role to be configured by the repository administrators.

The workflows initialize the SSH-configured private submodules through HTTPS on GitHub-hosted runners. Configure a repository or organization secret named `SUBMODULES_TOKEN` with read-only access to `tryfincomp/design-system` and `tryfincomp/grc-engine`. The default `GITHUB_TOKEN` cannot read sibling private repositories, so the dedicated token is required. This keeps the repository URLs aligned with `.gitmodules` while avoiding a dependency on runner SSH keys.

## Environment

Use `.env.example` as the root contract. Production secrets must be injected by the deployment environment; local defaults are for development only.

## Recovery and cost controls

Backups are encrypted and validated before publication. PostgreSQL, Redis, imports, and edge services have resource limits in production Compose. Metrics and health probes are exposed through the GRC engine management endpoints.
