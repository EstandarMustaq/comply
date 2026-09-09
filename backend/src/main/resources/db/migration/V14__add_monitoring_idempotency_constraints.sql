CREATE UNIQUE INDEX IF NOT EXISTS uq_flags_transaction_type
    ON flags(transaction_id, type)
    WHERE transaction_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_ctr_client_period_rule
    ON currency_transaction_reports(client_id, aggregation_period_start, aggregation_period_end, threshold_rule_id)
    WHERE threshold_rule_id IS NOT NULL;
