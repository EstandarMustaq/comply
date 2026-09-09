# API contract details

This page complements the endpoint inventory with the stable payload families used by the application client. Exact optionality follows the Java DTOs and `frontend/lib/types.ts`; generated OpenAPI should become the next source of machine validation.

## Authentication payloads

- `POST /auth/login`: `{ email, password }`. Response can be a successful access/refresh pair or an MFA challenge.
- `POST /auth/mfa/verify`: `{ challengeToken, code }`. Response is an access/refresh pair after verification.
- `POST /auth/refresh`: `{ refreshToken }`. Response is a rotated access/refresh pair.
- `POST /auth/mfa/enroll`: optional enrollment token. Response contains enrollment metadata and backup-code material only during enrollment.
- `POST /auth/mfa/confirm`: `{ code, enrollmentToken }`. Response is an authenticated session.

## Customer payloads

Create customer requests contain `fullName`, `idDocumentType`, `idDocumentNumber`, optional `clientType`, `nuib`, `dueDiligenceRegime`, `isPep`, `pepCategory`, and `seniorManagementApproved`. Customer responses include identity, risk, profile state, and beneficial-owner data.

## Import payloads

- Upload is multipart with the `file` part. The response includes `batchId`, `fileName`, `format`, `headers`, `previewRows`, and `suggestedMapping`.
- Mapping confirmation contains a mapping object from source columns to domain fields. Processing returns batch status, row totals, errors, and created transaction results.

## Signal payloads

Signals expose `id`, `flagType`, `status`, `ruleTriggered`, `reason`, optional `slaDeadline`, linked customer/transaction data, and audit timestamps. Review requests contain a note and next status.

## Report payloads

Draft investigation reports can contain account details, suspicion description, relationship description, employee involvement, and a voluntary statement. Submission requires the configured submission reference and creates an immutable submitted state. Transaction reports expose aggregation period, amount, transaction count, and submission state.

## Compatibility rules

- The application client may map legacy aliases such as `token` to `accessToken` and `tempToken` to challenge/enrollment tokens while the migration is in progress.
- New endpoints must add a typed client method and a documented error/state contract before UI integration.
- DTOs must not expose persistence-only fields such as internal legal traceability metadata.
