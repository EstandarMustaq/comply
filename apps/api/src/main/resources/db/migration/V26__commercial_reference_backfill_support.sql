-- Deployment input, separate from the four-field CommercialClient reference record.
-- Only the migration/operator principal may load a reviewed mapping.
CREATE TABLE platform_backoffice.commercial_reference_backfill (
    organization_id UUID PRIMARY KEY REFERENCES public.organizations(id),
    commercial_client_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL CHECK (btrim(name) <> ''),
    contract_reference VARCHAR(255) NOT NULL CHECK (btrim(contract_reference) <> ''),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    review_reference VARCHAR(255) NOT NULL CHECK (btrim(review_reference) <> ''),
    reviewed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_commercial_reference_backfill_client
    ON platform_backoffice.commercial_reference_backfill(commercial_client_id);

-- Stable operator report. Missing mappings remain visible until explicitly resolved.
CREATE VIEW platform_backoffice.unresolved_commercial_references AS
SELECT o.id AS organization_id,
       CASE WHEN b.organization_id IS NULL THEN 'MISSING_REVIEWED_MAPPING'
            ELSE 'PENDING_APPLICATION' END AS reason
FROM public.organizations o
LEFT JOIN platform_backoffice.commercial_reference_backfill b ON b.organization_id = o.id
WHERE o.commercial_client_id IS NULL;

-- Migration-only reconciliation, not a platform or customer authorization API.
-- Invoker privileges are preserved; PUBLIC execution is explicitly revoked below.
CREATE FUNCTION public.apply_commercial_reference_backfill()
RETURNS TABLE (organization_id UUID, commercial_client_id UUID, outcome TEXT)
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
DECLARE
    mapped_count BIGINT;
    unresolved_count BIGINT;
BEGIN
    -- Serialize reconciliation and prevent concurrent metadata/input changes.
    LOCK TABLE platform_backoffice.commercial_reference_backfill IN SHARE ROW EXCLUSIVE MODE;
    LOCK TABLE platform_backoffice.commercial_clients IN SHARE ROW EXCLUSIVE MODE;
    LOCK TABLE public.organizations IN SHARE ROW EXCLUSIVE MODE;

    IF EXISTS (
        SELECT 1 FROM platform_backoffice.commercial_reference_backfill b
        GROUP BY b.commercial_client_id
        HAVING count(DISTINCT (b.name, b.contract_reference, b.status)) > 1
    ) THEN
        RAISE EXCEPTION 'Conflicting CommercialClient metadata in reviewed mapping';
    END IF;

    IF EXISTS (
        SELECT 1 FROM platform_backoffice.commercial_reference_backfill b
        JOIN platform_backoffice.commercial_clients c ON c.id = b.commercial_client_id
        WHERE (c.name, c.contract_reference, c.status) IS DISTINCT FROM (b.name, b.contract_reference, b.status)
    ) THEN
        RAISE EXCEPTION 'Reviewed mapping conflicts with existing CommercialClient metadata';
    END IF;

    IF EXISTS (
        SELECT 1 FROM platform_backoffice.commercial_reference_backfill b
        JOIN public.organizations o ON o.id = b.organization_id
        WHERE o.commercial_client_id IS NOT NULL AND o.commercial_client_id <> b.commercial_client_id
    ) THEN
        RAISE EXCEPTION 'Reviewed mapping conflicts with an existing Organization commercial reference';
    END IF;

    INSERT INTO platform_backoffice.commercial_clients (id, name, contract_reference, status)
    SELECT DISTINCT b.commercial_client_id, b.name, b.contract_reference, b.status
    FROM platform_backoffice.commercial_reference_backfill b
    ON CONFLICT (id) DO NOTHING;

    UPDATE public.organizations o
    SET commercial_client_id = b.commercial_client_id, version = o.version + 1
    FROM platform_backoffice.commercial_reference_backfill b
    WHERE o.id = b.organization_id AND o.commercial_client_id IS NULL;
    GET DIAGNOSTICS mapped_count = ROW_COUNT;

    SELECT count(*) INTO unresolved_count FROM platform_backoffice.unresolved_commercial_references;
    RAISE NOTICE 'Commercial reference backfill: % Organizations mapped, % unresolved', mapped_count, unresolved_count;

    RETURN QUERY
    SELECT o.id, o.commercial_client_id,
           CASE WHEN o.commercial_client_id IS NULL THEN 'UNRESOLVED' ELSE 'MAPPED' END
    FROM public.organizations o
    ORDER BY o.id;
END $$;

REVOKE ALL ON FUNCTION public.apply_commercial_reference_backfill() FROM PUBLIC;
REVOKE ALL ON platform_backoffice.commercial_reference_backfill FROM PUBLIC;
REVOKE ALL ON platform_backoffice.unresolved_commercial_references FROM PUBLIC;
