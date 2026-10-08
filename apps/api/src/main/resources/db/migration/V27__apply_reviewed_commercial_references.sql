-- For staged deployments, migrate to V26, load reviewed input, then resume Flyway.
-- Fresh deployments with no input report unresolved Organizations explicitly.
-- Late input uses the same idempotent function under the migration/operator principal.
SELECT * FROM public.apply_commercial_reference_backfill();
