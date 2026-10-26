package com.comply.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.comply.clients.controller.ClientController;
import com.comply.flags.controller.FlagController;
import com.comply.ingest.controller.ImportController;
import com.comply.reporting.controller.ReportController;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class RbacPolicyTest {

  @Test
  void clientMutationsRequireOperationalComplianceRoles() throws Exception {
    assertPolicy(
        ClientController.class,
        "updateRiskRating",
        "hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')");
    assertPolicy(
        ClientController.class,
        "registerPepReview",
        "hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')");
    assertPolicy(
        ClientController.class,
        "createClient",
        "hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')");
  }

  @Test
  void flagReviewAndEscalationHaveSeparateSegregationOfDuties() throws Exception {
    assertPolicy(
        FlagController.class, "reviewFlag", "hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')");
    assertPolicy(FlagController.class, "escalateToStr", "hasRole('OCOS')");
  }

  @Test
  void reportingAndImportMutationsAreRestricted() throws Exception {
    assertPolicy(ReportController.class, "updateStr", "hasAnyRole('ADMIN', 'OCOS')");
    assertPolicy(ReportController.class, "submitStr", "hasRole('OCOS')");
    assertPolicy(ReportController.class, "submitCtr", "hasRole('OCOS')");
    assertPolicy(
        ImportController.class, "uploadBatch", "hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')");
    assertPolicy(
        ImportController.class,
        "confirmMapping",
        "hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')");
  }

  private void assertPolicy(Class<?> controller, String methodName, String expectedPolicy)
      throws Exception {
    Method method =
        java.util.Arrays.stream(controller.getDeclaredMethods())
            .filter(candidate -> candidate.getName().equals(methodName))
            .findFirst()
            .orElseThrow();

    PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
    assertEquals(expectedPolicy, annotation.value(), controller.getSimpleName() + "." + methodName);
  }
}
