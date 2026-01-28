package com.example.chained.auth.adapter.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

/**
 * Customizes JWT tokens to include claims from the test-auth-server authentication. This enables
 * chained authentication where the primary identity comes from test-auth-server.
 */
@Component
public class ChainedAuthTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

  @Override
  public void customize(JwtEncodingContext context) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && authentication.getPrincipal() instanceof OAuth2User oauth2User) {
      // Extract the sub claim from the test-auth-server authentication
      String testAuthServerSub = null;

      // Check if this is an OIDC user (from test-auth-server)
      if (oauth2User instanceof OidcUser oidcUser) {
        testAuthServerSub = oidcUser.getSubject();
      } else {
        // Fallback to extracting from attributes
        Object subAttribute = oauth2User.getAttribute("sub");
        if (subAttribute != null) {
          testAuthServerSub = subAttribute.toString();
        }
      }

      // Add the test-auth-server sub claim to the JWT token
      if (testAuthServerSub != null) {
        context
            .getClaims()
            .claim("test_auth_server_sub", testAuthServerSub)
            .claim("sub", testAuthServerSub); // Override sub with test-auth-server sub

        // Add username/preferred_username from test-auth-server
        String preferredUsername = oauth2User.getAttribute("preferred_username");
        if (preferredUsername != null) {
          context.getClaims().claim("preferred_username", preferredUsername);
        }

        // Add name from test-auth-server
        String name = oauth2User.getAttribute("name");
        if (name != null) {
          context.getClaims().claim("name", name);
        }
      }

      // Add all other OAuth2User attributes as custom claims
      context.getClaims().claim("authorities", oauth2User.getAuthorities());
    }
  }
}
