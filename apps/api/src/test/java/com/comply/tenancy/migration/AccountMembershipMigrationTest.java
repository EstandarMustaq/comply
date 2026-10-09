package com.comply.tenancy.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class AccountMembershipMigrationTest {
  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine");

  @BeforeEach
  void resetDatabase() throws SQLException {
    execute(
        "DROP SCHEMA IF EXISTS platform_backoffice CASCADE; DROP SCHEMA public CASCADE; CREATE SCHEMA public");
  }

  @Test
  void emptySchemaHasNoInventedAccountsOrCommercialContractsAndRerunIsANoOp() throws SQLException {
    assertThat(flyway(null).migrate().migrationsExecuted).isEqualTo(28);
    assertThat(count("accounts")).isZero();
    assertThat(count("organization_memberships")).isZero();
    assertThat(count("platform_backoffice.commercial_clients")).isZero();
    assertThat(number("SELECT count(*) FROM organizations WHERE commercial_client_id IS NULL"))
        .isEqualTo(1);
    assertThat(flyway(null).migrate().migrationsExecuted).isZero();
    flyway(null).validate();
  }

  @Test
  void populatedLegacyDatabasePreservesEveryIdentityRoleCredentialAndTenant() throws SQLException {
    flyway("24").migrate();
    seedLegacyUsers();
    execute(
        """
                INSERT INTO clients (id, full_name, id_document_type, id_document_number, organization_id)
                VALUES ('00000000-0000-0000-0000-000000000201', 'Legacy client', 'BI', 'LEGACY-1',
                        '00000000-0000-0000-0000-000000000101');
                INSERT INTO audit_log (actor_id, action, entity, entity_id)
                VALUES ('00000000-0000-0000-0000-000000000010', 'LOGIN', 'User',
                        '00000000-0000-0000-0000-000000000010');
                CREATE TABLE legacy_users_snapshot AS SELECT * FROM users;
                """);
    assertThat(count("users")).isEqualTo(4);
    assertThat(flyway(null).migrate().migrationsExecuted).isEqualTo(4);
    assertThat(count("accounts")).isEqualTo(4);
    assertThat(count("account_external_identities")).isZero();
    assertThat(count("account_factor_references")).isZero();
    assertThat(count("account_sessions")).isZero();
    assertThat(count("organization_memberships")).isEqualTo(4);
    assertThat(count("users")).isEqualTo(4);
    assertThat(count("clients")).isEqualTo(1);
    assertThat(count("audit_log")).isEqualTo(1);
    assertThat(
            number(
                "SELECT count(*) FROM ((SELECT * FROM users EXCEPT SELECT * FROM legacy_users_snapshot) "
                    + "UNION ALL (SELECT * FROM legacy_users_snapshot EXCEPT SELECT * FROM users)) differences"))
        .isZero();
    assertThat(
            number(
                """
                SELECT count(*) FROM users u
                JOIN accounts a ON a.id = u.id AND a.legacy_user_id = u.id
                  AND a.email = u.email AND a.full_name = u.full_name AND a.created_at = u.created_at AND a.version = 0
                JOIN organization_memberships m ON m.id = u.id AND m.account_id = a.id
                  AND m.organization_id = u.organization_id AND m.role = u.role
                  AND m.status = 'ACTIVE' AND m.created_at = u.created_at AND m.version = 0
                """))
        .isEqualTo(4);
    assertThat(number("SELECT count(*) FROM organization_memberships WHERE role = 'OWNER'"))
        .isZero();
    assertThat(number("SELECT count(*) FROM organizations WHERE commercial_client_id IS NULL"))
        .isEqualTo(3);
    assertThat(count("platform_backoffice.commercial_clients")).isZero();
    assertThat(flyway(null).migrate().migrationsExecuted).isZero();
    assertThat(count("accounts")).isEqualTo(4);
    assertThat(count("organization_memberships")).isEqualTo(4);
  }

  @Test
  void invalidLegacyRoleRollsBackExpansionAndCanBeCorrectedBeforeRetry() throws SQLException {
    flyway("24").migrate();
    seedLegacyUsers();
    execute(
        "ALTER TABLE users DROP CONSTRAINT ck_users_role; UPDATE users SET role = 'UNREVIEWED'");
    assertThatThrownBy(() -> flyway(null).migrate()).isInstanceOf(FlywayException.class);
    assertThat(
            number("SELECT count(*) FROM information_schema.tables WHERE table_name = 'accounts'"))
        .isZero();
    assertThat(
            number(
                "SELECT count(*) FROM information_schema.schemata WHERE schema_name = 'platform_backoffice'"))
        .isZero();
    assertThat(number("SELECT count(*) FROM flyway_schema_history WHERE version = '25'")).isZero();
    assertThat(count("users")).isEqualTo(4);
    execute("UPDATE users SET role = 'AUDITOR_READONLY'");
    assertThat(flyway(null).migrate().migrationsExecuted).isEqualTo(4);
    assertThat(
            number("SELECT count(*) FROM organization_memberships WHERE role = 'AUDITOR_READONLY'"))
        .isEqualTo(4);
  }

  @Test
  void referencesUniquenessAndRequiredFieldsAreEnforcedWithoutCascadingLegacyDeletion()
      throws SQLException {
    flyway("24").migrate();
    seedLegacyUsers();
    flyway(null).migrate();
    rejected(
        "INSERT INTO accounts (id, full_name, email) SELECT gen_random_uuid(), full_name, email FROM accounts LIMIT 1",
        "23505");
    rejected(
        "INSERT INTO accounts (id, full_name, email, legacy_user_id) VALUES (gen_random_uuid(), 'A', 'a@test', gen_random_uuid())",
        "23514");
    rejected(
        "INSERT INTO organization_memberships SELECT gen_random_uuid(), organization_id, account_id, role, status, created_at, version FROM organization_memberships LIMIT 1",
        "23505");
    rejected("UPDATE organization_memberships SET account_id = gen_random_uuid()", "23503");
    rejected("UPDATE organization_memberships SET organization_id = gen_random_uuid()", "23503");
    rejected("UPDATE organization_memberships SET role = 'PLATFORM_ADMIN'", "23514");
    rejected("UPDATE organization_memberships SET status = 'UNKNOWN'", "23514");
    rejected("UPDATE organization_memberships SET organization_id = NULL", "23502");
    rejected("UPDATE accounts SET version = -1", "23514");
    rejected("DELETE FROM users", "23503");
    rejected("DELETE FROM accounts", "23503");
    rejected("UPDATE organizations SET commercial_client_id = gen_random_uuid()", "23503");
    rejected(
        "INSERT INTO platform_backoffice.commercial_clients VALUES (gen_random_uuid(), 'Name', ' ', 'ACTIVE')",
        "23514");
    rejected(
        "INSERT INTO platform_backoffice.commercial_clients VALUES (gen_random_uuid(), 'Name', 'Contract', 'SUSPENDED')",
        "23514");
    execute(
        """
                INSERT INTO platform_backoffice.commercial_clients VALUES
                  ('00000000-0000-0000-0000-000000000301', 'Reference', 'APPROVED-1', 'INACTIVE');
                UPDATE organizations SET commercial_client_id = '00000000-0000-0000-0000-000000000301';
                """);
    rejected("DELETE FROM platform_backoffice.commercial_clients", "23503");
    assertThat(number("SELECT count(*) FROM organization_memberships WHERE status = 'ACTIVE'"))
        .isEqualTo(4);
  }

  private void seedLegacyUsers() throws SQLException {
    execute(
        """
                INSERT INTO organizations (id, slug, name) VALUES
                  ('00000000-0000-0000-0000-000000000101', 'legacy-a', 'Legacy A'),
                  ('00000000-0000-0000-0000-000000000102', 'legacy-b', 'Legacy B');
                """);
    var roles = List.of("ADMIN", "COMPLIANCE_OFFICER", "OCOS", "AUDITOR_READONLY");
    // Distinct legacy identities with case-variant emails must never be silently merged.
    var emails =
        List.of(
            "Person@example.test",
            "person@example.test",
            "ocos@example.test",
            "audit@example.test");
    try (var connection = connection();
        var insert =
            connection.prepareStatement(
                """
                INSERT INTO users (id, full_name, email, password_hash, role, mfa_enabled, mfa_secret_encrypted,
                    mfa_backup_codes_hash, created_at, last_login_at, organization_id)
                VALUES (CAST(? AS uuid), 'Legacy user', ?, 'synthetic-password-hash', ?, true, 'synthetic-ciphertext',
                    ARRAY['synthetic-backup-hash'], '2024-01-02T03:04:05Z', '2025-01-02T03:04:05Z', CAST(? AS uuid))
                """)) {
      for (int i = 0; i < roles.size(); i++) {
        insert.setString(1, "00000000-0000-0000-0000-00000000001" + i);
        insert.setString(2, emails.get(i));
        insert.setString(3, roles.get(i));
        insert.setString(4, "00000000-0000-0000-0000-00000000010" + (i % 2 + 1));
        insert.executeUpdate();
      }
    }
  }

  private Flyway flyway(String target) {
    var configuration =
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    if (target != null) configuration.target(target);
    return configuration.load();
  }

  private Connection connection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private void execute(String sql) throws SQLException {
    try (var connection = connection();
        var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private long count(String table) throws SQLException {
    return number("SELECT count(*) FROM " + table);
  }

  private long number(String sql) throws SQLException {
    try (var connection = connection();
        var statement = connection.createStatement();
        var rows = statement.executeQuery(sql)) {
      rows.next();
      return rows.getLong(1);
    }
  }

  private void rejected(String sql, String sqlState) {
    assertThatThrownBy(() -> execute(sql))
        .isInstanceOfSatisfying(
            SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo(sqlState));
  }
}
