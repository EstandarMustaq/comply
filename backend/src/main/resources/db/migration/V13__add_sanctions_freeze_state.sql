ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS sanctions_frozen BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS sanctions_frozen_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS sanctions_freeze_reason TEXT;

CREATE INDEX IF NOT EXISTS idx_clients_sanctions_frozen
    ON clients(sanctions_frozen);
