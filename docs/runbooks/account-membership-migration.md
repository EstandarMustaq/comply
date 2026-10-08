# Account and membership persistence rollout

## Scope and ownership

V25 expands the V24 schema; V26 adds reviewed commercial mapping support and
V27 applies that mapping. V28 adds external identity, factor-reference and session
lifecycle persistence owned by Identity & Access. Identity & Access owns `accounts`; Organization owns
`organizations` and `organization_memberships`. UUID references cross contexts
without JPA cascades. `platform_backoffice.commercial_clients` is a separate
namespace containing exactly identifier, name, contract reference and status.
It has no membership, policy, token, Organization navigation or authorization
behavior. The schema namespace alone does not establish a separate database
principal or back-office authentication boundary; those controls precede any
back-office endpoint.

The current API continues to authorize from the legacy User's Organization and
role. No Account or membership endpoint is introduced. The Organization's new
reference and version are excluded from legacy JSON, preserving HTTP contracts.
The repository interfaces are persistence foundations, not authorization services.
Consumers must derive Organization identity through `TenantAccessService`.

## Deterministic backfill

| Legacy value                                              | New value                                                                       |
| --------------------------------------------------------- | ------------------------------------------------------------------------------- |
| `users.id`                                                | `accounts.id`, `accounts.legacy_user_id`, initial `organization_memberships.id` |
| `users.organization_id`                                   | `organization_memberships.organization_id`                                      |
| `users.full_name`, `email`, `created_at`                  | Copied exactly to Account                                                       |
| `users.created_at`                                        | Initial membership `created_at`                                                 |
| `ADMIN`, `OCOS`, `COMPLIANCE_OFFICER`, `AUDITOR_READONLY` | Same Organization role, active membership                                       |
| No legacy equivalent                                      | Aggregate `version = 0`                                                         |

There is no automatic OWNER designation, case-folding, Account merging, new
privilege, or inference of platform identities. Case-variant emails remain
separate identities and require reconciliation before IdP linking. One legacy
role is retained per membership; task 0005 must introduce additional role grants,
external membership terms, invitations, last-owner rules and OCOS designation
controls before switching authorization. An Account can exist without a legacy
User or membership, and can have memberships across distinct commercial references.

Password hashes, encrypted MFA state, backup-code hashes and login timestamps
stay exclusively in `users`, unchanged. V28 creates separate external identity,
factor-reference and session records without migrating those credentials. It
creates no inferred IdP identity, factor enrollment or session for a legacy User.
Email alone must never link an IdP identity. Account remains the identity/profile
aggregate; the following records have their own lifecycle and optimistic version.

Account, Organization and membership UUIDs and optimistic versions are stable
inputs to task 0009. These tables are transitional persistence, not an event store
or an event history. Task 0009 must establish stream versions and bootstrap events
under a write freeze; it must not pretend historical events already exist.

## External identity, factor and session lifecycles (criterion 4)

V28 and the `users` entities/repositories implement the persistence portion of
D4 and task 0003's last criterion-4 clause. They support an Account with no
Organization membership and never read a CommercialClient or tenant membership
to establish identity ownership.

| Record                        | Stored data                                                                                                                        | Lifecycle                                                                                                                                                                                               |
| ----------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `account_external_identities` | Account UUID, exact issuer/subject, linked/unlinked timestamps, version                                                            | A link is active until explicitly unlinked. `(issuer, subject)` remains globally unique after unlink, preventing silent reassignment to another Account.                                                |
| `account_factor_references`   | Account and external identity UUIDs, non-secret provider factor identifier, registered/revoked timestamps, version                 | The record reflects an IdP enrollment. Individual revocation preserves the identity, other factor records and their histories. It does not claim to enroll, recover or remove a factor at the provider. |
| `account_sessions`            | Account and external identity UUIDs, SHA-256 fingerprint of the IdP session identifier, created/expiry/revoked timestamps, version | Individual revocation is terminal and idempotent. Expiry is exclusive: at the expiry instant the record is no longer active. Future-created records are not active.                                     |

An identity lookup matches verified issuer/subject exactly; there is no email
lookup or normalization. Issuer, subject and provider factor references are
nonblank, trimmed and limited to 255 characters. The provider factor identifier
must be its non-secret resource reference, never a TOTP seed or recovery code.
No new record has fields for passwords, TOTP seeds, recovery codes, access tokens
or refresh tokens. The session constructor hashes the opaque IdP session identifier
before persistence; raw input is not retained, and its fingerprint is excluded
from JSON. A fingerprint is correlation metadata and grants no authentication.

