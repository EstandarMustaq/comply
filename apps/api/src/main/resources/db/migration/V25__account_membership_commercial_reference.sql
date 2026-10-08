-- Expand only. Legacy credentials and authorization remain in users until the IdP cutover.
-- The back-office namespace is deliberately separate from customer identity and tenancy.
CREATE SCHEMA platform_backoffice;
CREATE TABLE platform_backoffice.commercial_clients (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL CHECK (btrim(name) <> ''),
    contract_reference VARCHAR(255) NOT NULL CHECK (btrim(contract_reference) <> ''),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

ALTER TABLE organizations
    ADD COLUMN commercial_client_id UUID REFERENCES platform_backoffice.commercial_clients(id),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0);
CREATE INDEX idx_organizations_commercial_client ON organizations(commercial_client_id);

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    legacy_user_id UUID UNIQUE REFERENCES users(id),
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE CHECK (email = btrim(email) AND email <> ''),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT ck_accounts_legacy_identity CHECK (legacy_user_id IS NULL OR legacy_user_id = id)
);

CREATE TABLE organization_memberships (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    account_id UUID NOT NULL REFERENCES accounts(id),
    role VARCHAR(50) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'OCOS', 'COMPLIANCE_OFFICER', 'AUDITOR_READONLY')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT uq_membership_organization_account UNIQUE (organization_id, account_id)
);
CREATE INDEX idx_memberships_account ON organization_memberships(account_id);

-- V23 already constrains the four legacy roles. No OWNER or additional grants are inferred.
-- UUID equality is intentional: the mapping is reproducible without email normalization/merging.
INSERT INTO accounts (id, legacy_user_id, full_name, email, created_at)
SELECT id, id, full_name, email, created_at FROM users;

INSERT INTO organization_memberships (id, organization_id, account_id, role, status, created_at)
SELECT id, organization_id, id, role, 'ACTIVE', created_at FROM users;

DO $$
DECLARE
    unresolved BIGINT;
BEGIN
    IF EXISTS (
        SELECT 1 FROM users u
        LEFT JOIN accounts a ON a.legacy_user_id = u.id
        LEFT JOIN organization_memberships m ON m.account_id = a.id AND m.organization_id = u.organization_id
        WHERE a.id IS NULL OR m.id IS NULL OR m.role <> u.role OR m.status <> 'ACTIVE'
    ) THEN
        RAISE EXCEPTION 'Account and OrganizationMembership backfill validation failed';
    END IF;
    -- V1-V24 contain no commercial reference source. Do not invent contracts or group Organizations.
    SELECT count(*) INTO unresolved FROM organizations WHERE commercial_client_id IS NULL;
    RAISE NOTICE 'CommercialClient mapping unresolved for % Organizations; reconcile the approved mapping before cutover', unresolved;
END $$;
