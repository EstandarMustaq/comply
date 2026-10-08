package com.complyz.tenancy.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class CommercialReferenceBackfillMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final String DEFAULT_ORG = "00000000-0000-0000-0000-000000000001";
    private static final String ORG_A = "00000000-0000-0000-0000-000000000101";
    private static final String ORG_B = "00000000-0000-0000-0000-000000000102";
    private static final String REFERENCE_A = "00000000-0000-0000-0000-000000000201";
    private static final String REFERENCE_B = "00000000-0000-0000-0000-000000000202";

    @BeforeEach
    void stageLegacyDatabase() throws SQLException {
        execute("DROP SCHEMA IF EXISTS platform_backoffice CASCADE; DROP SCHEMA public CASCADE; CREATE SCHEMA public");
        flyway("24").migrate();
        execute("""
                INSERT INTO organizations (id, slug, name) VALUES
                  ('00000000-0000-0000-0000-000000000101', 'org-a', 'Organization A'),
                  ('00000000-0000-0000-0000-000000000102', 'org-b', 'Organization B');
                INSERT INTO users (full_name, email, password_hash, role, organization_id) VALUES
                  ('Member A', 'a@example.test', 'synthetic-hash', 'ADMIN', '00000000-0000-0000-0000-000000000101'),
                  ('Member B', 'b@example.test', 'synthetic-hash', 'AUDITOR_READONLY', '00000000-0000-0000-0000-000000000102');
                """);
        flyway("26").migrate();
    }

    @Test
    void reviewedMappingCreatesReferencesLinksEveryOrganizationAndPreservesIdentityAndAuthorizationData() throws SQLException {
        stage(DEFAULT_ORG, REFERENCE_A, "Reference A");
        stage(ORG_A, REFERENCE_A, "Reference A");
        stage(ORG_B, REFERENCE_B, "Reference B");
        execute("""
                CREATE TABLE membership_snapshot AS SELECT * FROM organization_memberships;
                CREATE TABLE account_snapshot AS SELECT * FROM accounts;
                CREATE TABLE user_snapshot AS SELECT * FROM users;
                """);
        assertThat(flyway(null).migrate().migrationsExecuted).isEqualTo(2);
        assertThat(number("SELECT count(*) FROM platform_backoffice.commercial_clients")).isEqualTo(2);
        assertThat(number("SELECT count(*) FROM organizations WHERE commercial_client_id IS NOT NULL AND version = 1")).isEqualTo(3);
        assertThat(number("SELECT count(*) FROM organizations WHERE commercial_client_id = '" + REFERENCE_A + "'")).isEqualTo(2);
        assertThat(number("SELECT count(*) FROM platform_backoffice.unresolved_commercial_references")).isZero();
        assertThat(number("SELECT count(*) FROM information_schema.columns WHERE table_schema = 'platform_backoffice' AND table_name = 'commercial_clients'")).isEqualTo(4);
        unchanged("organization_memberships", "membership_snapshot");
        unchanged("accounts", "account_snapshot");
        unchanged("users", "user_snapshot");
        assertThat(flyway(null).migrate().migrationsExecuted).isZero();
        assertThat(number("SELECT count(*) FROM public.apply_commercial_reference_backfill() WHERE outcome = 'MAPPED'")).isEqualTo(3);
        assertThat(number("SELECT sum(version) FROM organizations")).isEqualTo(3);
        assertThat(number("SELECT count(*) FROM platform_backoffice.commercial_clients")).isEqualTo(2);
    }

    @Test
    void partialMappingReportsExactPendingOrganizationsAndLateInputCompletesWithoutNewImplementation() throws SQLException {
        stage(ORG_A, REFERENCE_A, "Reference A");
        assertThat(number("SELECT count(*) FROM platform_backoffice.unresolved_commercial_references WHERE reason = 'PENDING_APPLICATION'")).isEqualTo(1);
        flyway(null).migrate();
        assertThat(number("SELECT count(*) FROM platform_backoffice.unresolved_commercial_references WHERE reason = 'MISSING_REVIEWED_MAPPING'")).isEqualTo(2);
        assertThat(number("SELECT count(*) FROM platform_backoffice.unresolved_commercial_references WHERE organization_id IN ('" + DEFAULT_ORG + "', '" + ORG_B + "')")).isEqualTo(2);
        stage(DEFAULT_ORG, REFERENCE_A, "Reference A");
        stage(ORG_B, REFERENCE_B, "Reference B");
        assertThat(number("SELECT count(*) FROM public.apply_commercial_reference_backfill() WHERE outcome = 'MAPPED'")).isEqualTo(3);
        assertThat(number("SELECT count(*) FROM platform_backoffice.unresolved_commercial_references")).isZero();
        assertThat(number("SELECT sum(version) FROM organizations")).isEqualTo(3);
    }

    @Test
    void conflictingMetadataRollsBackMigrationAndNormalRetryAppliesCorrectedInput() throws SQLException {
        stage(ORG_A, REFERENCE_A, "Reference A");
        stage(ORG_B, REFERENCE_A, "Conflicting name");
        assertThatThrownBy(() -> flyway(null).migrate()).isInstanceOf(FlywayException.class)
                .hasStackTraceContaining("Conflicting CommercialClient metadata");
        assertThat(number("SELECT count(*) FROM platform_backoffice.commercial_clients")).isZero();
        assertThat(number("SELECT count(*) FROM organizations WHERE commercial_client_id IS NOT NULL")).isZero();
        assertThat(number("SELECT count(*) FROM flyway_schema_history WHERE version = '27'")).isZero();
        execute("UPDATE platform_backoffice.commercial_reference_backfill SET name = 'Reference A' WHERE organization_id = '" + ORG_B + "'");
        assertThat(flyway(null).migrate().migrationsExecuted).isEqualTo(2);
        assertThat(number("SELECT count(*) FROM organizations WHERE commercial_client_id = '" + REFERENCE_A + "'")).isEqualTo(2);
    }

    @Test
    void existingCommercialMetadataCannotBeSilentlyOverwritten() throws SQLException {
        execute("INSERT INTO platform_backoffice.commercial_clients VALUES ('" + REFERENCE_A + "', 'Existing name', 'CONTRACT', 'INACTIVE')");
        stage(ORG_A, REFERENCE_A, "Replacement name");
        assertThatThrownBy(() -> flyway(null).migrate()).isInstanceOf(FlywayException.class)
                .hasStackTraceContaining("conflicts with existing CommercialClient metadata");
        assertThat(number("SELECT count(*) FROM platform_backoffice.commercial_clients WHERE name = 'Existing name' AND status = 'INACTIVE'")).isEqualTo(1);
        assertThat(number("SELECT count(*) FROM organizations WHERE commercial_client_id IS NOT NULL")).isZero();
    }

    @Test
    void existingOrganizationReferenceCannotBeReassignedByBackfill() throws SQLException {
        execute("INSERT INTO platform_backoffice.commercial_clients VALUES ('" + REFERENCE_B + "', 'Existing', 'CONTRACT', 'ACTIVE')");
        execute("UPDATE organizations SET commercial_client_id = '" + REFERENCE_B + "' WHERE id = '" + ORG_A + "'");
        stage(ORG_A, REFERENCE_A, "Reference A");
        assertThatThrownBy(() -> flyway(null).migrate()).isInstanceOf(FlywayException.class)
                .hasStackTraceContaining("conflicts with an existing Organization commercial reference");
        assertThat(number("SELECT count(*) FROM organizations WHERE id = '" + ORG_A + "' AND commercial_client_id = '" + REFERENCE_B + "'")).isEqualTo(1);
        assertThat(number("SELECT count(*) FROM platform_backoffice.commercial_clients WHERE id = '" + REFERENCE_A + "'")).isZero();
    }

    @Test
    void failureAfterReferenceInsertionRollsBackAllWritesAndAllowsRetry() throws SQLException {
        stage(ORG_A, REFERENCE_A, "Reference A");
        execute("""
                CREATE FUNCTION public.reject_reference_update() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'Synthetic deployment failure'; END $$;
                CREATE TRIGGER reject_reference_update BEFORE UPDATE ON organizations
                FOR EACH ROW EXECUTE FUNCTION public.reject_reference_update();
                """);
        assertThatThrownBy(() -> flyway(null).migrate()).isInstanceOf(FlywayException.class);
        assertThat(number("SELECT count(*) FROM platform_backoffice.commercial_clients")).isZero();
        assertThat(number("SELECT sum(version) FROM organizations")).isZero();
        execute("DROP TRIGGER reject_reference_update ON organizations; DROP FUNCTION public.reject_reference_update()");
        assertThat(flyway(null).migrate().migrationsExecuted).isEqualTo(2);
        assertThat(number("SELECT count(*) FROM platform_backoffice.commercial_clients")).isEqualTo(1);
    }

    @Test
    void invalidUnreviewedDuplicateAndUnknownOrganizationInputIsRejectedAndPublicCannotExecute() throws SQLException {
        stage(ORG_A, REFERENCE_A, "Reference A");
        assertThatThrownBy(() -> stage(ORG_A, REFERENCE_B, "Reference B"))
                .isInstanceOfSatisfying(SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo("23505"));
        assertThatThrownBy(() -> stage("00000000-0000-0000-0000-000000000999", REFERENCE_A, "Reference A"))
                .isInstanceOfSatisfying(SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo("23503"));
        rejected("UPDATE platform_backoffice.commercial_reference_backfill SET review_reference = ''", "23514");
        rejected("UPDATE platform_backoffice.commercial_reference_backfill SET reviewed_at = NULL", "23502");
        rejected("UPDATE platform_backoffice.commercial_reference_backfill SET status = 'INVALID'", "23514");
        execute("CREATE ROLE unprivileged_operator");
        assertThat(number("SELECT count(*) WHERE has_function_privilege('unprivileged_operator', 'public.apply_commercial_reference_backfill()', 'EXECUTE')")).isZero();
        try (var connection = connection(); var statement = connection.createStatement()) {
            statement.execute("SET ROLE unprivileged_operator");
            assertThatThrownBy(() -> statement.execute("SELECT * FROM public.apply_commercial_reference_backfill()"))
                    .isInstanceOfSatisfying(SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo("42501"));
        }
    }

    @Test
    void concurrentRetriesDoNotDuplicateReferencesOrAdvanceVersionsTwice() throws Exception {
        stage(ORG_A, REFERENCE_A, "Reference A");
        stage(ORG_B, REFERENCE_B, "Reference B");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> number("SELECT count(*) FROM public.apply_commercial_reference_backfill() WHERE outcome = 'MAPPED'"));
            var second = executor.submit(() -> number("SELECT count(*) FROM public.apply_commercial_reference_backfill() WHERE outcome = 'MAPPED'"));
            assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo(2);
            assertThat(second.get(15, TimeUnit.SECONDS)).isEqualTo(2);
        }
        assertThat(number("SELECT count(*) FROM platform_backoffice.commercial_clients")).isEqualTo(2);
        assertThat(number("SELECT sum(version) FROM organizations")).isEqualTo(2);
    }

    private void stage(String organizationId, String referenceId, String name) throws SQLException {
        try (var connection = connection(); var statement = connection.prepareStatement("""
                INSERT INTO platform_backoffice.commercial_reference_backfill
                  (organization_id, commercial_client_id, name, contract_reference, status, review_reference, reviewed_at)
                VALUES (CAST(? AS uuid), CAST(? AS uuid), ?, 'CONTRACT', 'ACTIVE', 'REVIEW-0003', '2026-10-01T00:00:00Z')
                """)) {
            statement.setString(1, organizationId);
            statement.setString(2, referenceId);
            statement.setString(3, name);
            statement.executeUpdate();
        }
    }

    private Flyway flyway(String target) {
        var configuration = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        if (target != null) configuration.target(target);
        return configuration.load();
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private void execute(String sql) throws SQLException {
        try (var connection = connection(); var statement = connection.createStatement()) { statement.execute(sql); }
    }

    private long number(String sql) throws SQLException {
        try (var connection = connection(); var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private void unchanged(String actual, String snapshot) throws SQLException {
        assertThat(number("SELECT count(*) FROM ((SELECT * FROM " + actual + " EXCEPT SELECT * FROM " + snapshot
                + ") UNION ALL (SELECT * FROM " + snapshot + " EXCEPT SELECT * FROM " + actual + ")) differences")).isZero();
    }

    private void rejected(String sql, String sqlState) {
        assertThatThrownBy(() -> execute(sql)).isInstanceOfSatisfying(SQLException.class,
                failure -> assertThat(failure.getSQLState()).isEqualTo(sqlState));
    }
}
