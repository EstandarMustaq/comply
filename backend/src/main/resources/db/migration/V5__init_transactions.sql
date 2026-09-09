CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients(id),
    import_batch_id UUID REFERENCES import_batches(id),
    amount NUMERIC(18,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'MZN',
    transaction_date DATE NOT NULL,
    type VARCHAR(50) NOT NULL, -- DEPOSIT, WITHDRAWAL, TRANSFER, CREDIT
    counterparty_details JSONB, -- Reconstituição da operação (Art. 88.e)
    retention_until DATE NOT NULL, -- Prazo de 10 anos (Art. 88)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_client ON transactions(client_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
CREATE INDEX idx_transactions_client_date ON transactions(client_id, transaction_date);
CREATE INDEX idx_transactions_retention ON transactions(retention_until);
