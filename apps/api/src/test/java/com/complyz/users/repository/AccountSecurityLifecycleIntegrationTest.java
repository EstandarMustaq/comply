package com.complyz.users.repository;

import com.complyz.tenancy.entity.Organization;
import com.complyz.tenancy.entity.OrganizationMembership;
import com.complyz.tenancy.entity.OrganizationRole;
import com.complyz.tenancy.repository.OrganizationMembershipRepository;
import com.complyz.tenancy.repository.OrganizationRepository;
import com.complyz.users.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataJpaTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
class AccountSecurityLifecycleIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final Instant START = Instant.parse("2026-10-01T10:00:00Z");
    private static final Instant NOW = START.plusSeconds(60);

    /** Binds Spring persistence to the isolated PostgreSQL test container. */
    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private EntityManager entityManager;
    @Autowired private DataSource dataSource;
    @Autowired private AccountRepository accounts;
    @Autowired private AccountExternalIdentityRepository identities;
    @Autowired private AccountFactorReferenceRepository factors;
    @Autowired private AccountSessionRepository sessions;
    @Autowired private OrganizationRepository organizations;
    @Autowired private OrganizationMembershipRepository memberships;
    private JdbcTemplate jdbc;
    private Account account;
    private Account otherAccount;
    private AccountExternalIdentity identity;

    /** Creates two independent Accounts and one provider identity without Organization memberships. */
    @BeforeEach
    void createIdentityWithoutMembership() {
        jdbc = new JdbcTemplate(dataSource);
        account = accounts.save(new Account("Primary", "primary@example.test"));
        otherAccount = accounts.save(new Account("Other", "other@example.test"));
        identity = identities.save(new AccountExternalIdentity(account.getId(), "https://idp.example.test", "subject-1", START));
        entityManager.flush();
    }

    /** Verifies exact issuer/subject matching and permanent uniqueness after unlinking. */
    @Test
    void exactIssuerSubjectLinkIsUniqueAndNeverReassignedAfterUnlink() {
        assertThat(identities.findLinkedByIssuerAndSubject("https://idp.example.test", "subject-1", NOW)).isPresent();
        assertThat(identities.findLinkedByIssuerAndSubject("https://other.example.test", "subject-1", NOW)).isEmpty();
        assertThat(identities.findLinkedByIssuerAndSubject("https://idp.example.test", "Subject-1", NOW)).isEmpty();
        identities.save(new AccountExternalIdentity(otherAccount.getId(), "https://other.example.test", "subject-1", START));
        identity.unlink(NOW);
        entityManager.flush();
        assertThat(identities.findLinkedByIssuerAndSubject("https://idp.example.test", "subject-1", NOW)).isEmpty();
        rejected("INSERT INTO account_external_identities (id, account_id, issuer, subject, linked_at) VALUES (gen_random_uuid(), '"
                + otherAccount.getId() + "', 'https://idp.example.test', 'subject-1', CURRENT_TIMESTAMP)");
        assertThat(accounts.findById(account.getId())).isPresent();
    }

    /** Verifies session revocation is idempotent and preserves the independent identity, factor and membership lifecycles. */
    @Test
    void revokingOneSessionPreservesIdentityFactorsOtherSessionsAndMembership() {
        var organization = organizations.save(new Organization("lifecycle", "Lifecycle"));
        var membership = memberships.save(new OrganizationMembership(organization.getId(), account.getId(), OrganizationRole.OCOS));
        var factor = factors.save(new AccountFactorReference(account.getId(), identity.getId(), "idp-factor-1", START));
        var first = session("first", START.plusSeconds(300));
        var second = session("second", START.plusSeconds(300));
        first.revoke(NOW);
        first.revoke(NOW.plusSeconds(1));
        entityManager.flush();
        entityManager.clear();
        assertThat(sessions.findActiveByAccountId(account.getId(), NOW)).extracting(AccountSession::getId).containsExactly(second.getId());
        assertThat(sessions.findByIdAndAccountId(first.getId(), account.getId()))
                .hasValueSatisfying(s -> assertThat(s.getRevokedAt()).isEqualTo(NOW));
        assertThat(factors.findActiveByAccountId(account.getId(), NOW)).extracting(AccountFactorReference::getId).containsExactly(factor.getId());
        assertThat(identities.findLinkedByIssuerAndSubject("https://idp.example.test", "subject-1", NOW)).isPresent();
        assertThat(memberships.findByIdAndOrganizationId(membership.getId(), organization.getId()))
                .hasValueSatisfying(m -> assertThat(m.getVersion()).isZero());
    }

    /** Verifies active queries exclude revoked factors, expired sessions and future sessions independently. */
    @Test
    void factorRevocationAndSessionExpiryHaveIndependentBoundaries() {
        var revoked = factors.save(new AccountFactorReference(account.getId(), identity.getId(), "revoked-factor", START));
        var active = factors.save(new AccountFactorReference(account.getId(), identity.getId(), "active-factor", START));
        revoked.revoke(NOW);
        revoked.revoke(NOW.plusSeconds(1));
        var expiresNow = session("expires-now", NOW);
        var activeSession = session("active", NOW.plusSeconds(1));
        sessions.save(new AccountSession(account.getId(), identity.getId(), "future", NOW.plusSeconds(1), NOW.plusSeconds(5)));
        entityManager.flush();
        assertThat(factors.findActiveByAccountId(account.getId(), NOW)).extracting(AccountFactorReference::getId).containsExactly(active.getId());
        assertThat(revoked.getRevokedAt()).isEqualTo(NOW);
        assertThat(sessions.findActiveByAccountId(account.getId(), NOW)).extracting(AccountSession::getId).containsExactly(activeSession.getId());
        assertThat(sessions.findByIdAndAccountId(expiresNow.getId(), account.getId())).isPresent();
        assertThat(accounts.findById(account.getId())).isPresent();
    }

    /** Verifies unlinking excludes active factors and sessions while preserving their independent history. */
    @Test
    void unlinkImmediatelyExcludesDependentRecordsWithoutDeletingTheirIndependentHistory() {
        var factor = factors.save(new AccountFactorReference(account.getId(), identity.getId(), "factor", START));
        var session = session("session", NOW.plusSeconds(300));
        identity.unlink(NOW);
        identity.unlink(NOW.plusSeconds(1));
        entityManager.flush();
        assertThat(factors.findActiveByAccountId(account.getId(), NOW)).isEmpty();
        assertThat(sessions.findActiveByAccountId(account.getId(), NOW)).isEmpty();
        assertThat(identity.getUnlinkedAt()).isEqualTo(NOW);
        assertThat(factors.findByIdAndAccountId(factor.getId(), account.getId())).isPresent();
        assertThat(sessions.findByIdAndAccountId(session.getId(), account.getId())).isPresent();
        assertThat(accounts.findById(account.getId())).isPresent();
    }

    /** Verifies Account-scoped reads fail closed and database constraints prevent cross-Account links or destructive deletion. */
    @Test
    void foreignAccountAndMissingContextCannotReadOrAttachIdentityRecords() {
        var factor = factors.save(new AccountFactorReference(account.getId(), identity.getId(), "factor", START));
        var session = session("session", NOW.plusSeconds(300));
        entityManager.flush();
        assertThat(identities.findByIdAndAccountId(identity.getId(), otherAccount.getId())).isEmpty();
        assertThat(factors.findByIdAndAccountId(factor.getId(), otherAccount.getId())).isEmpty();
        assertThat(sessions.findByIdAndAccountId(session.getId(), otherAccount.getId())).isEmpty();
        assertThat(factors.findActiveByAccountId(null, NOW)).isEmpty();
        assertThat(sessions.findActiveByAccountId(null, NOW)).isEmpty();
        assertThat(sessions.findActiveByAccountId(otherAccount.getId(), NOW)).isEmpty();
        rejected("UPDATE account_sessions SET account_id = '" + otherAccount.getId() + "'");
        rejected("UPDATE account_factor_references SET account_id = '" + otherAccount.getId() + "'");
        rejected("UPDATE account_external_identities SET account_id = '" + otherAccount.getId() + "'");
        rejected("DELETE FROM account_external_identities");
        rejected("DELETE FROM accounts WHERE id = '" + account.getId() + "'");
    }

    /** Verifies lifecycle constraints, fingerprint-only session storage and exclusion of secrets from schema and JSON. */
    @Test
    void schemaRejectsInvalidLifecycleTimesDuplicateFactorsAndRawSessionIdentifiers() throws Exception {
        var factor = factors.save(new AccountFactorReference(account.getId(), identity.getId(), "provider-factor", START));
        var session = session("abc", NOW.plusSeconds(300));
        entityManager.flush();
        assertThat(jdbc.queryForObject("SELECT session_fingerprint FROM account_sessions WHERE id = ?", String.class, session.getId()))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        var json = new ObjectMapper().findAndRegisterModules().writeValueAsString(session);
        assertThat(json).doesNotContain("sessionFingerprint", "ba7816bf", "idpSessionId");
        rejected("UPDATE account_sessions SET session_fingerprint = 'raw-session-id'");
        rejected("UPDATE account_sessions SET expires_at = created_at");
        rejected("UPDATE account_sessions SET revoked_at = created_at - interval '1 second'");
        rejected("UPDATE account_factor_references SET revoked_at = registered_at - interval '1 second'");
        rejected("UPDATE account_external_identities SET unlinked_at = linked_at - interval '1 second'");
        rejected("INSERT INTO account_factor_references SELECT gen_random_uuid(), account_id, external_identity_id, provider_factor_id, registered_at, revoked_at, version FROM account_factor_references");
        rejected("INSERT INTO account_sessions SELECT gen_random_uuid(), account_id, external_identity_id, session_fingerprint, created_at, expires_at, revoked_at, version FROM account_sessions");
        assertThatThrownBy(() -> factor.revoke(START.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> session.revoke(START.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> identity.unlink(START.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AccountSession(account.getId(), identity.getId(), "session", NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_name IN ('account_external_identities', 'account_factor_references', 'account_sessions')", String.class))
                .doesNotContain("password", "secret", "token", "access_token", "refresh_token", "totp_secret", "recovery_code");
    }

    /** Verifies optimistic locking prevents a stale session update from undoing revocation. */
    @Test
    void staleWritesCannotUndoRevocation() {
        var session = session("session", NOW.plusSeconds(300));
        entityManager.flush();
        entityManager.detach(session);
        var current = sessions.findByIdAndAccountId(session.getId(), account.getId()).orElseThrow();
        current.revoke(NOW);
        entityManager.flush();
        assertThat(current.getVersion()).isEqualTo(1);
        entityManager.clear();
        assertThatThrownBy(() -> entityManager.merge(session)).isInstanceOf(OptimisticLockException.class);
    }

    /** Persists synthetic session metadata for the primary fixture Account and external identity. */
    private AccountSession session(String providerId, Instant expiry) {
        return sessions.save(new AccountSession(account.getId(), identity.getId(), providerId, START, expiry));
    }

    /** Asserts a database integrity rejection and rolls back to a savepoint so the test transaction remains usable. */
    private void rejected(String sql) {
        jdbc.execute("SAVEPOINT invalid_input");
        try {
            assertThatThrownBy(() -> jdbc.update(sql)).isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbc.execute("ROLLBACK TO SAVEPOINT invalid_input");
            jdbc.execute("RELEASE SAVEPOINT invalid_input");
        }
    }
}
