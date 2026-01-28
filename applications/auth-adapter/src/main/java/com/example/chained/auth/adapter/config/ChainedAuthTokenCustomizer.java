package com.example.chained.auth.adapter.config;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Customizes JWT tokens to include claims from both test-auth-server and GitHub authentications.
 * This enables chained authentication where the primary identity comes from test-auth-server and
 * additional data comes from GitHub.
 */
@Component
public class ChainedAuthTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChainedAuthTokenCustomizer.class);
  private static final String TEST_AUTH_SERVER_AUTH_KEY = "TEST_AUTH_SERVER_AUTHENTICATION";

  private final OAuth2AuthorizedClientService authorizedClientService;

  public ChainedAuthTokenCustomizer(OAuth2AuthorizedClientService authorizedClientService) {
    this.authorizedClientService = authorizedClientService;
  }

  @Override
  public void customize(JwtEncodingContext context) {
    LOGGER.info("=== Starting JWT Token Customization ===");

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    LOGGER.debug(
        "Current authentication: type={}, principal={}",
        authentication != null ? authentication.getClass().getSimpleName() : "null",
        authentication != null ? authentication.getName() : "null");

    // Get test-auth-server authentication from session
    OAuth2User testAuthServerUser = getTestAuthServerUser();
    if (testAuthServerUser != null) {
      Object usernameAttr = testAuthServerUser.getAttribute("preferred_username");
      LOGGER.info(
          "Found test-auth-server user in session: {}",
          usernameAttr != null ? usernameAttr.toString() : "[no username]");
    } else {
      LOGGER.warn("No test-auth-server user found in session!");
    }

    // Get current authentication (might be GitHub)
    OAuth2User currentUser = null;
    String currentRegistration = null;
    if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
      currentUser = oauth2Token.getPrincipal();
      currentRegistration = oauth2Token.getAuthorizedClientRegistrationId();
      LOGGER.info("Current OAuth2 registration: {}", currentRegistration);
      LOGGER.debug(
          "Current user attributes: {}",
          currentUser != null ? currentUser.getAttributes() : "null");
    } else {
      LOGGER.debug("Current authentication is not OAuth2AuthenticationToken");
    }

    // Add claims from test-auth-server (primary identity)
    if (testAuthServerUser != null) {
      LOGGER.info("Adding test-auth-server claims to JWT");
      addTestAuthServerClaims(context, testAuthServerUser);
    } else {
      LOGGER.warn(
          "Skipping test-auth-server claims - no test-auth-server user available in session");
    }

    // Add additional claims from GitHub if available
    if (currentUser != null && "github".equals(currentRegistration)) {
      LOGGER.info("Adding GitHub claims to JWT");
      addGitHubClaims(context, currentUser, (OAuth2AuthenticationToken) authentication);
    } else {
      LOGGER.debug(
          "Skipping GitHub claims - currentUser={}, registration={}",
          currentUser != null ? "present" : "null",
          currentRegistration);
    }

    // Add authorities from current authentication
    if (authentication != null) {
      LOGGER.debug("Adding authorities: {}", authentication.getAuthorities());
      context.getClaims().claim("authorities", authentication.getAuthorities());
    }

    LOGGER.info("=== Completed JWT Token Customization ===");
  }

  private void addTestAuthServerClaims(JwtEncodingContext context, OAuth2User oauth2User) {
    LOGGER.debug("Processing test-auth-server claims");

    // Extract the sub claim from the test-auth-server authentication
    String testAuthServerSub = null;

    // Check if this is an OIDC user (from test-auth-server)
    if (oauth2User instanceof OidcUser oidcUser) {
      testAuthServerSub = oidcUser.getSubject();
      LOGGER.debug("Extracted sub from OidcUser: {}", testAuthServerSub);
    } else {
      // Fallback to extracting from attributes
      Object subAttribute = oauth2User.getAttribute("sub");
      if (subAttribute != null) {
        testAuthServerSub = subAttribute.toString();
        LOGGER.debug("Extracted sub from OAuth2User attributes: {}", testAuthServerSub);
      } else {
        LOGGER.warn("No 'sub' attribute found in OAuth2User");
      }
    }

    // Add the test-auth-server sub claim to the JWT token
    if (testAuthServerSub != null) {
      LOGGER.info("Setting primary subject (sub) to: {}", testAuthServerSub);
      context
          .getClaims()
          .claim("test_auth_server_sub", testAuthServerSub)
          .claim("sub", testAuthServerSub); // Override sub with test-auth-server sub

      // Add username/preferred_username from test-auth-server
      String preferredUsername = oauth2User.getAttribute("preferred_username");
      if (preferredUsername != null) {
        LOGGER.debug("Adding preferred_username: {}", preferredUsername);
        context.getClaims().claim("preferred_username", preferredUsername);
      } else {
        LOGGER.debug("No preferred_username found in test-auth-server user");
      }

      // Add name from test-auth-server
      String name = oauth2User.getAttribute("name");
      if (name != null) {
        LOGGER.debug("Adding name: {}", name);
        context.getClaims().claim("name", name);
      } else {
        LOGGER.debug("No name found in test-auth-server user");
      }

      LOGGER.info(
          "Successfully added test-auth-server claims: sub={}, preferred_username={}, name={}",
          testAuthServerSub,
          preferredUsername,
          name);
    } else {
      LOGGER.error(
          "Failed to extract test-auth-server sub claim - JWT will not have proper identity!");
    }
  }

  private void addGitHubClaims(
      JwtEncodingContext context, OAuth2User githubUser, OAuth2AuthenticationToken oauth2Token) {
    LOGGER.debug("Processing GitHub claims");

    int claimsAdded = 0;

    // Add GitHub-specific claims with github_ prefix to avoid conflicts
    String githubLogin = githubUser.getAttribute("login");
    if (githubLogin != null) {
      LOGGER.debug("Adding github_login: {}", githubLogin);
      context.getClaims().claim("github_login", githubLogin);
      claimsAdded++;
    }

    String githubName = githubUser.getAttribute("name");
    if (githubName != null) {
      LOGGER.debug("Adding github_name: {}", githubName);
      context.getClaims().claim("github_name", githubName);
      claimsAdded++;
    }

    String githubEmail = githubUser.getAttribute("email");
    if (githubEmail != null) {
      LOGGER.debug("Adding github_email: {}", githubEmail);
      context.getClaims().claim("github_email", githubEmail);
      claimsAdded++;
    }

    Integer githubId = githubUser.getAttribute("id");
    if (githubId != null) {
      LOGGER.debug("Adding github_id: {}", githubId);
      context.getClaims().claim("github_id", githubId);
      claimsAdded++;
    }

    String githubAvatarUrl = githubUser.getAttribute("avatar_url");
    if (githubAvatarUrl != null) {
      LOGGER.debug("Adding github_avatar_url: {}", githubAvatarUrl);
      context.getClaims().claim("github_avatar_url", githubAvatarUrl);
      claimsAdded++;
    }

    // Add GitHub access token
    String githubAccessToken = getGitHubAccessToken(oauth2Token);
    if (githubAccessToken != null) {
      LOGGER.info("Adding github_access_token to JWT (length: {})", githubAccessToken.length());
      context.getClaims().claim("github_access_token", githubAccessToken);
      claimsAdded++;
    } else {
      LOGGER.warn("GitHub access token not available - will not be included in JWT");
    }

    LOGGER.info(
        "Successfully added {} GitHub claims: login={}, name={}, email={}, id={}, avatar={}, access_token={}",
        claimsAdded,
        githubLogin,
        githubName,
        githubEmail != null ? "[present]" : null,
        githubId,
        githubAvatarUrl != null ? "[present]" : null,
        githubAccessToken != null ? "[present]" : null);
  }

  private String getGitHubAccessToken(OAuth2AuthenticationToken oauth2Token) {
    LOGGER.debug("Attempting to retrieve GitHub access token");

    try {
      OAuth2AuthorizedClient authorizedClient =
          authorizedClientService.loadAuthorizedClient(
              oauth2Token.getAuthorizedClientRegistrationId(), oauth2Token.getName());

      if (authorizedClient == null) {
        LOGGER.warn("No OAuth2AuthorizedClient found for GitHub");
        return null;
      }

      OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
      if (accessToken == null) {
        LOGGER.warn("OAuth2AuthorizedClient exists but access token is null");
        return null;
      }

      String tokenValue = accessToken.getTokenValue();
      LOGGER.debug(
          "Retrieved GitHub access token: type={}, expires={}",
          accessToken.getTokenType() != null ? accessToken.getTokenType().getValue() : "null",
          accessToken.getExpiresAt());

      return tokenValue;
    } catch (Exception e) {
      LOGGER.error("Failed to retrieve GitHub access token", e);
      return null;
    }
  }

  private OAuth2User getTestAuthServerUser() {
    LOGGER.debug("Attempting to retrieve test-auth-server user from session");

    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      LOGGER.warn("No ServletRequestAttributes available - cannot access session");
      return null;
    }

    HttpSession session = attributes.getRequest().getSession(false);
    if (session == null) {
      LOGGER.warn("No HTTP session available - user may not have completed test-auth-server login");
      return null;
    }

    LOGGER.debug("Session ID: {}", session.getId());

    Object auth = session.getAttribute(TEST_AUTH_SERVER_AUTH_KEY);
    if (auth == null) {
      LOGGER.warn(
          "No '{}' attribute found in session - test-auth-server authentication not stored",
          TEST_AUTH_SERVER_AUTH_KEY);
      return null;
    }

    if (auth instanceof OAuth2AuthenticationToken oauth2Token) {
      OAuth2User user = oauth2Token.getPrincipal();
      Object usernameAttr = user.getAttribute("preferred_username");
      LOGGER.info(
          "Retrieved test-auth-server user from session: registration={}, username={}",
          oauth2Token.getAuthorizedClientRegistrationId(),
          usernameAttr != null ? usernameAttr.toString() : "[no username]");
      return user;
    } else {
      LOGGER.error(
          "Session attribute '{}' is not OAuth2AuthenticationToken: {}",
          TEST_AUTH_SERVER_AUTH_KEY,
          auth.getClass().getName());
      return null;
    }
  }
}
