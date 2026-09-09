CREATE TABLE IF NOT EXISTS sanctions_list_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version VARCHAR(80) NOT NULL UNIQUE,
    source_list VARCHAR(100) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    source_uri VARCHAR(500) NOT NULL,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE sanctions_list_entries ADD COLUMN IF NOT EXISTS version_id UUID;

INSERT INTO sanctions_list_versions
    (id, version, source_list, checksum, source_uri, valid_from, status)
SELECT
    '00000000-0000-0000-0000-000000000019',
    'legacy-v1',
    'LISTA_DESIGNADA_NACIONAL',
    'legacy-data-not-verifiable',
    'classpath:/sanctions_nacional_seed.json',
    CURRENT_TIMESTAMP,
    'ACTIVE'
WHERE EXISTS (SELECT 1 FROM sanctions_list_entries)
  AND NOT EXISTS (SELECT 1 FROM sanctions_list_versions);

UPDATE sanctions_list_entries
SET version_id = '00000000-0000-0000-0000-000000000019'
WHERE version_id IS NULL
  AND EXISTS (
      SELECT 1 FROM sanctions_list_versions
      WHERE id = '00000000-0000-0000-0000-000000000019'
  );

ALTER TABLE sanctions_list_entries
    ADD CONSTRAINT fk_sanctions_entry_version
    FOREIGN KEY (version_id) REFERENCES sanctions_list_versions(id);

CREATE INDEX IF NOT EXISTS idx_sanctions_entry_version
    ON sanctions_list_entries(version_id);
CREATE INDEX IF NOT EXISTS idx_sanctions_version_status
    ON sanctions_list_versions(status, source_list);
