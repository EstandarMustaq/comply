package com.comply.tenancy.service;

import com.comply.tenancy.entity.Organization;
import com.comply.users.entity.User;
import java.util.UUID;
import org.springframework.stereotype.Service;

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
