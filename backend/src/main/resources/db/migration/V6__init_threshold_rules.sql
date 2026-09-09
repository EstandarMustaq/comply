CREATE TABLE IF NOT EXISTS threshold_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- SINGLE_TRANSACTION_VALUE, MONTHLY_AGGREGATE_VALUE, STRUCTURING
    value_threshold NUMERIC(18,2),
    aggregation_window_days INT DEFAULT 30,
    legal_basis VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Regras padrão da legislação moçambicana
INSERT INTO threshold_rules (id, name, type, value_threshold, aggregation_window_days, legal_basis, active)
VALUES 
(
    '11111111-1111-1111-1111-111111111111',
    'Limiar de Transacção em Numerário / Operação Relevante',
    'SINGLE_TRANSACTION_VALUE',
    250000.00,
    1,
    'Art. 44(3) da Lei n.º 14/2023',
    TRUE
),
(
    '22222222-2222-2222-2222-222222222222',
    'Agregação Mensal de Operações em Numerário',
    'MONTHLY_AGGREGATE_VALUE',
    750000.00,
    30,
    'Art. 129(2) do Aviso n.º 10/GBM/2024',
    TRUE
),
(
    '33333333-3333-3333-3333-333333333333',
    'Detecção de Fraccionamento de Operações (Structuring)',
    'STRUCTURING',
    100000.00,
    30,
    'Art. 82 do Aviso n.º 10/GBM/2024',
    TRUE
);
