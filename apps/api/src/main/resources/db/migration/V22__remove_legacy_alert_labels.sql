UPDATE flags
SET reason = regexp_replace(reason, '[[:space:]]+Regra:.*$', '', 'i')
WHERE reason ~* '[[:space:]]+Regra:';
