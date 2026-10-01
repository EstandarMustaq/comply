ALTER TABLE import_batches
    ADD COLUMN IF NOT EXISTS source_file_path VARCHAR(1024);

CREATE INDEX IF NOT EXISTS idx_import_batches_source_file_path
    ON import_batches(source_file_path);
