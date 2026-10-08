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
    private static final String META_SCHEMA = "ApiResponseMetadata";
    private static final String ERROR_SCHEMA = "ApiError";
    private static final String ERROR_ENVELOPE_SCHEMA = "ApiErrorEnvelope";
    private static final String API_BASE_PATH = ApiVersioningConfiguration.API_BASE_PATH;
    private static final Map<String, String> RESOURCE_TAGS = Map.of(
            "/auth", "Authentication",
            "/clients", "Customers",
            "/flags", "Alerts",
            "/import-batches", "Imports",
            "/insights", "Insights",
            "/reports", "Reports",
            "/settings/users", "Users",
            "/settings", "Settings",
            "/audit-log", "Activity history"
    );

    @Bean
    public OpenAPI complyzOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Complyz API")
                        .version("1.0")
                        .description("API for Complyz's financial compliance workspace."))
                .servers(List.of(new Server()
                        .url("https://api.trycomplyz.com")
                        .description("Production")))
                .components(new Components()
                        .addSecuritySchemes(API_KEY, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSchemas(META_SCHEMA, metadataSchema())
                        .addSchemas(ERROR_SCHEMA, errorSchema())
                        .addSchemas(ERROR_ENVELOPE_SCHEMA, errorEnvelopeSchema()))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY));
    }

    @Bean
    public OpenApiCustomizer publicApiResponseEnvelopeCustomizer() {
        return openApi -> {
            ensureEnvelopeSchemas(openApi);
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> {
                if (!path.startsWith(API_BASE_PATH + "/")) {
                    return;
                }
                String resourcePath = path.substring(API_BASE_PATH.length());
                pathItem.readOperations().forEach(operation -> {
                    boolean publicAuthenticationOperation = resourcePath.matches(
                            "^/auth/(login|mfa/verify|mfa/enroll|mfa/confirm|refresh)$"
                    );
                    if (publicAuthenticationOperation) {
                        operation.setSecurity(List.of());
                    }
                    operation.setTags(List.of(resourceTag(resourcePath)));
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
        envelope.addProperty("data", dataSchema);
        envelope.addProperty("meta", new Schema<>().$ref("#/components/schemas/" + META_SCHEMA));
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
                        .description("The service returned an error.")
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

    private void ensureEnvelopeSchemas(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        Components components = openApi.getComponents();
        components.addSchemas(META_SCHEMA, metadataSchema());
        components.addSchemas(ERROR_SCHEMA, errorSchema());
        components.addSchemas(ERROR_ENVELOPE_SCHEMA, errorEnvelopeSchema());
    }

    private Schema<?> metadataSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("timestamp", new StringSchema().format("date-time"));
        schema.addProperty("requestId", new StringSchema().format("uuid"));
        schema.setRequired(List.of("timestamp", "requestId"));
        return schema;
    }

    private Schema<?> errorSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("code", new StringSchema());
        schema.addProperty("message", new StringSchema());
        schema.addProperty("details", new ObjectSchema().additionalProperties(true));
        schema.setRequired(List.of("code", "message", "details"));
        return schema;
    }

    private Schema<?> errorEnvelopeSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("error", new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA));
        schema.addProperty("meta", new Schema<>().$ref("#/components/schemas/" + META_SCHEMA));
        schema.setRequired(List.of("error", "meta"));
        return schema;
    }
}
