package com.complyz.tenancy.repository;

import com.complyz.tenancy.entity.OrganizationMembership;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Callers must obtain Organization identity from TenantAccessService before accessing memberships. */
public interface OrganizationMembershipRepository extends Repository<OrganizationMembership, UUID> {
    OrganizationMembership save(OrganizationMembership membership);
    Optional<OrganizationMembership> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<OrganizationMembership> findByAccountIdAndOrganizationId(UUID accountId, UUID organizationId);
    List<OrganizationMembership> findAllByOrganizationId(UUID organizationId);
}
