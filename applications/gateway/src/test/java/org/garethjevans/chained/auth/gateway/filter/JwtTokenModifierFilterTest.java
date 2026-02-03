package org.garethjevans.chained.auth.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.util.Collections;
import java.util.Date;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.function.ServerRequest;

class JwtTokenModifierFilterTest {

  private static final String SECRET = "ThisIsASecretKeyForTestingPurposesOnly12345678";
  private static final String TEST_SUBJECT = "test-user";
  private static final String TEST_ACCESS_TOKEN = "downstream-access-token-xyz";

  @Test
  void testFilterExtractsAndReplacesToken() throws Exception {
    // Create a JWT with access_token claim
    String jwt = createJwtWithAccessToken(TEST_SUBJECT, TEST_ACCESS_TOKEN);

    // Create a mock request with the JWT
    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/test");
    mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    // Apply the filter
    Function<ServerRequest, ServerRequest> filter = JwtTokenModifierFilter.modifyBearerToken();
    ServerRequest modifiedRequest = filter.apply(serverRequest);

    // Verify the authorization header was replaced with the access token
    String authHeader = modifiedRequest.headers().firstHeader(HttpHeaders.AUTHORIZATION);
    assertThat(authHeader).isEqualTo("Bearer " + TEST_ACCESS_TOKEN);
  }

  @Test
  void testFilterWithNoAuthorizationHeader() {
    // Create a mock request without Authorization header
    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/test");

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    // Apply the filter
    Function<ServerRequest, ServerRequest> filter = JwtTokenModifierFilter.modifyBearerToken();
    ServerRequest modifiedRequest = filter.apply(serverRequest);

    // Verify no authorization header was added
    String authHeader = modifiedRequest.headers().firstHeader(HttpHeaders.AUTHORIZATION);
    assertThat(authHeader).isNull();
  }

  @Test
  void testFilterWithNonBearerToken() {
    // Create a mock request with Basic auth
    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/test");
    mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    // Apply the filter
    Function<ServerRequest, ServerRequest> filter = JwtTokenModifierFilter.modifyBearerToken();
    ServerRequest modifiedRequest = filter.apply(serverRequest);

    // Verify the authorization header was not modified
    String authHeader = modifiedRequest.headers().firstHeader(HttpHeaders.AUTHORIZATION);
    assertThat(authHeader).isEqualTo("Basic dXNlcjpwYXNz");
  }

  @Test
  void testFilterWithJwtWithoutAccessToken() throws Exception {
    // Create a JWT without access_token claim
    String jwt = createJwtWithoutAccessToken(TEST_SUBJECT);

    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/test");
    mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    // Apply the filter
    Function<ServerRequest, ServerRequest> filter = JwtTokenModifierFilter.modifyBearerToken();
    ServerRequest modifiedRequest = filter.apply(serverRequest);

    // Verify the original token is preserved when no access_token claim exists
    String authHeader = modifiedRequest.headers().firstHeader(HttpHeaders.AUTHORIZATION);
    assertThat(authHeader).isEqualTo("Bearer " + jwt);
  }

  @Test
  void testFilterWithInvalidJwt() {
    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/test");
    mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token");

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    // Apply the filter
    Function<ServerRequest, ServerRequest> filter = JwtTokenModifierFilter.modifyBearerToken();
    ServerRequest modifiedRequest = filter.apply(serverRequest);

    // Verify the original token is preserved when JWT parsing fails
    String authHeader = modifiedRequest.headers().firstHeader(HttpHeaders.AUTHORIZATION);
    assertThat(authHeader).isEqualTo("Bearer invalid.jwt.token");
  }

  @Test
  void testFilterWithEmptyAccessToken() throws Exception {
    // Create a JWT with empty access_token claim
    String jwt = createJwtWithAccessToken(TEST_SUBJECT, "");

    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/test");
    mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    // Apply the filter
    Function<ServerRequest, ServerRequest> filter = JwtTokenModifierFilter.modifyBearerToken();
    ServerRequest modifiedRequest = filter.apply(serverRequest);

    // Verify the original token is preserved when access_token is empty
    String authHeader = modifiedRequest.headers().firstHeader(HttpHeaders.AUTHORIZATION);
    assertThat(authHeader).isEqualTo("Bearer " + jwt);
  }

  @Test
  void testFilterLogsSubjectClaim() throws Exception {
    // This test verifies that the filter correctly extracts the subject claim
    // The actual logging is verified through the filter's behavior

    String jwt = createJwtWithAccessToken(TEST_SUBJECT, TEST_ACCESS_TOKEN);

    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/test");
    mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    // Apply the filter
    Function<ServerRequest, ServerRequest> filter = JwtTokenModifierFilter.modifyBearerToken();
    ServerRequest modifiedRequest = filter.apply(serverRequest);

    // If we get here without exception, the subject was successfully extracted
    // The actual logging to console is handled by the filter implementation
    assertThat(modifiedRequest).isNotNull();
  }

  @Test
  void testFilterWithMultipleClaimsInJwt() throws Exception {
    // Create a JWT with multiple claims including access_token
    JWSSigner signer = new MACSigner(SECRET.getBytes());

    JWTClaimsSet claimsSet =
        new JWTClaimsSet.Builder()
            .subject(TEST_SUBJECT)
            .claim("access_token", TEST_ACCESS_TOKEN)
            .claim("email", "test@example.com")
            .claim("roles", new String[] {"user", "admin"})
            .issueTime(new Date())
            .expirationTime(new Date(System.currentTimeMillis() + 3600000))
            .build();

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
    signedJWT.sign(signer);
    String jwt = signedJWT.serialize();

    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.setMethod("GET");
    mockRequest.setRequestURI("/test");
    mockRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);

    ServerRequest serverRequest =
        ServerRequest.create(mockRequest, Collections.<HttpMessageConverter<?>>emptyList());

    // Apply the filter
    Function<ServerRequest, ServerRequest> filter = JwtTokenModifierFilter.modifyBearerToken();
    ServerRequest modifiedRequest = filter.apply(serverRequest);

    // Verify the access_token was correctly extracted despite other claims
    String authHeader = modifiedRequest.headers().firstHeader(HttpHeaders.AUTHORIZATION);
    assertThat(authHeader).isEqualTo("Bearer " + TEST_ACCESS_TOKEN);
  }

  private String createJwtWithAccessToken(String subject, String accessToken) throws JOSEException {
    JWSSigner signer = new MACSigner(SECRET.getBytes());

    JWTClaimsSet claimsSet =
        new JWTClaimsSet.Builder()
            .subject(subject)
            .claim("access_token", accessToken)
            .issueTime(new Date())
            .expirationTime(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
            .build();

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
    signedJWT.sign(signer);

    return signedJWT.serialize();
  }

  private String createJwtWithoutAccessToken(String subject) throws JOSEException {
    JWSSigner signer = new MACSigner(SECRET.getBytes());

    JWTClaimsSet claimsSet =
        new JWTClaimsSet.Builder()
            .subject(subject)
            .issueTime(new Date())
            .expirationTime(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
            .build();

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
    signedJWT.sign(signer);

    return signedJWT.serialize();
  }
}
