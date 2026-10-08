package com.complyz.tenancy.repository;

import com.complyz.tenancy.entity.OrganizationMembership;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Callers must obtain Organization identity from TenantAccessService before accessing memberships. */
public interface OrganizationMembershipRepository extends Repository<OrganizationMembership, UUID> {
    /**
     * Persists a membership; callers must establish Organization scope before writing.
     * @param membership membership within an authorized Organization
     * @return the saved entity; use this instance for subsequent changes
     */
    OrganizationMembership save(OrganizationMembership membership);
    /**
     * Finds a membership only within the Organization obtained through TenantAccessService.
     * @param id persistent record UUID
     * @param organizationId Organization UUID defining the membership scope
     * @return the matching record, or empty when no record matches the query
     */
    Optional<OrganizationMembership> findByIdAndOrganizationId(UUID id, UUID organizationId);
    /**
     * Finds an Account's membership within the Organization obtained through TenantAccessService.
     * @param accountId target Account UUID within the authorized Organization scope
     * @param organizationId Organization UUID defining the membership scope
     * @return the matching record, or empty when no record matches the query
     */
    Optional<OrganizationMembership> findByAccountIdAndOrganizationId(UUID accountId, UUID organizationId);
    /**
     * Lists memberships of the Organization obtained through TenantAccessService, including inactive memberships.
     * @param organizationId Organization UUID defining the membership scope
     * @return matching records, or an empty list
     */
    List<OrganizationMembership> findAllByOrganizationId(UUID organizationId);
}
