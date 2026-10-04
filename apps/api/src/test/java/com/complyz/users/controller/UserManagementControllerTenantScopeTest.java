package com.complyz.users.controller;

import com.complyz.audit.service.AuditService;
import com.complyz.tenancy.entity.Organization;
import com.complyz.tenancy.service.TenantAccessService;
import com.complyz.users.entity.Role;
import com.complyz.users.entity.User;
import com.complyz.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;


import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserManagementControllerTenantScopeTest {

    @Test
    void userListIsLoadedByAuthenticatedOrganizationInPersistence() {
        UserRepository userRepository = mock(UserRepository.class);

        AuditService auditService = mock(AuditService.class);
        Organization organization = new Organization("actor-org", "Actor organization");
        organization.setId(UUID.randomUUID());
        User actor = new User();
        actor.setOrganization(organization);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(actor);
        when(userRepository.findAllByOrganizationId(organization.getId())).thenReturn(List.of());
        UserManagementController controller = new UserManagementController(
                userRepository, auditService, new TenantAccessService());

        var response = controller.listUsers(authentication);

        assertThat(response.getBody()).isEmpty();
        verify(userRepository).findAllByOrganizationId(organization.getId());
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void directUserCreationDoesNotRevealWhetherAnEmailExists() {
        UserRepository userRepository = mock(UserRepository.class);
        AuditService auditService = mock(AuditService.class);
        UserManagementController controller = new UserManagementController(
                userRepository, auditService, new TenantAccessService());

        var existingEmailResponse = controller.createUser(new UserManagementController.CreateUserRequest(
                "A Utilizadora", "existing@other-organization.test", "password", Role.ADMIN));
        var newEmailResponse = controller.createUser(new UserManagementController.CreateUserRequest(
                "A Utilizadora", "new@other-organization.test", "password", Role.ADMIN));

        assertThat(existingEmailResponse.getStatusCode().value()).isEqualTo(501);
        assertThat(newEmailResponse.getStatusCode()).isEqualTo(existingEmailResponse.getStatusCode());
        assertThat(newEmailResponse.getBody()).isEqualTo(existingEmailResponse.getBody());
        verifyNoInteractions(userRepository, auditService);
    }
}
