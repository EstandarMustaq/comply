-- Remove deprecated metadata from rules and configuration.
ALTER TABLE threshold_rules DROP COLUMN IF EXISTS legal_basis;

UPDATE system_config
SET value = value - 'basis'
WHERE value ? 'basis';

UPDATE flags
SET reason = regexp_replace(reason, '[[:space:]]+Regra:.*$', '', 'i')
WHERE reason ~* '[[:space:]]+Regra:';
