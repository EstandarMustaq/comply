# Production operations

This runbook covers release readiness, service health, backup, and recovery for
Complyz deployments.

## Local operation

For local development, follow the repository's [setup guide](../../README.md).
`pnpm dev` starts the local database and cache before running the API and web
app. `pnpm compose:up` runs the application in containers.

Use the root `.env.example` as the configuration reference. Keep local values
in `.env`; use the deployment environment for production credentials.

## Release validation

- Run `pnpm api:test` for API tests.
- Run `pnpm check` and `pnpm test:unit` for web and shared-package checks.
- Run `pnpm test:contract` when the API is available.
- Run `pnpm test:e2e` with the Playwright browser installed.
- Run `pnpm compose:config` to validate application and production service
  settings.

GitHub Actions validate changes before publishing API packages or application
images. `pnpm deploy` operates the production profile in this worktree's
Docker Compose environment. The separate `Deploy to Amazon ECS` GitHub Actions
workflow deploys to ECS using its protected environment and approved inputs;
do not treat the local Compose command as an ECS release.

## Production configuration

Set `SPRING_PROFILES_ACTIVE=production` and `COMPLYZ_ENVIRONMENT=production` in
the deployment environment. Activate the production profile with `pnpm deploy`
or `docker compose --profile production up -d --build`. Production Compose uses
separate API, UI, PostgreSQL, and Redis services from the local application profile.
The production preflight and API startup validator reject development secrets,
unsafe bootstrap settings, and unexpected data volume names before production
services can start.

Before deploying, verify the configured PostgreSQL, Redis, imported-file, and
proxy volume names against the target environment. Compose resolves them from
`POSTGRES_VOLUME_NAME`, `REDIS_VOLUME_NAME`, `COMPLYZ_IMPORT_VOLUME_NAME`,
`CADDY_DATA_VOLUME_NAME`, and `CADDY_CONFIG_VOLUME_NAME`. Never use `down -v`
or otherwise remove production volumes as part of a release.

The deployment preflight requires the existing PostgreSQL and Redis volumes:

| Service    | Required production volume   | Configuration variable |
| ---------- | ---------------------------- | ---------------------- |
| PostgreSQL | `complyz_postgres_prod_data` | `POSTGRES_VOLUME_NAME` |
| Redis      | `complyz_redis_prod_data`    | `REDIS_VOLUME_NAME`    |

Set these values explicitly before deployment. Compose development defaults
use different names and must not be allowed to create empty production
volumes in place of existing data.

`pnpm backup` creates an encrypted PostgreSQL logical dump and uploads it to
the configured S3 bucket with Object Lock retention. It does not back up Redis
or imported files. Do not treat this command as a complete environment backup.
Supply the encryption passphrase through the deployment secret store and
maintain a separately protected recovery copy; losing it makes retained
database backups unrecoverable. Object Lock COMPLIANCE retention prevents
deletion until its configured expiry, so verify the target bucket and retention
settings before enabling uploads.
Verify a recent PostgreSQL backup and separately protect Redis and imported
files using the environment's approved backup process. Rehearse restoration
and readiness checks before routing traffic.

## Database migration recovery

Before reconciling Flyway history, take a verified backup and compare the
database's `flyway_schema_history` entries with the checked-in migration set.
Only run `repair` after reviewing the exact checksum or description mismatch
and confirming the schema itself is consistent. `repair` updates migration
history metadata; it does not execute migration SQL or reconcile schema
differences. Stop and investigate any unreviewed mismatch. Never run `repair`
automatically during application startup or deployment.

## Health and resource use

The API exposes readiness and operational health endpoints. Compose services
include health checks, resource limits, and bounded log retention. Review
service readiness and backup status as part of routine operations.
