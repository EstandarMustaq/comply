---
id: 0008
title: KYC client types and INDIVIDUAL migration
status: draft
branch: task/0008-kyc-client-types-and-individual-migration
scope:
  - apps/api/src/main/java/com/comply/clients/**
  - apps/api/src/main/resources/db/migration/V*__*.sql
  - apps/api/src/test/java/com/comply/clients/**
  - apps/ui/app/**/clients/**
  - apps/ui/components/clients/**
  - apps/ui/lib/**
  - packages/api-clients/**
  - packages/ui/tests/**
reserved:
  - apps/api/src/main/java/com/comply/users/**
  - apps/api/src/main/java/com/comply/tenancy/**
  - docs/**/*.mdx
depends_on:
  - 0001
  - 0003
  - 0007
---

# 0008 KYC client types and INDIVIDUAL migration

## Context

`ClientType` currently contains `INDIVIDUAL` and `CORPORATE`; a UI mapping defaults KYC records to `INDIVIDUAL`. In the target, `INDIVIDUAL` is not a tenant type and is removed from the enterprise product domain, while people remain valid KYC subjects. An enterprise KYC Client and an Organization are separate records even when both represent a business.

## Objective

Resolve KYC subject classification, API/UI mapping and stored legacy values for the enterprise model. Preserve natural persons as persons and provide an explicit, auditable relationship from corporate KYC Clients to an Organization only where the business workflow requires it.

## Acceptance criteria

- [ ] Domain/API/UI no longer default KYC records to `INDIVIDUAL` and do not confuse KYC `Client` with Account, Organization or CommercialClient.
- [ ] A reviewed classification maps every legacy `INDIVIDUAL` record to an explicit supported person-subject representation before removing/deprecating the enum; no person is automatically converted into a corporation.
- [ ] Any corporate KYC Client ↔ Organization relationship is explicit, tenant-scoped, validated and independently persisted; no table/entity is conflated solely because both describe a business.
- [ ] New migration(s) are append-only and tested against actual legacy values, unknown/null values, duplicate identifiers and rollback/forward recovery. No old Flyway file is edited.
- [ ] UI forms, validation, labels and API contracts express the approved person/business distinction in clear, direct English; no internal enum or implementation text leaks to users.
- [ ] Cross-Organization KYC access/duplicate-document rules are tested; generated client/types/contract tests remain synchronized. Product-facing copy follows `docs/standards/product-language.md`; `pnpm api:test` and `pnpm check` pass.

## Verification

```bash
pnpm api:test
pnpm check
```

## Operational considerations

Report classification counts and unresolved records before applying a destructive enum/schema change. Preserve customer evidence, audit history and existing natural-person records. If records cannot be classified deterministically, quarantine for reviewed remediation instead of guessing.

## Work log

- 2026-10-03: Brief created from the reviewed model; implementation has not classified or migrated existing KYC records.
