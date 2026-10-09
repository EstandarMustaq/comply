package com.comply.api;

import java.util.Map;

public final class ApiErrorBodies {

  private ApiErrorBodies() {}

  public static Map<String, String> notFound() {
    return Map.of("message", "The requested record was not found.");
  }

  public static Map<String, String> invalidExportState() {
    return Map.of("message", "File the report before exporting it.");
  }

  public static Map<String, String> invalidExportPeriod() {
    return Map.of("message", "Enter a valid year for this export.");
  }
}
