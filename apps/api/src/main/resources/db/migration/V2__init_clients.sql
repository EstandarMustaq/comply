CREATE TABLE IF NOT EXISTS clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    id_document_type VARCHAR(50) NOT NULL, -- BI, NUIT, PASSPORT
    id_document_number VARCHAR(100) NOT NULL,
    nuib VARCHAR(50), -- Número Único de Identificação Bancária (Art. 138)
    risk_rating VARCHAR(50) NOT NULL DEFAULT 'LOW', -- LOW, MODERATE, HIGH
    risk_rating_updated_at DATE,
    due_diligence_regime VARCHAR(50) NOT NULL DEFAULT 'NORMAL', -- NORMAL, SIMPLIFIED (Art. 137)
    is_pep BOOLEAN NOT NULL DEFAULT FALSE,
    pep_category VARCHAR(100),
    pep_last_reviewed_at DATE,
    senior_management_approved BOOLEAN NOT NULL DEFAULT FALSE,
    kyc_complete BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_clients_doc_num ON clients(id_document_number);
CREATE INDEX idx_clients_regime ON clients(due_diligence_regime);
CREATE INDEX idx_clients_pep ON clients(is_pep, pep_last_reviewed_at);
CREATE INDEX idx_clients_risk ON clients(risk_rating);
