package org.garethjevans.chained.auth.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

class AuthenticationRequiredFilterTest {

  private static final String TEST_HOST = "resource.example.com";
  private static final String EXPECTED_METADATA_URL =
      "http://resource.example.com/.well-known/oauth-protected-resource";

  @Test
  void testFilterRejectsRequestWithNoAuthorizationHeader() throws Exception {
    // Create a mock request without Authorization header
    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/mcp/test");
    mockRequest.setServerName(TEST_HOST);

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    // Apply the filter
    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        AuthenticationRequiredFilter.requireAuthentication();

    ServerResponse response =
        filter.filter(
            serverRequest,
            req -> {
              // This should not be called
              throw new IllegalStateException("Handler should not be called");
            });

    // Verify 401 status
    assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    // Verify WWW-Authenticate header is set correctly
    String wwwAuthHeader = response.headers().getFirst(HttpHeaders.WWW_AUTHENTICATE);
    assertThat(wwwAuthHeader)
        .isEqualTo(String.format("Bearer resource_metadata=\"%s\"", EXPECTED_METADATA_URL));
  }

  @Test
  void testFilterRejectsRequestWithEmptyAuthorizationHeader() throws Exception {
    // Create a mock request with empty Authorization header
    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/mcp/test");
    mockRequest.setServerName(TEST_HOST);
    mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "");

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    // Apply the filter
    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        AuthenticationRequiredFilter.requireAuthentication();

    ServerResponse response =
        filter.filter(
            serverRequest,
            req -> {
              // This should not be called
              throw new IllegalStateException("Handler should not be called");
            });

    // Verify 401 status
    assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    // Verify WWW-Authenticate header is set correctly
    String wwwAuthHeader = response.headers().getFirst(HttpHeaders.WWW_AUTHENTICATE);
    assertThat(wwwAuthHeader)
        .isEqualTo(String.format("Bearer resource_metadata=\"%s\"", EXPECTED_METADATA_URL));
  }

  @Test
  void testFilterRejectsRequestWithWhitespaceOnlyAuthorizationHeader() throws Exception {
    // Create a mock request with whitespace-only Authorization header
    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/mcp/test");
    mockRequest.setServerName(TEST_HOST);
    mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "   ");

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    // Apply the filter
    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        AuthenticationRequiredFilter.requireAuthentication();

    ServerResponse response =
        filter.filter(
            serverRequest,
            req -> {
              // This should not be called
              throw new IllegalStateException("Handler should not be called");
            });

    // Verify 401 status
    assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    // Verify WWW-Authenticate header is set correctly
    String wwwAuthHeader = response.headers().getFirst(HttpHeaders.WWW_AUTHENTICATE);
    assertThat(wwwAuthHeader)
        .isEqualTo(String.format("Bearer resource_metadata=\"%s\"", EXPECTED_METADATA_URL));
  }

  @Test
  void testFilterAllowsRequestWithBearerToken() throws Exception {
    // Create a mock request with Bearer token
    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/mcp/test");
    mockRequest.setServerName(TEST_HOST);
    mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token-123");

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    // Apply the filter
    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        AuthenticationRequiredFilter.requireAuthentication();

    final boolean[] handlerCalled = {false};

    ServerResponse response =
        filter.filter(
            serverRequest,
            req -> {
              handlerCalled[0] = true;
              return ServerResponse.ok().body("success");
            });

    // Verify the handler was called (request was allowed)
    assertThat(handlerCalled[0]).isTrue();

    // Verify successful response
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testFilterAllowsRequestWithBasicAuth() throws Exception {
    // Create a mock request with Basic auth
    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/mcp/test");
    mockRequest.setServerName(TEST_HOST);
    mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    // Apply the filter
    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        AuthenticationRequiredFilter.requireAuthentication();

    final boolean[] handlerCalled = {false};

    ServerResponse response =
        filter.filter(
            serverRequest,
            req -> {
              handlerCalled[0] = true;
              return ServerResponse.ok().body("success");
            });

    // Verify the handler was called (request was allowed)
    assertThat(handlerCalled[0]).isTrue();

    // Verify successful response
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testFilterAllowsRequestWithApiKeyHeader() throws Exception {
    // Create a mock request with API key in Authorization header
    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/mcp/test");
    mockRequest.setServerName(TEST_HOST);
    mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "ApiKey abc123xyz");

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    // Apply the filter
    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        AuthenticationRequiredFilter.requireAuthentication();

    final boolean[] handlerCalled = {false};

    ServerResponse response =
        filter.filter(
            serverRequest,
            req -> {
              handlerCalled[0] = true;
              return ServerResponse.ok().body("success");
            });

    // Verify the handler was called (request was allowed)
    assertThat(handlerCalled[0]).isTrue();

    // Verify successful response
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testFilterBuildsCorrectMetadataUrlForDifferentHosts() throws Exception {
    // Test with a different host
    String testHost = "api.example.org";
    String expectedUrl = "http://api.example.org/.well-known/oauth-protected-resource";

    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/mcp/test");
    mockRequest.setServerName(testHost);

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    // Apply the filter
    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        AuthenticationRequiredFilter.requireAuthentication();

    ServerResponse response =
        filter.filter(
            serverRequest,
            req -> {
              throw new IllegalStateException("Handler should not be called");
            });

    // Verify WWW-Authenticate header contains the correct host
    String wwwAuthHeader = response.headers().getFirst(HttpHeaders.WWW_AUTHENTICATE);
    assertThat(wwwAuthHeader)
        .isEqualTo(String.format("Bearer resource_metadata=\"%s\"", expectedUrl));
  }

  @Test
  void testFilterWWWAuthenticateHeaderFormat() throws Exception {
    // Verify the exact format matches RFC 9728
    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/mcp/test");
    mockRequest.setServerName(TEST_HOST);

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        AuthenticationRequiredFilter.requireAuthentication();

    ServerResponse response =
        filter.filter(
            serverRequest,
            req -> {
              throw new IllegalStateException("Handler should not be called");
            });

    String wwwAuthHeader = response.headers().getFirst(HttpHeaders.WWW_AUTHENTICATE);

    // Verify format: Bearer resource_metadata="<url>"
    assertThat(wwwAuthHeader).startsWith("Bearer resource_metadata=\"");
    assertThat(wwwAuthHeader).endsWith("\"");
    assertThat(wwwAuthHeader).contains("/.well-known/oauth-protected-resource");
  }
}
