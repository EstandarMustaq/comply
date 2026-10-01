CREATE TABLE IF NOT EXISTS suspicious_activity_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_id UUID REFERENCES flags(id) ON DELETE SET NULL,
    reporting_institution VARCHAR(255) NOT NULL,
    account_number VARCHAR(100),
    account_holder VARCHAR(255) NOT NULL,
    transaction_executor VARCHAR(255),
    monetary_value NUMERIC(18,2),
    suspicion_description TEXT NOT NULL,
    business_relationship_description TEXT NOT NULL,
    internal_employee_involved BOOLEAN NOT NULL DEFAULT FALSE,
    voluntary_statement TEXT,
    ocos_name VARCHAR(255) NOT NULL,
    gifim_registration_number VARCHAR(100),
    submitted_at TIMESTAMP WITH TIME ZONE,
    retention_until DATE -- submetido + 10 anos (Art. 89 do Aviso 10/GBM/2024)
);

CREATE INDEX idx_str_flag ON suspicious_activity_reports(flag_id);
CREATE INDEX idx_str_submitted ON suspicious_activity_reports(submitted_at);
CREATE INDEX idx_str_retention ON suspicious_activity_reports(retention_until);
