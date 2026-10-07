package com.complyz.ingest.controller;


import com.complyz.ingest.service.ImportService;
import com.complyz.tenancy.entity.Organization;

import com.complyz.users.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportControllerTenantScopeTest {

    @Test
    void listAndDetailUseTheAuthenticatedOrganization() {
        ImportService importService = mock(ImportService.class);
        Organization organization = new Organization("actor-org", "Actor organization");
        organization.setId(UUID.randomUUID());
        User actor = new User();
        actor.setOrganization(organization);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(actor);
        when(importService.listBatches(actor)).thenReturn(List.of());
        UUID foreignBatchId = UUID.randomUUID();
        when(importService.findBatch(foreignBatchId, actor)).thenReturn(Optional.empty());
        ImportController controller = new ImportController(importService);

        assertThat(controller.listBatches(authentication).getBody()).isEmpty();
        assertThat(controller.getBatch(foreignBatchId, authentication).getStatusCode().value()).isEqualTo(404);

        verify(importService).listBatches(actor);
        verify(importService).findBatch(foreignBatchId, actor);
    }
}
