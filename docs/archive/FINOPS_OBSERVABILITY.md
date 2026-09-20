# Fincomp Observability and FinOps Baseline

> **Historical baseline — current terminology:** This document records the earlier Fincomp baseline. The current product is **Financial Compliance Intelligent**, with the service in `package/grc-engine`, the design system app in `app/design-system`, and workspace operations exposed through `scripts/workspace.sh`.

This repository now exposes a production baseline for operational telemetry and resource governance.

## Observability

- Spring Boot Actuator exposes `health`, `info`, `metrics`, and Prometheus-compatible metrics.
- Liveness and readiness probes are available at:
  - `/actuator/health/liveness`
  - `/actuator/health/readiness`
- Production Compose waits for GRC engine and design system health before starting Caddy.
- Docker health checks cover Caddy, design system, GRC engine, PostgreSQL, and Redis.
- Container logs use bounded `json-file` rotation (`10m` per file, three files).

The Actuator endpoints are intentionally not routed through the public Caddy API path. Expose them only through an internal network, authenticated monitoring proxy, or private management port before connecting an external scraper.

## Resource and cost controls

Production containers have configurable CPU and memory limits:

| Service | CPU variable | Memory variable | Default |
|---|---|---|---|
| GRC Engine | `GRC_ENGINE_CPU_LIMIT` | `GRC_ENGINE_MEMORY_LIMIT` | `1.00` / `1024m` |
| Design System | `DESIGN_SYSTEM_CPU_LIMIT` | `DESIGN_SYSTEM_MEMORY_LIMIT` | `0.50` / `512m` |
| PostgreSQL | `POSTGRES_CPU_LIMIT` | `POSTGRES_MEMORY_LIMIT` | `1.00` / `1024m` |
| Redis | `REDIS_CPU_LIMIT` | `REDIS_MEMORY_LIMIT` | `0.50` / `512m` |
| Caddy | `CADDY_CPU_LIMIT` | `CADDY_MEMORY_LIMIT` | `0.25` / `128m` |

Compose labels identify environment, service, and cost center for inventory and allocation reporting. The import staging directory is persisted in the `fincomp_imports` volume and is controlled by the existing import TTL cleanup job.

## AWS operating requirements

For an AWS deployment, connect Prometheus metrics to CloudWatch/Application Signals or a managed Prometheus workspace, and create alarms for:

- GRC engine readiness failures and HTTP 5xx rate;
- JVM heap, container memory, CPU throttling, and database connection pool exhaustion;
- PostgreSQL storage growth and Redis memory pressure;
- import backlog and failed import rows;
- backup job failures and backup age.

Create monthly AWS Budgets with alerts at 50%, 80%, and 100% of the approved limit. Apply the Compose cost-center labels to the corresponding ECS/EC2 resources, enforce mandatory `Environment`, `Service`, `CostCenter`, and `Owner` tags, and review Cost Explorer by service and tag monthly.

No AWS budget or alarm is created automatically by this repository because account, region, notification topic, and approved spending limits are deployment-specific.
