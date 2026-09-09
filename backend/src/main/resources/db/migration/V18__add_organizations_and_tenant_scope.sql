CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY,
    slug VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO organizations (id, slug, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'default', 'Default Organization')
ON CONFLICT (id) DO NOTHING;

ALTER TABLE users ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE clients ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE import_batches ADD COLUMN IF NOT EXISTS organization_id UUID;

UPDATE users SET organization_id = '00000000-0000-0000-0000-000000000001'
WHERE organization_id IS NULL;
UPDATE clients SET organization_id = '00000000-0000-0000-0000-000000000001'
WHERE organization_id IS NULL;
UPDATE import_batches SET organization_id = '00000000-0000-0000-0000-000000000001'
WHERE organization_id IS NULL;

ALTER TABLE users ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE clients ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE import_batches ALTER COLUMN organization_id SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);
ALTER TABLE clients
    ADD CONSTRAINT fk_clients_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);
ALTER TABLE import_batches
    ADD CONSTRAINT fk_import_batches_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);

CREATE INDEX IF NOT EXISTS idx_users_organization ON users(organization_id);
CREATE INDEX IF NOT EXISTS idx_clients_organization ON clients(organization_id);
CREATE INDEX IF NOT EXISTS idx_import_batches_organization ON import_batches(organization_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_clients_org_document
    ON clients(organization_id, id_document_number);
