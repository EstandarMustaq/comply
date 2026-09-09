# API catalog

Base path: `/api`. Authentication uses the access token issued by the authentication flow. Tenant scope is derived from the authenticated user; clients must not select a tenant by request payload.

## Contract conventions

- JSON request/response bodies use camelCase.
- IDs are UUID strings.
- Dates are ISO-8601 strings; timestamps include an offset or `Z`.
- `401` means the access session is missing or expired; clients may attempt one refresh rotation.
- `403` means the role or tenant scope does not permit the operation.
- `404` means the resource is absent from the caller scope.
- `409` means a state transition or idempotency conflict.
- `422`/`400` means request validation failed.
- Error bodies use `{ "message": "..." }` where a message is available.

## Endpoint inventory

### Authentication

| Method | Path | Behavior | Access |
|---|---|---|---|
| POST | `/auth/login` | Starts authentication and returns an access session or MFA challenge. | Public |
| POST | `/auth/mfa/verify` | Verifies a challenge and returns an access session. | Challenge token |
| POST | `/auth/refresh` | Rotates a refresh token and returns a new access/refresh pair. | Refresh token |
| POST | `/auth/mfa/enroll` | Creates an MFA enrollment challenge. | Authenticated |
| POST | `/auth/mfa/confirm` | Confirms MFA enrollment. | Enrollment token |
| POST | `/auth/logout` | Revokes the current access/refresh session. | Authenticated |
| GET | `/auth/me` | Returns the authenticated user summary. | Authenticated |

### Customers and KYC

| Method | Path | Behavior | Access |
|---|---|---|---|
| GET | `/clients` | Paginated, tenant-scoped customer search and filtering. | Authenticated |
| GET | `/clients/{id}` | Returns one customer in the caller tenant. | Authenticated |
| POST | `/clients` | Creates a customer profile. | Admin, compliance, operations |
| PATCH | `/clients/{id}/risk-rating` | Changes the risk rating and records activity. | Admin, compliance, operations |
| PATCH | `/clients/{id}/pep-review` | Records the latest review date. | Admin, compliance, operations |
| GET | `/clients/{clientId}/beneficial-owners` | Lists beneficial owners for a customer. | Authenticated |
| POST | `/clients/{clientId}/beneficial-owners` | Adds a beneficial owner. | Admin, compliance, operations |
| POST | `/clients/beneficial-owners/{ownerId}/verify` | Verifies a beneficial owner. | Admin, compliance, operations |
| POST | `/clients/{clientId}/kyc/recalculate` | Recalculates profile completeness and risk prerequisites. | Admin, compliance, operations |

### Risk signals and investigations

| Method | Path | Behavior | Access |
|---|---|---|---|
| GET | `/flags` | Lists tenant-scoped signals with optional status/type filters. | Authenticated |
| PATCH | `/flags/{id}/review` | Records a review note and moves a signal to the requested state. | Admin, compliance, operations |
| POST | `/flags/{id}/escalate-to-str` | Creates a compliance report from a signal. | Operations officer |

### Imports and transactions

| Method | Path | Behavior | Access |
|---|---|---|---|
| POST | `/import-batches` | Stores an upload, parses a preview, and returns suggested mapping. | Admin, compliance, operations |
| POST | `/import-batches/{id}/confirm-mapping` | Processes a stored upload with validated mapping and tenant ownership. | Admin, compliance, operations |
| GET | `/import-batches` | Lists import batches in the caller tenant. | Authenticated |
| GET | `/import-batches/{id}` | Returns one batch in the caller tenant. | Authenticated |

### Insights

| Method | Path | Behavior | Access |
|---|---|---|---|
| GET | `/insights/summary` | Returns dashboard counts and operational indicators for the tenant. | Authenticated |

### Compliance reports and exports

| Method | Path | Behavior | Access |
|---|---|---|---|
| GET | `/reports/str` | Lists investigation reports, optionally filtered to submitted items. | Authenticated |
| GET | `/reports/str/{id}` | Returns one investigation report. | Authenticated |
| PUT | `/reports/str/{id}` | Updates a draft report; submitted reports are immutable. | Admin, operations officer |
| POST | `/reports/str/{id}/submit` | Validates and submits a draft report once. | Operations officer |
| GET | `/reports/ctr` | Lists transaction reports. | Authenticated |
| POST | `/reports/ctr/{id}/submit` | Submits a transaction report once. | Operations officer |
| GET | `/reports/exports/str/{id}.pdf` | Downloads a submitted report export. | Admin, operations officer |
| GET | `/reports/exports/annual/{year}.xlsx` | Downloads the annual operational export. | Admin, operations officer |

### Settings, users, and audit

| Method | Path | Behavior | Access |
|---|---|---|---|
| GET | `/settings/threshold-rules` | Lists configurable monitoring rules. | Admin |
| PUT | `/settings/threshold-rules/{id}` | Updates a monitoring rule. | Admin |
| GET | `/settings/system-config` | Lists system configuration. | Admin |
| PUT | `/settings/system-config/{key}` | Updates one configuration value. | Admin |
| GET | `/settings/users` | Lists users in the caller organization. | Admin |
| POST | `/settings/users` | Creates a user in the caller organization. | Admin |
| PATCH | `/settings/users/{id}/reset-mfa` | Resets MFA for a user in the caller organization. | Admin |
| GET | `/audit-log` | Lists activity history for authorized operators. | Auditor, admin |

## Behavioral contracts

- Authentication state is represented by access and refresh tokens; refresh rotation is required after an expired access session.
- Draft compliance reports can be edited until submission. Submission is a one-way transition.
- Imports are owned by both the user and organization; cross-tenant access is rejected.
- Screening data is versioned and only active, valid versions participate in matching.
- Customer and operational data is tenant-scoped.
- Important state changes create audit records.
