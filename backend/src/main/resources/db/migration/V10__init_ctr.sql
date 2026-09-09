CREATE TABLE IF NOT EXISTS currency_transaction_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients(id),
    aggregation_period_start DATE NOT NULL,
    aggregation_period_end DATE NOT NULL,
    aggregate_value NUMERIC(18,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'MZN',
    threshold_rule_id UUID REFERENCES threshold_rules(id),
    submitted_at TIMESTAMP WITH TIME ZONE,
    retention_until DATE -- 10 anos a partir da elaboração/submissão
);

CREATE INDEX idx_ctr_client ON currency_transaction_reports(client_id);
CREATE INDEX idx_ctr_period ON currency_transaction_reports(aggregation_period_start, aggregation_period_end);
