---
name: add-flyway-migration
description: Add an append-only Flyway schema or data migration for Comply. Use when changing PostgreSQL schema or migration data; align JPA and tenant isolation, and preserve production rollout safety.
---

# Add a Flyway migration

1. Read root `AGENTS.md` and inspect all files in `apps/api/src/main/resources/db/migration` to find the current maximum version and affected schema.
2. Add `V<next>__descriptive_name.sql`. Never modify, rename, or delete an existing migration.
3. Make data migrations safe for existing populated databases; use additive/expand-and-contract changes when deployment compatibility requires it.
4. Every customer-owned row and reference must preserve `Organization` scope and referential integrity. Avoid globalizing tenant data.
5. Align entity mappings, repository queries, and nullability with the new schema. Add persistence/service tests for behavior and tenant boundaries.
6. Run the API tests and inspect migration SQL against both a clean schema and the relevant pre-change schema when infrastructure is available.
7. Document rollout, backfill, and operational sequencing in the appropriate runbook when needed.
