# Fincomp GRC Engine

The GRC Engine is the Spring Boot service for Financial Compliance Intelligent. It owns authentication, MFA, organization scope, customer profiles, KYC, transaction ingestion, monitoring, restricted-party screening, reporting, exports, audit history, and persistence migrations.

The published Maven coordinates are:

```text
com.fincomp:grc-engine
```

## Requirements

- Java 21
- Maven 3.9+
- PostgreSQL 16+
- Redis 7+

## Commands

```bash
mvn test
mvn package -DskipTests
mvn spring-boot:run
```

From the platform workspace, use `./scripts/workspace.sh test`, `build`, or `dev`.

## Configuration

Runtime configuration is provided through environment variables and Spring profiles. Do not commit secrets, local environment files, build output, or generated reports.

## API

The service exposes REST resources under `/api`. The platform-level endpoint catalog and behavioral contracts are documented in the parent repository under `docs/api/`.

## Design boundaries

- Organization scope is derived from the authenticated principal.
- Access and refresh tokens have separate lifecycles.
- Important workflow transitions are auditable and report submission is immutable.
- Screening data is versioned, checksummed, time-bounded, and activated through a controlled lifecycle.
- Product responses use operational language; internal traceability is kept outside API responses.
