---
applyTo: "apps/api/**"
---

# API engineering rules

- Read root `AGENTS.md` first. The API is Java 21, Spring Boot, Gradle, and PostgreSQL/Flyway; run its tasks through root `./gradlew` or `pnpm api:*`.
- Keep HTTP concerns and request validation in controllers, business rules and transaction boundaries in services, and persistence queries in repositories.
- Resolve tenant context from the authenticated `User` with `TenantAccessService`. Never authorize from client-supplied organization identifiers. Include organization predicates through entity relationships in list, detail, mutation, aggregate, export, and background-work queries.
- Use explicit DTOs for external contracts. Keep role checks in the established security configuration and cover authorization behavior with tests.
- Record sensitive state changes through the audit service. Do not put credentials, tokens, MFA material, or unnecessary personal data in logs or audit details.
- Add a new Flyway version for every schema/data migration; never change an existing version. Ensure JPA constraints, migration SQL, and null behavior agree.
- For API surface changes, update/generate `packages/api-clients`, coordinate UI types/client and contract assertions, then run API and contract tests.
- Run an individual class with `./gradlew :api:test --tests 'com.complyz.<module>.<ClassName>'`; run the full API suite with `pnpm api:test`.
