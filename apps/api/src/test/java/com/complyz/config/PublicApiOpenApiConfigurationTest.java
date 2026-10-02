package com.complyz.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PublicApiOpenApiConfigurationTest {

    private final PublicApiOpenApiConfiguration configuration = new PublicApiOpenApiConfiguration();

    @Test
    void configuresCanonicalApiServerAndBearerAuthentication() {
        OpenAPI openAPI = configuration.complyzOpenAPI();

        assertEquals("https://api.trycomplyz.com", openAPI.getServers().getFirst().getUrl());
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get("bearerAuth"));
        assertNotNull(openAPI.getComponents().getSchemas().get("ApiErrorEnvelope"));
        assertNotNull(openAPI.getComponents().getSchemas().get("ApiResponseMeta"));
    }

    @Test
    void documentsEnvelopesAndPublicLoginSecurity() {
        OpenAPI openAPI = configuration.complyzOpenAPI();
        Operation login = operationWithJsonResponse("/api/auth/login", 200, 401);
        Operation secured = operationWithJsonResponse("/api/clients", 200);
        secured.getResponses().get("200").getContent().remove("application/json");
        secured.getResponses().get("200").getContent().addMediaType(
                "*/*",
                new MediaType().schema(new StringSchema())
        );
        openAPI.path("/api/auth/login", new PathItem().post(login));
        openAPI.path("/api/clients", new PathItem().get(secured));
        configuration.publicApiResponseEnvelopeCustomizer().customise(openAPI);

        var loginSchema = login.getResponses().get("200").getContent().get("application/json").getSchema();
        assertTrue(loginSchema.getProperties().containsKey("data"));
        assertTrue(loginSchema.getProperties().containsKey("meta"));
        assertTrue(login.getSecurity().isEmpty());
        assertEquals("ApiErrorEnvelope", login.getResponses().get("401").getContent()
                .get("application/json").getSchema().get$ref().replace("#/components/schemas/", ""));
        assertEquals("ApiErrorEnvelope", login.getResponses().get("default").getContent()
                .get("application/json").getSchema().get$ref().replace("#/components/schemas/", ""));
        assertNotNull(openAPI.getSecurity());
        assertNull(secured.getSecurity());
        assertFalse(secured.getResponses().get("200").getContent().containsKey("*/*"));
        assertTrue(secured.getResponses().get("200").getContent().get("application/json")
                .getSchema().getProperties().containsKey("data"));
        assertEquals("Clientes", secured.getTags().getFirst());
    }

    private Operation operationWithJsonResponse(String path, int... statuses) {
        ApiResponses responses = new ApiResponses();
        for (int status : statuses) {
            responses.addApiResponse(String.valueOf(status), new ApiResponse()
                    .content(new Content().addMediaType(
                            "application/json",
                            new MediaType().schema(new StringSchema())
                    )));
        }
        return new Operation().responses(responses);
    }
}
