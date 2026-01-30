package com.example.chained.auth.adapter.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Custom authentication success handler for chained authentication flow. After successful
 * test-auth-server login, redirects to GitHub authentication to collect additional user data.
 */
@Component
public class ChainedAuthenticationSuccessHandler
    extends SavedRequestAwareAuthenticationSuccessHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ChainedAuthenticationSuccessHandler.class);

  private static final String TEST_AUTH_SERVER_REGISTRATION = "test-auth-server";
  private static final String GITHUB_REGISTRATION = "github";
  private static final String TEST_AUTH_SERVER_AUTH_KEY = "TEST_AUTH_SERVER_AUTHENTICATION";

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws ServletException, IOException {

    LOGGER.info("=== Authentication Success Handler Invoked ===");
    LOGGER.debug(
        "Request URI: {}, Method: {}, Remote Address: {}",
        request.getRequestURI(),
        request.getMethod(),
        request.getRemoteAddr());

    // Log authentication details
    if (authentication != null) {
      LOGGER.info(
          "Authentication type: {}, Principal: {}, Authenticated: {}",
          authentication.getClass().getSimpleName(),
          authentication.getName(),
          authentication.isAuthenticated());
      LOGGER.debug("Authorities: {}", authentication.getAuthorities());
    } else {
      LOGGER.warn("Authentication object is null!");
    }

    if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
      String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
      LOGGER.info("OAuth2 authentication detected: registrationId={}", registrationId);

      // Log OAuth2 user details
      OAuth2User oauth2User = oauth2Token.getPrincipal();
      if (oauth2User != null) {
        LOGGER.debug("OAuth2 User attributes: {}", oauth2User.getAttributes());
        LOGGER.debug(
            "OAuth2 User name attribute: {}",
            oauth2User.getAttribute("name") != null
                ? oauth2User.getAttribute("name")
                : "[not present]");
      } else {
        LOGGER.warn("OAuth2User principal is null");
      }

      HttpSession session = request.getSession();
      LOGGER.debug(
          "Session ID: {}, IsNew: {}, MaxInactiveInterval: {}s",
          session.getId(),
          session.isNew(),
          session.getMaxInactiveInterval());

      // Check which OAuth2 provider authenticated
      if (TEST_AUTH_SERVER_REGISTRATION.equals(registrationId)) {
        LOGGER.info("=== Step 1: test-auth-server authentication complete ===");

        // First authentication (test-auth-server) is complete
        // Store it in session and redirect to GitHub
        LOGGER.info(
            "Storing test-auth-server authentication in session with key: {}",
            TEST_AUTH_SERVER_AUTH_KEY);
        session.setAttribute(TEST_AUTH_SERVER_AUTH_KEY, authentication);

        // Verify it was stored
        Object storedAuth = session.getAttribute(TEST_AUTH_SERVER_AUTH_KEY);
        if (storedAuth != null) {
          LOGGER.debug(
              "Successfully stored test-auth-server authentication in session: type={}",
              storedAuth.getClass().getSimpleName());
        } else {
          LOGGER.error("Failed to store test-auth-server authentication in session!");
        }

        // Redirect to GitHub OAuth2 login
        String redirectUrl = "/oauth2/authorization/github";
        LOGGER.info("=== Redirecting to GitHub authentication: {} ===", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        return;

      } else if (GITHUB_REGISTRATION.equals(registrationId)) {
        LOGGER.info("=== Step 2: GitHub authentication complete ===");

        // Second authentication (GitHub) is complete
        // Both authentications are now available
        // The session already has test-auth-server auth stored
        // Spring Security context has GitHub auth

        // Verify test-auth-server auth is still in session
        Object testAuthServerAuth = session.getAttribute(TEST_AUTH_SERVER_AUTH_KEY);
        if (testAuthServerAuth != null) {
          LOGGER.info(
              "Confirmed: test-auth-server authentication found in session: type={}",
              testAuthServerAuth.getClass().getSimpleName());

          if (testAuthServerAuth instanceof OAuth2AuthenticationToken testAuthToken) {
            OAuth2User testAuthUser = testAuthToken.getPrincipal();
            Object testAuthUsername =
                testAuthUser != null ? testAuthUser.getAttribute("preferred_username") : null;
            LOGGER.debug(
                "Test-auth-server user: {}",
                testAuthUsername != null ? testAuthUsername : "[none]");
          }
        } else {
          LOGGER.error(
              "ERROR: test-auth-server authentication NOT found in session! Chained auth may fail!");
        }

        LOGGER.info("=== Both authentications complete, continuing normal flow ===");
        // Continue with normal flow
      } else {
        LOGGER.warn("Unknown OAuth2 registration: {}", registrationId);
      }
    } else {
      LOGGER.warn(
          "Authentication is not OAuth2AuthenticationToken: {}",
          authentication != null ? authentication.getClass().getName() : "null");
    }

    // Call parent handler for default behavior (redirect to original request or default success
    // URL)
    LOGGER.info("Delegating to parent SavedRequestAwareAuthenticationSuccessHandler");
    try {
      super.onAuthenticationSuccess(request, response, authentication);
      LOGGER.info("=== Authentication Success Handler Completed Successfully ===");
    } catch (Exception e) {
      LOGGER.error("Error in parent authentication success handler", e);
      throw e;
    }
  }
}
