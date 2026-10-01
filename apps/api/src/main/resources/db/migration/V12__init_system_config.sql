CREATE TABLE IF NOT EXISTS system_config (
    key VARCHAR(100) PRIMARY KEY,
    value JSONB NOT NULL,
    updated_by UUID REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Configurações padrão iniciais alinhadas ao Aviso 10/GBM/2024 e Leis 14/2023 e 15/2023
INSERT INTO system_config (key, value) VALUES
('diligence.simplified_monthly_limit', '{"value": 30000.00, "currency": "MZN", "basis": "Art. 137 Aviso 10/GBM/2024"}'),
('diligence.simplified_annual_limit', '{"value": 240000.00, "currency": "MZN", "basis": "Art. 137 Aviso 10/GBM/2024"}'),
('retention.identity_years', '{"years": 10, "basis": "Art. 86 Aviso 10/GBM/2024"}'),
('retention.transactions_years', '{"years": 10, "basis": "Art. 88 Aviso 10/GBM/2024"}'),
('retention.reports_years', '{"years": 10, "basis": "Art. 89 Aviso 10/GBM/2024"}'),
('sanctions.sla_hours', '{"hours": 24, "basis": "Art. 38-40 Lei 15/2023"}'),
('pep.review_interval_days', '{"days": 365, "basis": "Art. 66 Aviso 10/GBM/2024"}');
