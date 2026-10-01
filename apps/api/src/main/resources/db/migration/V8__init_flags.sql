CREATE TABLE IF NOT EXISTS flags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID REFERENCES transactions(id) ON DELETE SET NULL,
    client_id UUID REFERENCES clients(id) ON DELETE SET NULL,
    type VARCHAR(50) NOT NULL, -- KYC_INCOMPLETE, THRESHOLD_VALUE, STRUCTURING_SUSPECTED, SANCTIONS_MATCH, PEP_REVIEW_DUE
    reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, IN_REVIEW, ESCALATED_TO_STR, RESOLVED
    sla_deadline TIMESTAMP WITH TIME ZONE, -- 24 horas para SANCTIONS_MATCH (Lei 15/2023, Art. 38-40)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    review_note TEXT
);

CREATE INDEX idx_flags_status ON flags(status);
CREATE INDEX idx_flags_sla ON flags(sla_deadline);
CREATE INDEX idx_flags_type ON flags(type);
CREATE INDEX idx_flags_client ON flags(client_id);
