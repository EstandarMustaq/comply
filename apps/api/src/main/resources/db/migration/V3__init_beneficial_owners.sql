CREATE TABLE IF NOT EXISTS beneficial_owners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL, -- FOUNDER, ADMINISTRATOR, TRUSTEE, BENEFICIARY, CONTROLLING_PERSON (Art. 36.3)
    ownership_percentage NUMERIC(5,2), -- ≥10% triggers mandatory identification (Art. 51.1.e)
    verified_at DATE
);

CREATE INDEX idx_bo_client ON beneficial_owners(client_id);
