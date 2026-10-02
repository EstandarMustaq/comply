ALTER TABLE threshold_rules DROP COLUMN IF EXISTS legal_basis;

UPDATE system_config
SET value = value - 'basis'
WHERE value ? 'basis';
