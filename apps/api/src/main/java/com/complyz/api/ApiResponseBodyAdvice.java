package com.complyz.api;

import com.complyz.config.ApiVersioningConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ControllerAdvice
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final String GENERIC_ERROR_MESSAGE = "O pedido não pôde ser concluído.";

    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)
                || !(response instanceof ServletServerHttpResponse servletResponse)
                || !servletRequest.getServletRequest().getRequestURI()
                        .startsWith(ApiVersioningConfiguration.API_BASE_PATH + "/")
                || body == null
                || body instanceof byte[]
                || body instanceof ApiSuccessEnvelope<?>
                || body instanceof ApiErrorEnvelope
                || !MediaType.APPLICATION_JSON.isCompatibleWith(selectedContentType)) {
            return body;
        }

        HttpServletResponse httpResponse = servletResponse.getServletResponse();
        ApiResponseMetadata meta = new ApiResponseMetadata(Instant.now(), UUID.randomUUID().toString());
        httpResponse.setHeader("X-Request-Id", meta.requestId());

        if (httpResponse.getStatus() >= 400) {
            return new ApiErrorEnvelope(
                    new ApiError(
                            "HTTP_" + httpResponse.getStatus(),
                            errorMessage(body),
                            errorDetails(body)
                    ),
                    meta
            );
        }
        return new ApiSuccessEnvelope<>(body, meta);
    }

    private String errorMessage(Object body) {
        if (body instanceof Map<?, ?> values) {
            Object message = values.get("message");
            if (message instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return GENERIC_ERROR_MESSAGE;
    }

    private Map<String, Object> errorDetails(Object body) {
        if (!(body instanceof Map<?, ?> values)) {
            return Map.of();
        }
        Map<String, Object> details = new HashMap<>();
        values.forEach((key, value) -> {
            if (key instanceof String name && !"message".equals(name) && value != null) {
                details.put(name, value);
            }
        });
        return Map.copyOf(details);
    }
}
