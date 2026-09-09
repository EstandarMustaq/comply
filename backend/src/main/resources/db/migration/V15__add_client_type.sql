ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS client_type VARCHAR(30) NOT NULL DEFAULT 'INDIVIDUAL';

CREATE INDEX IF NOT EXISTS idx_clients_client_type ON clients(client_type);
