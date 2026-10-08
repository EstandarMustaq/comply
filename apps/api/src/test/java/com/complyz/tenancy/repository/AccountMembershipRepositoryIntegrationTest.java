package com.complyz.tenancy.repository;

import com.complyz.tenancy.backoffice.CommercialClient;
import com.complyz.tenancy.backoffice.CommercialClientRepository;
import com.complyz.tenancy.backoffice.CommercialClientStatus;
import com.complyz.tenancy.entity.Organization;
import com.complyz.tenancy.entity.OrganizationMembership;
import com.complyz.tenancy.entity.OrganizationRole;
import com.complyz.tenancy.service.TenantAccessService;
import com.complyz.users.entity.Account;
import com.complyz.users.entity.Role;
import com.complyz.users.entity.User;
import com.complyz.users.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataJpaTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
class AccountMembershipRepositoryIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private EntityManager entityManager;
    @Autowired private AccountRepository accounts;
    @Autowired private OrganizationRepository organizations;
    @Autowired private OrganizationMembershipRepository memberships;
    @Autowired private CommercialClientRepository commercialClients;

    @Test
    void accountMayHaveNoMembershipOrDifferentRolesAcrossCommercialReferences() {
        var account = accounts.save(new Account("Member", "member@example.test"));
        var independentAccount = accounts.save(new Account("Profile only", "profile@example.test"));
        var organizationA = organization("a", CommercialClientStatus.ACTIVE);
        var organizationB = organization("b", CommercialClientStatus.INACTIVE);
        var membershipA = memberships.save(new OrganizationMembership(organizationA.getId(), account.getId(), OrganizationRole.ADMIN));
        var membershipB = memberships.save(new OrganizationMembership(organizationB.getId(), account.getId(), OrganizationRole.AUDITOR_READONLY));
        entityManager.flush();
        entityManager.clear();

        assertThat(accounts.findById(independentAccount.getId())).isPresent();
        assertThat(memberships.findByAccountIdAndOrganizationId(independentAccount.getId(), organizationA.getId())).isEmpty();
        assertThat(accounts.findById(account.getId())).hasValueSatisfying(loaded -> {
            assertThat(loaded.getLegacyUserId()).isNull();
            assertThat(loaded.getVersion()).isZero();
        });
        assertThat(memberships.findAllByOrganizationId(organizationA.getId()))
                .extracting(OrganizationMembership::getId).containsExactly(membershipA.getId());
        assertThat(memberships.findByIdAndOrganizationId(membershipB.getId(), organizationA.getId())).isEmpty();
        assertThat(memberships.findByIdAndOrganizationId(membershipA.getId(), null)).isEmpty();
        assertThat(memberships.findByAccountIdAndOrganizationId(account.getId(), organizationB.getId()))
                .hasValueSatisfying(loaded -> assertThat(loaded.getRole()).isEqualTo(OrganizationRole.AUDITOR_READONLY));
        assertThat(memberships.findAllByOrganizationId(UUID.randomUUID())).isEmpty();
        assertThat(commercialClients.findById(organizationB.getCommercialClientId()))
                .hasValueSatisfying(loaded -> assertThat(loaded.getStatus()).isEqualTo(CommercialClientStatus.INACTIVE));
    }

    @Test
    void commercialReferenceDoesNotLeakThroughLegacyJsonOrChangeTenantAuthorization() throws Exception {
        var organizationA = organization("a", CommercialClientStatus.INACTIVE);
        var organizationB = organizations.save(new Organization("b", "B"));
        organizationB.setCommercialClientId(organizationA.getCommercialClientId());
        var actor = new User("Actor", "actor@example.test", "synthetic-hash", Role.ADMIN);
        actor.setOrganization(organizationA);
        var access = new TenantAccessService();
        assertThat(access.organizationId(actor)).isEqualTo(organizationA.getId());
        assertThatThrownBy(() -> access.requireSameOrganization(actor, organizationB)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> access.organizationId(null)).isInstanceOf(IllegalStateException.class);
        var json = new ObjectMapper().findAndRegisterModules().readTree(
                new ObjectMapper().findAndRegisterModules().writeValueAsString(actor));
        assertThat(json.get("organization").has("commercialClientId")).isFalse();
        assertThat(json.get("organization").has("version")).isFalse();
    }

    @Test
    void organizationVersionAdvancesAndStaleUpdatesAreRejected() {
        var organization = organizations.save(new Organization("versioned", "Versioned"));
        entityManager.flush();
        assertThat(organization.getVersion()).isZero();
        entityManager.detach(organization);
        var current = entityManager.find(Organization.class, organization.getId());
        var reference = commercialClients.save(new CommercialClient("Reference", "CONTRACT", CommercialClientStatus.ACTIVE));
        current.setCommercialClientId(reference.getId());
        entityManager.flush();
        assertThat(current.getVersion()).isEqualTo(1);
        entityManager.clear();
        assertThatThrownBy(() -> entityManager.merge(organization)).isInstanceOf(OptimisticLockException.class);
    }

    private Organization organization(String slug, CommercialClientStatus status) {
        var reference = commercialClients.save(new CommercialClient("Reference " + slug, "CONTRACT-" + slug, status));
        var organization = new Organization(slug, "Organization " + slug);
        organization.setCommercialClientId(reference.getId());
        return organizations.save(organization);
    }
}
