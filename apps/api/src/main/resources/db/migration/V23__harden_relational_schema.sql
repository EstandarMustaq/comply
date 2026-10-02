ALTER TABLE organizations
    ADD CONSTRAINT ck_organizations_slug_format
    CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$');

ALTER TABLE users
    ADD CONSTRAINT ck_users_email_trimmed
    CHECK (email = btrim(email) AND email <> '');

ALTER TABLE users
    ADD CONSTRAINT ck_users_role
    CHECK (role IN ('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS', 'AUDITOR_READONLY'));

ALTER TABLE clients
    ADD CONSTRAINT ck_clients_name_nonblank
    CHECK (btrim(full_name) <> ''),
    ADD CONSTRAINT ck_clients_document_nonblank
    CHECK (btrim(id_document_type) <> '' AND btrim(id_document_number) <> ''),
    ADD CONSTRAINT ck_clients_risk_rating
    CHECK (risk_rating IN ('LOW', 'MODERATE', 'HIGH')),
    ADD CONSTRAINT ck_clients_diligence_regime
    CHECK (due_diligence_regime IN ('NORMAL', 'SIMPLIFIED')),
    ADD CONSTRAINT ck_clients_type
    CHECK (client_type IN ('INDIVIDUAL', 'CORPORATE'));

ALTER TABLE beneficial_owners
    ADD CONSTRAINT ck_beneficial_owners_role
    CHECK (role IN ('FOUNDER', 'ADMINISTRATOR', 'TRUSTEE', 'BENEFICIARY', 'CONTROLLING_PERSON')),
    ADD CONSTRAINT ck_beneficial_owners_ownership_percentage
    CHECK (ownership_percentage IS NULL OR ownership_percentage BETWEEN 0 AND 100),
    ADD CONSTRAINT ck_beneficial_owners_name_nonblank
    CHECK (btrim(full_name) <> '');

ALTER TABLE import_batches
    ADD CONSTRAINT ck_import_batches_format
    CHECK (format IN ('CSV', 'XLSX')),
    ADD CONSTRAINT ck_import_batches_row_counts
    CHECK (total_rows >= 0 AND error_rows >= 0 AND error_rows <= total_rows),
    ADD CONSTRAINT ck_import_batches_column_mapping_object
    CHECK (column_mapping IS NULL OR jsonb_typeof(column_mapping) = 'object');

ALTER TABLE transactions
    ADD CONSTRAINT ck_transactions_amount_positive
    CHECK (amount > 0),
    ADD CONSTRAINT ck_transactions_currency_format
    CHECK (currency ~ '^[A-Z]{3}$'),
    ADD CONSTRAINT ck_transactions_retention_date
    CHECK (retention_until >= transaction_date),
    ADD CONSTRAINT ck_transactions_type
    CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'CREDIT'));

ALTER TABLE threshold_rules
    ADD CONSTRAINT ck_threshold_rules_type
    CHECK (type IN ('SINGLE_TRANSACTION_VALUE', 'MONTHLY_AGGREGATE_VALUE', 'STRUCTURING')),
    ADD CONSTRAINT ck_threshold_rules_threshold_positive
    CHECK (value_threshold IS NULL OR value_threshold > 0),
    ADD CONSTRAINT ck_threshold_rules_window_positive
    CHECK (aggregation_window_days IS NULL OR aggregation_window_days > 0);

ALTER TABLE flags
    ADD CONSTRAINT ck_flags_type
    CHECK (type IN ('KYC_INCOMPLETE', 'THRESHOLD_VALUE', 'STRUCTURING_SUSPECTED', 'SANCTIONS_MATCH', 'PEP_REVIEW_DUE')),
    ADD CONSTRAINT ck_flags_status
    CHECK (status IN ('PENDING', 'IN_REVIEW', 'ESCALATED_TO_STR', 'RESOLVED')),
    ADD CONSTRAINT ck_flags_reason_nonblank
    CHECK (btrim(reason) <> '');

ALTER TABLE suspicious_activity_reports
    ADD CONSTRAINT ck_suspicious_activity_reports_value_nonnegative
    CHECK (monetary_value IS NULL OR monetary_value >= 0);

ALTER TABLE currency_transaction_reports
    ADD CONSTRAINT ck_currency_transaction_reports_period
    CHECK (aggregation_period_end >= aggregation_period_start),
    ADD CONSTRAINT ck_currency_transaction_reports_value_nonnegative
    CHECK (aggregate_value >= 0),
    ADD CONSTRAINT ck_currency_transaction_reports_currency_format
    CHECK (currency ~ '^[A-Z]{3}$');

ALTER TABLE suspicious_activity_reports
    ADD CONSTRAINT ck_suspicious_activity_reports_required_text
    CHECK (
        btrim(reporting_institution) <> ''
        AND btrim(account_holder) <> ''
        AND btrim(suspicion_description) <> ''
        AND btrim(business_relationship_description) <> ''
        AND btrim(ocos_name) <> ''
    );

ALTER TABLE sanctions_list_entries
    ADD CONSTRAINT ck_sanctions_entries_entity_type
    CHECK (entity_type IN ('INDIVIDUAL', 'ORGANIZATION')),
    ADD CONSTRAINT ck_sanctions_entries_source_list
    CHECK (source_list IN ('LISTA_DESIGNADA_NACIONAL', 'UN_CONSOLIDATED'));

ALTER TABLE sanctions_list_versions
    ADD CONSTRAINT ck_sanctions_versions_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'ROLLED_BACK')),
    ADD CONSTRAINT ck_sanctions_versions_validity
    CHECK (valid_until IS NULL OR valid_until >= valid_from);

ALTER TABLE audit_log
    ALTER COLUMN entity_id DROP NOT NULL;

DROP INDEX IF EXISTS idx_users_email;
DROP INDEX IF EXISTS idx_flags_status;
DROP INDEX IF EXISTS idx_flags_sla;
DROP INDEX IF EXISTS idx_clients_sanctions_frozen;
DROP INDEX IF EXISTS uq_clients_org_document;

CREATE UNIQUE INDEX uq_clients_org_document
    ON clients (
        organization_id,
        upper(btrim(id_document_type)),
        upper(btrim(id_document_number))
    );

CREATE UNIQUE INDEX uq_organizations_slug_normalized
    ON organizations (lower(btrim(slug)));

CREATE INDEX idx_flags_review_queue
    ON flags (status, sla_deadline, created_at DESC)
    WHERE status IN ('PENDING', 'IN_REVIEW');

CREATE INDEX idx_transactions_screening
    ON transactions (client_id, type, transaction_date)
    INCLUDE (amount);

CREATE INDEX idx_clients_frozen_at
    ON clients (sanctions_frozen_at)
    WHERE sanctions_frozen = TRUE;

ALTER TABLE sanctions_list_entries
    DROP COLUMN IF EXISTS raw_reference;

UPDATE sanctions_list_versions
SET source_uri = 'classpath:/sanctions_national_seed.json'
WHERE source_uri = 'classpath:/sanctions_nacional_seed.json';