Composite foreign keys prevent attaching an Account's factors or sessions to
another Account's external identity. Foreign-key deletion is restrictive and
JPA does not cascade removal. Revoking one record does not delete its parent
Account, memberships or sibling records. Unlinking an identity immediately makes
its factor/session records ineligible in active queries while retaining history.
The original revocation/unlink timestamp is preserved on retry. Each record has
an optimistic version; stale writes cannot undo a concurrent revocation.

Account-scoped repositories are the read boundary. Historical lookups explicitly
include Account ID; active factor/session queries additionally require a linked
identity, a non-revoked record and applicable time bounds. Callers must supply the
authenticated Account ID and trusted server time. The exact issuer/subject lookup
is intentionally global to Identity & Access and is only for already-verified IdP
claims. Repository access by itself is not an authentication or policy decision.

The provider remains authoritative for enrollment/recovery and session evidence.
Task 0006 integrates these records with verified IdP/BFF events, token validation,
idle/absolute policy enforcement and back-channel logout. Revoking factor metadata
alone does not establish that the provider revoked sessions; task 0006 must record
the provider's session changes and apply its assurance rules. No legacy login
path is switched by V28. This separation preserves the approved owner of each
operation while implementing the record lifecycles required in task 0003.

For rollout, apply V28 before deploying the entities. Existing local authentication
continues against preserved User data; provider records begin empty. After verified
provider matching, Identity & Access may create the explicit link and record
factor/session observations. Never bulk infer them from local MFA/password state.
Rollback keeps V28 tables and deploys the prior compatible binary; forward fixes
must preserve identity ownership and revocation history. Unlink/revoke have no
implicit reactivation operation. A later recovery/relink command needs explicit
identity verification and audit, rather than moving an existing subject between
Accounts or resetting a terminal lifecycle timestamp.

## Commercial mapping and application

V26 supplies `platform_backoffice.commercial_reference_backfill`, a restricted
migration input table. Each row contains an existing Organization UUID, the
CommercialClient UUID/name/contract reference/status, a nonblank review reference
and a review timestamp. This provenance belongs to the migration input; the
CommercialClient record itself retains exactly its four approved fields.

V27 calls `public.apply_commercial_reference_backfill()`. The same function handles
late reviewed input after V27 has run. It validates the whole mapping before any
write, creates the specified commercial references, and fills only previously
unassigned Organization references. Successful assignments advance Organization
versions once. Matching records remain unchanged on replay. Table locks serialize
concurrent reconciliation and protect the input and existing metadata during the
transaction.

The database rejects unknown Organizations, duplicate Organization mappings,
missing review provenance and invalid metadata. Reconciliation rejects conflicting
metadata for one CommercialClient UUID, differences from an existing commercial
record, and attempted reassignment of an existing Organization reference. A failure
rolls back all reference creation and assignment. Neither metadata nor status changes
Organization membership, authorization, policy, entitlements or lifecycle.

There is no source for actual commercial contracts in V1–V24. An Organization
without reviewed input is returned as `UNRESOLVED`, and the restricted view
`platform_backoffice.unresolved_commercial_references` reports its UUID and reason:
`MISSING_REVIEWED_MAPPING` or `PENDING_APPLICATION`. Both staged and late mappings use the shipped function. No contract or association is inferred from names,
slugs, email domains or V18's default Organization.

### Operator procedure

Use the migration principal or an explicitly authorized operator database principal.
The function is security-invoker, has a fixed search path, and grants no privileges;
`PUBLIC` execution is revoked. Never grant it to a customer principal. A separate
operator needs explicit input/report access, function execution, and table privileges
for the writes and locks. Record the review in the deployment audit before loading
input; the review reference identifies that approval, not an authentication credential.

1. In a staged deployment, set Spring Flyway's target to `26`
   (`SPRING_FLYWAY_TARGET=26`) before starting the candidate with bootstrap disabled.
   Stop the application after migration while loading the reviewed input. The
   repository's `pnpm migrate` command runs Gradle checks; it does not apply database
   migrations. Spring Boot startup runs Flyway.
