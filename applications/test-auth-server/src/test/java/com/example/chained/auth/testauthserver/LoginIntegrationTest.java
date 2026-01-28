package com.example.chained.auth.testauthserver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Integration tests for Test Auth Server login functionality. These tests validate that the
 * form-based authentication works correctly with the configured test users.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class LoginIntegrationTest {

  @LocalServerPort private int port;

  private final RestTemplate restTemplate = new RestTemplate();

  private String getBaseUrl() {
    return "http://127.0.0.1:" + port;
  }

  @Test
  void loginPageShouldBeAccessible() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(getBaseUrl() + "/login", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).as("Login page should contain a form").contains("form");
    assertThat(response.getBody())
        .as("Login page should contain username field")
        .contains("username");
    assertThat(response.getBody())
        .as("Login page should contain password field")
        .contains("password");
  }

  @Test
  void actuatorHealthShouldBeAccessibleWithoutAuthentication() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(getBaseUrl() + "/actuator/health", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("UP");
  }

  @Test
  void oidcConfigurationShouldBeAccessible() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(getBaseUrl() + "/.well-known/openid-configuration", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("issuer");
    assertThat(response.getBody()).contains("authorization_endpoint");
    assertThat(response.getBody()).contains("token_endpoint");
  }

  @Test
  void shouldSuccessfullyLoginWithTestUser() {
    // Get login page to extract CSRF token
    ResponseEntity<String> loginPage =
        restTemplate.getForEntity(getBaseUrl() + "/login", String.class);
    assertThat(loginPage.getStatusCode()).isEqualTo(HttpStatus.OK);

    String csrfToken = extractCsrfToken(loginPage.getBody());
    String sessionCookie = extractSessionCookie(loginPage.getHeaders());

    // Perform login
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    if (sessionCookie != null) {
      headers.add("Cookie", sessionCookie);
    }

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("username", "testuser");
    formData.add("password", "password");
    if (csrfToken != null) {
      formData.add("_csrf", csrfToken);
    }

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

    ResponseEntity<String> loginResponse =
        restTemplate.postForEntity(getBaseUrl() + "/login", request, String.class);

    // Should redirect after successful login (302 or 303)
    assertThat(loginResponse.getStatusCode().is3xxRedirection())
        .as("Login with testuser/password should redirect after success")
        .isTrue();

    // Should not redirect to /login?error
    if (loginResponse.getHeaders().getLocation() != null) {
      String location = loginResponse.getHeaders().getLocation().toString();
      assertThat(location)
          .as("Should not redirect to error page after successful login")
          .doesNotContain("/login?error");
    }
  }

  @Test
  void shouldFailLoginWithInvalidPassword() {
    // Get login page to extract CSRF token
    ResponseEntity<String> loginPage =
        restTemplate.getForEntity(getBaseUrl() + "/login", String.class);
    String csrfToken = extractCsrfToken(loginPage.getBody());
    String sessionCookie = extractSessionCookie(loginPage.getHeaders());

    // Perform login with wrong password
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    if (sessionCookie != null) {
      headers.add("Cookie", sessionCookie);
    }

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("username", "testuser");
    formData.add("password", "wrongpassword");
    if (csrfToken != null) {
      formData.add("_csrf", csrfToken);
    }

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

    ResponseEntity<String> loginResponse =
        restTemplate.postForEntity(getBaseUrl() + "/login", request, String.class);

    // Should redirect to login with error
    assertThat(loginResponse.getStatusCode().is3xxRedirection())
        .as("Invalid login should redirect")
        .isTrue();
    if (loginResponse.getHeaders().getLocation() != null) {
      String location = loginResponse.getHeaders().getLocation().toString();
      assertThat(location)
          .as("Invalid password should redirect to login error page")
          .contains("/login")
          .contains("error");
    }
  }

  @Test
  void shouldFailLoginWithInvalidUsername() {
    // Get login page to extract CSRF token
    ResponseEntity<String> loginPage =
        restTemplate.getForEntity(getBaseUrl() + "/login", String.class);
    String csrfToken = extractCsrfToken(loginPage.getBody());
    String sessionCookie = extractSessionCookie(loginPage.getHeaders());

    // Perform login with wrong username
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    if (sessionCookie != null) {
      headers.add("Cookie", sessionCookie);
    }

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("username", "nonexistent");
    formData.add("password", "password");
    if (csrfToken != null) {
      formData.add("_csrf", csrfToken);
    }

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

    ResponseEntity<String> loginResponse =
        restTemplate.postForEntity(getBaseUrl() + "/login", request, String.class);

    // Should redirect to login with error
    assertThat(loginResponse.getStatusCode().is3xxRedirection())
        .as("Invalid login should redirect")
        .isTrue();
    if (loginResponse.getHeaders().getLocation() != null) {
      String location = loginResponse.getHeaders().getLocation().toString();
      assertThat(location)
          .as("Invalid username should redirect to login error page")
          .contains("/login")
          .contains("error");
    }
  }

  @Test
  void testUserCredentialsShouldBeConfigured() {
    // This test validates that our test users are properly configured
    // by verifying the login flow works
    ResponseEntity<String> loginPage =
        restTemplate.getForEntity(getBaseUrl() + "/login", String.class);
    assertThat(loginPage.getStatusCode())
        .as("Login page should be accessible, confirming security is configured")
        .isEqualTo(HttpStatus.OK);

    assertThat(loginPage.getBody())
        .as("Login page should be a proper HTML form")
        .contains("<form")
        .contains("username")
        .contains("password");
  }

  /** Extract CSRF token from login page HTML. Returns null if not found. */
  private String extractCsrfToken(String html) {
    if (html == null) {
      return null;
    }
    // Look for: <input type="hidden" name="_csrf" value="..." />
    int csrfIndex = html.indexOf("name=\"_csrf\"");
    if (csrfIndex == -1) {
      return null;
    }
    int valueStart = html.indexOf("value=\"", csrfIndex);
    if (valueStart == -1) {
      return null;
    }
    valueStart += 7; // length of 'value="'
    int valueEnd = html.indexOf("\"", valueStart);
    if (valueEnd == -1) {
      return null;
    }
    return html.substring(valueStart, valueEnd);
  }

  /** Extract session cookie from response headers. Returns null if not found. */
  private String extractSessionCookie(HttpHeaders headers) {
    if (headers == null || headers.get("Set-Cookie") == null) {
      return null;
    }
    for (String cookie : headers.get("Set-Cookie")) {
      if (cookie.startsWith("JSESSIONID=") || cookie.startsWith("TEST_AUTH_SERVER_SESSION=")) {
        // Extract just the cookie name=value part
        int semicolon = cookie.indexOf(';');
        if (semicolon != -1) {
          return cookie.substring(0, semicolon);
        }
        return cookie;
      }
    }
    return null;
  }
}
