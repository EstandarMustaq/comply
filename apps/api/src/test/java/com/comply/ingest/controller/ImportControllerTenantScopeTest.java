package com.comply.ingest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.comply.ingest.service.ImportService;
import com.comply.tenancy.entity.Organization;
import com.comply.users.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ImportControllerTenantScopeTest {

  @Test
  void uploadAcceptsMultipartFileAndRequiresTheFilePart() throws Exception {
    ImportService service = mock(ImportService.class);
    var mvc = MockMvcBuilders.standaloneSetup(new ImportController(service)).build();
    User actor = new User();
    var authentication = new TestingAuthenticationToken(actor, null);
    var file =
        new MockMultipartFile("file", "transactions.csv", "text/csv", "amount\n10".getBytes());

    mvc.perform(multipart("/import-batches").file(file).principal(authentication))
        .andExpect(status().isOk());
    verify(service).createUploadBatch(file, actor);
    mvc.perform(multipart("/import-batches").principal(authentication))
        .andExpect(status().isBadRequest());
  }

  @Test
  void uploadRejectsJsonInsteadOfAFileUpload() throws Exception {
    ImportService service = mock(ImportService.class);
    var mvc = MockMvcBuilders.standaloneSetup(new ImportController(service)).build();

    mvc.perform(post("/import-batches").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnsupportedMediaType());
    verifyNoInteractions(service);
  }

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
    assertThat(controller.getBatch(foreignBatchId, authentication).getStatusCode().value())
        .isEqualTo(404);

    verify(importService).listBatches(actor);
    verify(importService).findBatch(foreignBatchId, actor);
  }
}
