package com.complyz.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class PublicApiOpenApiConfiguration {

    private static final String API_KEY = "bearerAuth";
    private static final String META_SCHEMA = "ApiResponseMeta";
    private static final String ERROR_SCHEMA = "ApiError";
    private static final String ERROR_ENVELOPE_SCHEMA = "ApiErrorEnvelope";
    private static final Map<String, String> RESOURCE_TAGS = Map.of(
            "/api/auth", "Autenticação",
            "/api/clients", "Clientes",
            "/api/flags", "Alertas",
            "/api/import-batches", "Importações",
            "/api/insights", "Indicadores",
            "/api/reports", "Relatórios",
            "/api/settings/users", "Utilizadores",
            "/api/settings", "Definições",
            "/api/audit-log", "Histórico de actividade"
    );

    @Bean
    public OpenAPI complyzOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Complyz")
                        .version("1.0")
                        .description("API para integração com a plataforma Complyz."))
                .servers(List.of(new Server()
                        .url("https://api.trycomplyz.com")
                        .description("Produção")))
                .components(new Components()
                        .addSecuritySchemes(API_KEY, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSchemas(META_SCHEMA, metaSchema())
                        .addSchemas(ERROR_SCHEMA, errorSchema())
                        .addSchemas(ERROR_ENVELOPE_SCHEMA, errorEnvelopeSchema()))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY));
    }

    @Bean
    public OpenApiCustomizer publicApiResponseEnvelopeCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> {
                if (!path.startsWith("/api/")) {
                    return;
                }
                pathItem.readOperations().forEach(operation -> {
                    boolean publicAuthenticationOperation = path.matches(
                            "^/api/auth/(login|mfa/verify|mfa/enroll|mfa/confirm|refresh)$"
                    );
                    if (publicAuthenticationOperation) {
                        operation.setSecurity(List.of());
                    }
                    operation.setTags(List.of(resourceTag(path)));
                    operation.getResponses().forEach((status, response) -> {
                        if (isSuccess(status)) {
                            wrapSuccessResponse(response);
                        } else if (isError(status)) {
                            wrapErrorResponse(response);
                        }
                    });
                    addDefaultErrorResponse(operation.getResponses());
                });
            });
        };
    }

    private String resourceTag(String path) {
        return RESOURCE_TAGS.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .max(Map.Entry.comparingByKey((left, right) -> Integer.compare(left.length(), right.length())))
                .map(Map.Entry::getValue)
                .orElse("Complyz");
    }

    private boolean isSuccess(String status) {
        return status.matches("2[0-9]{2}");
    }

    private boolean isError(String status) {
        return status.matches("[45][0-9]{2}") || "default".equals(status);
    }

    private void wrapSuccessResponse(ApiResponse response) {
        if (response.getContent() == null) {
            return;
        }
        var json = response.getContent().get("application/json");
        if (json == null) {
            json = response.getContent().get("*/*");
            if (json == null) {
                return;
            }
            response.getContent().remove("*/*");
            response.getContent().addMediaType("application/json", json);
        }
        if (json.getSchema() == null) {
            return;
        }
        Schema<?> dataSchema = json.getSchema();
        ObjectSchema envelope = new ObjectSchema();
        envelope.addProperties("data", dataSchema);
        envelope.addProperties("meta", new Schema<>().$ref("#/components/schemas/" + META_SCHEMA));
        envelope.setRequired(List.of("data", "meta"));
        json.setSchema(envelope);
    }

    private void addDefaultErrorResponse(ApiResponses responses) {
        if (responses.containsKey("default")) {
            wrapErrorResponse(responses.get("default"));
            return;
        }
        responses.addApiResponse(
                "default",
                new ApiResponse()
                        .description("Erro devolvido pelo serviço.")
                        .content(new io.swagger.v3.oas.models.media.Content().addMediaType(
                                "application/json",
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(new Schema<>().$ref("#/components/schemas/" + ERROR_ENVELOPE_SCHEMA))
                        ))
        );
    }

    private void wrapErrorResponse(ApiResponse response) {
        if (response.getContent() == null) {
            response.setContent(new io.swagger.v3.oas.models.media.Content());
        }
        var json = response.getContent().get("application/json");
        if (json == null) {
            json = new io.swagger.v3.oas.models.media.MediaType();
            response.getContent().addMediaType("application/json", json);
        }
        json.setSchema(new Schema<>().$ref("#/components/schemas/" + ERROR_ENVELOPE_SCHEMA));
    }

    private Schema<?> metaSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperties("timestamp", new StringSchema().format("date-time"));
        schema.addProperties("requestId", new StringSchema().format("uuid"));
        schema.setRequired(List.of("timestamp", "requestId"));
        return schema;
    }

    private Schema<?> errorSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperties("code", new StringSchema());
        schema.addProperties("message", new StringSchema());
        schema.addProperties("details", new ObjectSchema().additionalProperties(true));
        schema.setRequired(List.of("code", "message", "details"));
        return schema;
    }

    private Schema<?> errorEnvelopeSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperties("error", new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA));
        schema.addProperties("meta", new Schema<>().$ref("#/components/schemas/" + META_SCHEMA));
        schema.setRequired(List.of("error", "meta"));
        return schema;
    }
}
