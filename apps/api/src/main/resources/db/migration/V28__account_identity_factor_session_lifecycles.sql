-- Identity & Access records have independent lifecycles; no provider credentials are copied.
CREATE TABLE account_external_identities (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id),
    issuer VARCHAR(255) NOT NULL CHECK (issuer = btrim(issuer) AND issuer <> ''),
    subject VARCHAR(255) NOT NULL CHECK (subject = btrim(subject) AND subject <> ''),
    linked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    unlinked_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT uq_external_identity_issuer_subject UNIQUE (issuer, subject),
    CONSTRAINT uq_external_identity_account UNIQUE (id, account_id),
    CONSTRAINT ck_external_identity_unlink_time CHECK (unlinked_at IS NULL OR unlinked_at >= linked_at)
);
CREATE INDEX idx_external_identity_account ON account_external_identities(account_id);

-- Provider-owned enrollment/recovery material stays at the IdP. Store only its non-secret reference.
CREATE TABLE account_factor_references (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    external_identity_id UUID NOT NULL,
    provider_factor_id VARCHAR(255) NOT NULL CHECK (provider_factor_id = btrim(provider_factor_id) AND provider_factor_id <> ''),
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT fk_factor_identity_account FOREIGN KEY (external_identity_id, account_id)
        REFERENCES account_external_identities(id, account_id),
    CONSTRAINT uq_factor_identity_provider UNIQUE (external_identity_id, provider_factor_id),
    CONSTRAINT ck_factor_revocation_time CHECK (revoked_at IS NULL OR revoked_at >= registered_at)
);
CREATE INDEX idx_factor_account ON account_factor_references(account_id);

-- This is session lifecycle metadata, not a bearer credential/token store.
CREATE TABLE account_sessions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    external_identity_id UUID NOT NULL,
    session_fingerprint VARCHAR(64) NOT NULL CHECK (session_fingerprint ~ '^[0-9a-f]{64}$'),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT fk_session_identity_account FOREIGN KEY (external_identity_id, account_id)
        REFERENCES account_external_identities(id, account_id),
    CONSTRAINT uq_session_identity_fingerprint UNIQUE (external_identity_id, session_fingerprint),
    CONSTRAINT ck_session_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_session_revocation_time CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);
CREATE INDEX idx_session_account_expiry ON account_sessions(account_id, expires_at);
