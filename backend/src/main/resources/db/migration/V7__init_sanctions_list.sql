CREATE TABLE IF NOT EXISTS sanctions_list_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    entity_type VARCHAR(50) NOT NULL, -- INDIVIDUAL, ORGANIZATION
    source_list VARCHAR(100) NOT NULL, -- LISTA_DESIGNADA_NACIONAL, UN_CONSOLIDATED
    raw_reference VARCHAR(255),
    imported_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sanctions_name ON sanctions_list_entries(full_name);
CREATE INDEX idx_sanctions_source ON sanctions_list_entries(source_list);
