package com.comply.api;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiResponseBodyAdviceTest {

  private final ApiResponseBodyAdvice advice = new ApiResponseBodyAdvice();

  @Test
  void wrapsSuccessfulJsonApiResponsesWithDataAndMetadata() {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/api/v1/clients");
    MockHttpServletResponse servletResponse = new MockHttpServletResponse();
    Object result =
        advice.beforeBodyWrite(
            Map.of("items", 2),
            (MethodParameter) null,
            MediaType.APPLICATION_JSON,
            JacksonJsonHttpMessageConverter.class,
            new ServletServerHttpRequest(servletRequest),
            new ServletServerHttpResponse(servletResponse));

    assertInstanceOf(ApiSuccessEnvelope.class, result);
    ApiSuccessEnvelope<?> envelope = (ApiSuccessEnvelope<?>) result;
    assertEquals(Map.of("items", 2), envelope.data());
    assertNotNull(envelope.meta().timestamp());
    assertNotNull(envelope.meta().requestId());
    assertEquals(envelope.meta().requestId(), servletResponse.getHeader("X-Request-Id"));
  }

  @Test
  void convertsJsonErrorBodiesToSafeStructuredErrors() {
    MockHttpServletRequest servletRequest =
        new MockHttpServletRequest("POST", "/api/v1/auth/login");
    MockHttpServletResponse servletResponse = new MockHttpServletResponse();
    servletResponse.setStatus(401);
    Object result =
        advice.beforeBodyWrite(
            Map.of("message", "Email or password is incorrect"),
            (MethodParameter) null,
            MediaType.APPLICATION_JSON,
            JacksonJsonHttpMessageConverter.class,
            new ServletServerHttpRequest(servletRequest),
            new ServletServerHttpResponse(servletResponse));

    assertInstanceOf(ApiErrorEnvelope.class, result);
    ApiErrorEnvelope envelope = (ApiErrorEnvelope) result;
    assertEquals("HTTP_401", envelope.error().code());
    assertEquals("Email or password is incorrect", envelope.error().message());
    assertEquals(Map.of(), envelope.error().details());
  }

  @Test
  void preservesStructuredErrorDetails() {
    MockHttpServletRequest servletRequest =
        new MockHttpServletRequest("POST", "/api/v1/import-batches");
    MockHttpServletResponse servletResponse = new MockHttpServletResponse();
    servletResponse.setStatus(400);
    Object result =
        advice.beforeBodyWrite(
            Map.of("message", "File is invalid", "fieldErrors", Map.of("amount", "is required")),
            (MethodParameter) null,
            MediaType.APPLICATION_JSON,
            JacksonJsonHttpMessageConverter.class,
            new ServletServerHttpRequest(servletRequest),
            new ServletServerHttpResponse(servletResponse));

    ApiErrorEnvelope envelope = (ApiErrorEnvelope) result;
    assertEquals(
        Map.of("fieldErrors", Map.of("amount", "is required")), envelope.error().details());
  }

  @Test
  void leavesNonApiAndBinaryResponsesUnchanged() {
    MockHttpServletRequest apiRequest =
        new MockHttpServletRequest("GET", "/api/v1/reports/exports/file.pdf");
    MockHttpServletResponse servletResponse = new MockHttpServletResponse();
    byte[] content = {1, 2, 3};
    Object binary =
        advice.beforeBodyWrite(
            content,
            (MethodParameter) null,
            MediaType.APPLICATION_OCTET_STREAM,
            ByteArrayHttpMessageConverter.class,
            new ServletServerHttpRequest(apiRequest),
            new ServletServerHttpResponse(servletResponse));
    assertSame(content, binary);

    HttpServletRequest unversionedApiRequest = new MockHttpServletRequest("GET", "/api/clients");
    Object unversioned =
        advice.beforeBodyWrite(
            Map.of("items", 2),
            (MethodParameter) null,
            MediaType.APPLICATION_JSON,
            JacksonJsonHttpMessageConverter.class,
            new ServletServerHttpRequest(unversionedApiRequest),
            new ServletServerHttpResponse(servletResponse));
    assertEquals(Map.of("items", 2), unversioned);

    HttpServletRequest nonApiRequest = new MockHttpServletRequest("GET", "/actuator/health");
    Object unchanged =
        advice.beforeBodyWrite(
            Map.of("status", "UP"),
            (MethodParameter) null,
            MediaType.APPLICATION_JSON,
            JacksonJsonHttpMessageConverter.class,
            new ServletServerHttpRequest(nonApiRequest),
            new ServletServerHttpResponse(servletResponse));
    assertEquals(Map.of("status", "UP"), unchanged);
  }
}
