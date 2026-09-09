-- Product surfaces use internal rule labels; historical migrations remain immutable.
UPDATE threshold_rules
SET legal_basis = CASE
    WHEN legal_basis IS NULL OR legal_basis = '' THEN 'configured-rule'
    ELSE 'configured-rule'
END;

UPDATE system_config
SET value = regexp_replace(value::text, ',?[[:space:]]*"basis"[[:space:]]*:[[:space:]]*"[^"]*"', '', 'g')::jsonb
WHERE value::text LIKE '%"basis"%';
