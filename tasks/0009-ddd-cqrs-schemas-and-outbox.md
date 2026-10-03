---
id: 0009
title: DDD, CQRS, shared schemas and transactional outbox
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

# 0009 DDD, CQRS, shared schemas and transactional outbox

## Context

The target proposes Identity & Access, Tenancy & Commercial, Policy and Compliance bounded contexts; `OrganizationAccess` and `CommercialClient` aggregates; CQRS-light without event sourcing; strict shared JSON Schemas; and transactional outbox events. These architecture choices must be ratified in task 0001 and introduced without a rewrite that destabilizes existing compliance workflows.

## Objective

Establish enforceable module boundaries, command/query seams, versioned shared contracts and reliable domain-event publication. Keep a single relational database and existing controller → service → repository flow where a conversion adds no value; use ArchUnit tests to prevent forbidden context dependencies.

## Acceptance criteria

- [ ] Bounded contexts and allowed dependency directions are recorded; ArchUnit tests enforce them, including `OrganizationAccess` owner invariant and CommercialClient aggregate ownership without leaking compliance entities into commercial administration.
- [ ] CQRS remains lightweight: commands own authorization, invariants and transactional quota checks; queries provide read projections constrained by authorized Organization IDs. No event sourcing is introduced.
- [ ] New `packages/contracts` contains versioned JSON Schemas for approved policy/template/floor documents, canonical ingestion payloads and context-token claims; schemas use strict properties (`additionalProperties: false`) and are compatible with both Java and TypeScript consumers.
- [ ] UI/BFF validates with strict AJV, `allErrors` disabled for untrusted input and schemas compiled at build time (standalone); external input cannot define or compile a schema. Java validates the same versioned schemas at command boundaries.
- [ ] Transactional outbox persists domain events atomically with state and the authorization-version bump for membership/policy/floor changes; publication is retryable, idempotent, observable and cannot expose cross-tenant payloads.
- [ ] Schema changes are versioned and contract-sync workflow is documented; invalid, unknown-version and incompatible inputs fail closed with safe errors.
- [ ] `pnpm api:test`, `pnpm check` and architecture-boundary tests pass.

## Verification

```bash
pnpm api:test
pnpm check
```

Include DB-backed transaction/rollback and duplicate-delivery tests for outbox events. Confirm strict AJV build artifacts are generated from trusted repository schemas only.

## Operational considerations

Schema evolution must support the chosen rollout compatibility window. Outbox retention, dead-letter remediation, replay authority and PII minimization require runbooks and metrics. Never let a replay bypass the original Organization authorization or duplicate a business side effect.

## Work log

- 2026-10-03: Brief created; CQRS/schema/outbox are target architecture, not current capabilities.