2. Load the reviewed CSV in a restricted `psql` session with `ON_ERROR_STOP` enabled.
   The header/order is `organization_id,commercial_client_id,name,contract_reference,status,review_reference,reviewed_at`.
   Values must use explicit UUIDs and `ACTIVE`/`INACTIVE` commercial metadata status.
   For example, replacing the path with the reviewed local file:

   ```sql
   BEGIN;
   \copy platform_backoffice.commercial_reference_backfill (organization_id, commercial_client_id, name, contract_reference, status, review_reference, reviewed_at) FROM '/secure/reviewed-commercial-mapping.csv' WITH (FORMAT csv, HEADER true)
   COMMIT;
   ```

3. Remove the Flyway target and resume migration. V27 applies the input and emits
   mapped/unresolved counts. Query the report below for row-level results. The CSV
   loading step is performed once per new input: duplicate Organization rows are
   rejected, including accidental reloads. Application of already loaded input is
   idempotent.
4. If V27 already ran, load newly reviewed rows and apply them in an operator
   transaction using the same implemented function:

   ```sql
   BEGIN;
   SELECT * FROM public.apply_commercial_reference_backfill();
   SELECT * FROM platform_backoffice.unresolved_commercial_references ORDER BY organization_id;
   COMMIT;
   ```

5. Correct rejected input against its review before retrying. Preserve applied input
   and review evidence. Changes to already assigned references or existing commercial
   metadata require an explicit reviewed forward correction with expected old values;
   reconciliation deliberately rejects implicit replacement. Keep commercial cutover
   gated on an empty unresolved report. During the compatible expand phase the
   reference remains nullable so legacy Organization writers and unresolved legacy
   data remain representable, as required by the staged rollout.

## Expand, backfill, validate, contract

1. Back up the database and test restoration on an isolated PostgreSQL instance.
   Capture counts from existing `users`, `organizations`, `clients` and `audit_log`
   before applying V25. The migration principal needs database `CREATE` permission
   for the new schema and permission to alter the existing tables. Inventory legacy
   bootstrap/provisioning processes and stop changes to User identity, role and
   Organization assignment during the backfill. Disable bootstrap user creation
   before migration; provision required legacy users before V25. Customer data
   operations need not be stopped solely for this identity expansion.
2. Rehearse against a representative copy with production cardinalities. Measure
   migration duration, WAL growth and lock waits. Adding columns and foreign keys
   takes locks on existing tables; schedule a maintenance window if measured waits
   exceed the deployment budget. Set deployment-level `lock_timeout` and
   `statement_timeout` to approved limits and abort/retry on timeout. The test
   fixtures below establish correctness, not production-scale lock/runtime bounds.
3. Run Flyway using the staged commercial procedure above when reviewed input is
   available. Each version is transactional: schema/data changes and that version
   history succeed together. Preserve V1–V24 checksums.
   Account and membership counts must each equal the pre-migration User count;
   every migrated row must match identity, Organization, role and timestamp.
4. Deploy this persistence-compatible binary. Older binaries continue using legacy
   fields. No dual-write trigger or cross-context synchronization is installed.
   Account/membership writes must remain disabled in product flows during mixed
   versions. Keep legacy identity provisioning/assignment frozen, or plan a
   reviewed delta reconciliation after all legacy writers stop. MFA/password
   changes can continue because credentials remain legacy-owned.
5. Review commercial mappings, unresolved owners and email collisions. Re-run the
   reconciliation query after all legacy writers stop. Any new/changed legacy
   User is an explicit cutover blocker, not an automatically merged identity.
   Implement delta reconciliation as a new migration; never rerun V25 SQL manually.
6. Tasks 0005/0006/0009 own authorization, IdP and event-write cutovers. Define a
   single writer per aggregate and complete security/compatibility gates before
   enabling their writes. Delay dropping legacy fields, credentials or references
   until every application version and recovery path has migrated.

## Validation queries

Run after V27 with an operator database connection. Review IDs in a restricted operations
channel; never export password/MFA fields in reports.

