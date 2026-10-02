package com.complyz.api;

import java.util.Map;

public final class ApiErrorBodies {

    private ApiErrorBodies() {}

    public static Map<String, String> notFound() {
        return Map.of("message", "O registo pedido não foi encontrado.");
    }

    public static Map<String, String> invalidExportState() {
        return Map.of("message", "O relatório tem de estar submetido antes de ser exportado.");
    }

    public static Map<String, String> invalidExportPeriod() {
        return Map.of("message", "O ano indicado não é válido para esta exportação.");
    }
}
