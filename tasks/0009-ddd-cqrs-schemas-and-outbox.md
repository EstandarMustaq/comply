---
id: 0009
title: DDD, event sourcing, event contracts and outbox
status: draft
branch: task/0009-ddd-cqrs-schemas-and-outbox
scope:
  - packages/contracts/**
  - apps/api/src/main/java/com/complyz/architecture/**
  - apps/api/src/main/java/com/complyz/shared/**
  - apps/api/src/main/java/com/complyz/**/outbox/**
  - apps/api/src/test/java/com/complyz/architecture/**
  - apps/api/src/test/java/com/complyz/shared/**
  - apps/api/src/main/resources/db/migration/V*__*.sql
  - apps/api/build.gradle.kts
  - apps/ui/package.json
  - pnpm-lock.yaml
  - packages/config/**
  - docs/adr/**
  - GLOSSARY.md
reserved:
  - apps/api/src/main/java/com/complyz/auth/**
  - apps/api/src/main/java/com/complyz/clients/**
  - apps/api/src/main/java/com/complyz/reporting/**
  - apps/ui/app/**
  - apps/ui/components/**
depends_on:
  - 0001
  - 0003
  - 0005
  - 0007
  - 0008
---

# 0009 DDD, event sourcing, event contracts and outbox

## Context

Task 0001 ratifies three bounded contexts—Identity & Access, Organization and Compliance—and makes each context solely responsible for changing its own aggregates. Event Sourcing supersedes the earlier CQRS-light/no-event-sourcing proposal. Cross-context communication uses business-named, versioned Event Contracts described in the accepted Context Map and validated against a trusted JSON Schema catalog. The task is architecture implementation, not permission to rewrite all existing compliance workflows at once.

## Objective

Establish enforceable context boundaries and an explicit Context Map; make event streams the write-side source of truth for the agreed aggregates; build rebuildable read projections and a transactional publication path; and publish a versioned event envelope/catalog shared safely by Java and TypeScript producers/consumers. Keep HTTP API major version, event-contract schema version and OpenAPI document revision as separate axes.

## Acceptance criteria

- [ ] The Context Map identifies each context's ownership, upstream/downstream relationship, published commands/events, translation/anti-corruption boundary and data it may reference. Identity & Access owns Account/profile aggregates; Organization owns Organization, memberships and policy/entitlement aggregates; Compliance owns KYC, transaction, monitoring, flag and report aggregates. `CommercialClient` remains a segregated platform-back-office reference (`organization.commercial_client_id`), not an Organization parent/group or aggregate in those contexts; its metadata is never an authorization or entitlement input.
- [ ] Only a context's command API can change its aggregates. Cross-context direct writes, shared mutable aggregate tables and repository dependencies that bypass the owning context are prohibited. ArchUnit/boundary tests enforce the map and aggregate ownership.
- [ ] Event streams are the write-side source of truth for the scoped aggregates. Appends enforce expected aggregate version/optimistic concurrency and stable ordering; commands are idempotent; state is derived by deterministic folds. Snapshotting is optional and never replaces the event history.
- [ ] Read models are projections, not independent write authorities; they are tenant-scoped, observable and rebuildable from committed events. Projection lag, rebuild, correction and consumer checkpoint behavior are documented and tested.
- [ ] Event types use stable business-language facts (for example, an Organization membership accepted or a report submitted), not generic CRUD, table or framework names. Each contract has a distinct name and explicit schema version; additive compatible changes preserve existing consumers, and breaking changes get a new contract version plus migration guidance.
- [ ] `packages/contracts` contains a versioned catalog, a common event envelope and per-event JSON Schemas. The envelope requires event ID/type/version, source context, aggregate ID/version, occurrence time, Organization attribution where applicable, correlation/causation IDs and payload. Schemas are strict, contain no credentials, and minimize regulated/personal data.
- [ ] Producers validate events before commit/publication; consumers validate before handling. TypeScript uses strict AJV with schemas compiled from trusted repository files; Java uses an equivalent JSON Schema validator against the same schema version. Unknown/incompatible schema versions fail closed or enter an observable quarantine path; external input cannot supply or compile schemas.
- [ ] Event append and publishable outbox record commit atomically. Delivery is at-least-once, ordered per aggregate where required, idempotent at consumers, retryable and observable. Replays cannot duplicate external/business side effects or cross Organization boundaries.
- [ ] Public REST API remains major-versioned (`/api/v1`); API compatibility rules and OpenAPI host/path representation are documented. Public API version, event schema version and OpenAPI `info.version` are never conflated. Protected resource-server required headers are specified/enforced by task 0006 and covered by contract tests.
- [ ] Schema compatibility tests, event evolution/upcasting tests, optimistic-concurrency and duplicate-delivery tests, deterministic projection rebuild tests, tenant-isolation tests and context-boundary tests pass. `pnpm api:test`, `pnpm check` and architecture tests pass.

## Verification

```bash
pnpm api:test
pnpm check
pnpm test:contract
```

Include database-backed transaction/rollback, concurrent append, duplicate delivery, replay, projection rebuild and schema compatibility tests. Verify strict AJV build artifacts are generated only from trusted repository schemas. Run the generated OpenAPI contract against the candidate API when available.

## Operational considerations

Event histories are append-only and must not be edited to repair a defect; corrections are compensating domain events. Define stream retention, backup/restore, encryption, access, legal retention, PII minimization/redaction strategy, projection rebuild time and forward recovery before production enablement. Never put passwords, tokens, IdP secrets or unnecessary KYC data in events. Treat event stores and DLQs as regulated customer-data stores. Roll out projections/consumers before switching command ownership; serialize migrations and prevent old and new writers from mutating the same aggregate concurrently.

## Work log

- 2026-10-03: Original brief proposed lightweight CQRS without event sourcing; that choice is superseded by task 0001's 2026-10-04 approval.
- 2026-10-04: Reframed around context-owned event-sourced aggregates, business-named/versioned event contracts, Context Map, JSON Schema catalog/envelope and the resource-server contract.
