package com.complyz.tenancy.service;

import com.complyz.tenancy.entity.Organization;
import com.complyz.users.entity.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantAccessService {
    public Organization requireOrganization(User user) {
        if (user == null || user.getOrganization() == null) {
            throw new IllegalStateException("The user is not assigned to an Organization.");
        }
        return user.getOrganization();
    }

    public void requireSameOrganization(User user, Organization organization) {
        if (requireOrganization(user).getId() == null
                || organization == null
                || !requireOrganization(user).getId().equals(organization.getId())) {
            throw new SecurityException("Access denied outside the user's Organization.");
        }
    }

    public UUID organizationId(User user) {
        return requireOrganization(user).getId();
    }
}