```sql
SELECT 'users' AS relation, count(*) FROM users
UNION ALL SELECT 'accounts', count(*) FROM accounts
UNION ALL SELECT 'organization_memberships', count(*) FROM organization_memberships
UNION ALL SELECT 'organizations', count(*) FROM organizations
UNION ALL SELECT 'commercial_clients', count(*) FROM platform_backoffice.commercial_clients;

-- Must return zero rows before identity cutover (also detects late legacy writes).
SELECT u.id, u.organization_id
FROM users u
LEFT JOIN accounts a ON a.legacy_user_id = u.id
LEFT JOIN organization_memberships m
  ON m.account_id = a.id AND m.organization_id = u.organization_id
WHERE a.id IS NULL OR a.id <> u.id OR m.id IS NULL
   OR a.full_name <> u.full_name OR a.email <> u.email
   OR a.created_at <> u.created_at OR m.created_at <> u.created_at
   OR m.role <> u.role OR m.status <> 'ACTIVE';

-- Must return zero rows before commercial cutover; V26+ reports exact pending IDs.
SELECT * FROM platform_backoffice.unresolved_commercial_references ORDER BY organization_id;

-- Explicit OWNER appointment is required before membership authorization cutover.
SELECT o.id FROM organizations o
WHERE NOT EXISTS (SELECT 1 FROM organization_memberships m
                  WHERE m.organization_id = o.id AND m.role = 'OWNER' AND m.status = 'ACTIVE');

-- Identify case-fold collisions for separate IdP reconciliation; never auto-merge.
SELECT array_agg(id ORDER BY id) AS account_ids
FROM accounts GROUP BY lower(email) HAVING count(*) > 1;
```

Foreign keys reject orphan identities, memberships and commercial references.
Deletion is restrictive; there is no cascading deletion of Account, User,
Organization, membership or commercial references. Commercial status cannot
suspend an Organization or membership.

## Failure and recovery

- If V25 fails, PostgreSQL rolls back the whole migration, including its Flyway
  history entry. Inspect the error and fix the verified legacy-data or deployment
  problem, then retry normal Flyway migration. Do not use `repair` to hide a
  checksum or data error. Failure/retry coverage uses an unapproved legacy role
  and proves no partially created Account or commercial schema remains.
- If V27 fails, V25/V26 and the reviewed input remain available while V27's
  writes/history roll back. Correct the rejected input or deployment failure and
  resume normal Flyway migration. If late application fails, roll back the operator
  transaction and retry the function after correction. Tests inject a failure after
  commercial record insertion and prove no partial reference or version change.
- After the expansion succeeds, roll the application back to the previous compatible binary
  while retaining the expanded schema. Do not run a down migration or remove
  Accounts/memberships: legacy data and references remain available.
- Correct a successful but incorrect mapping through a reviewed forward migration
  with explicit expected old values and before/after counts. Stop incompatible
  writers first; retain the correction's operator audit. After a later IdP/event
  cutover, old-binary rollback is no longer assumed safe: use that cutover's
  recovery plan and restore/replay procedure.

## Automated evidence

`AccountMembershipMigrationTest` applies the actual Flyway history to PostgreSQL
16, covering an empty database, four legacy roles across two Organizations,
case-variant identities, credentials and audit/client preservation, rerun,
transactional failure/retry and required/unique/check/foreign-key constraints.
The populated fixture has 4 Users before and 4 Accounts plus 4 active memberships
after; all 3 Organizations (including V18's default) remain commercially unresolved.

`AccountMembershipRepositoryIntegrationTest` validates JPA against Flyway and
checks Accounts without memberships, independent roles across commercial
references, scoped reads, unchanged legacy JSON/tenant authorization, optimistic
version increments and stale-write rejection. Run `pnpm api:test` for these and
the existing containment/authentication regressions.

`CommercialReferenceBackfillMigrationTest` covers the implemented V26/V27 path:
3 explicitly mapped Organizations, 2 commercial references, preserved User/Account/
membership snapshots, replay without version increments, partial mapping with exact
pending IDs, late mapping completion, conflicting and existing-reference rejection,
transactional failure/retry, input constraints, denied unprivileged execution and
concurrent application. Every Organization in the complete fixture is linked; the
partial fixture is resolved using the same shipped function.

`AccountSecurityLifecycleIntegrationTest` validates the V28/JPA schema on PostgreSQL:
issuer/subject uniqueness and terminal unlink, independent revocation and exact
expiry, historical preservation, Account isolation/composite foreign keys,
restrictive deletion, known SHA-256 persistence without raw identifiers, invalid
time rejection, duplicate records and stale-write rejection. The populated legacy
migration test additionally proves V28 leaves all three new tables empty while
preserving existing credential state.
