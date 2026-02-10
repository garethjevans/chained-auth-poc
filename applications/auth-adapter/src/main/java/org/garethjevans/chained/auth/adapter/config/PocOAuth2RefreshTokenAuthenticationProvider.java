package org.garethjevans.chained.auth.adapter.config;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.*;
import org.springframework.util.Assert;

public class PocOAuth2RefreshTokenAuthenticationProvider implements AuthenticationProvider {

  public static final String ACCESS_TOKEN_KEY = "access_token";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PocOAuth2RefreshTokenAuthenticationProvider.class);

  private final OAuth2RefreshTokenAuthenticationProvider delegate;
  private final OAuth2AuthorizationService authorizationService;
  private final OAuth2AuthorizedClientManager authorizedClientManager;
  private final OAuth2AuthorizedClientService authorizedClientService;

  public PocOAuth2RefreshTokenAuthenticationProvider(
      OAuth2RefreshTokenAuthenticationProvider delegate,
      OAuth2AuthorizationService authorizationService,
      OAuth2AuthorizedClientManager authorizedClientManager,
      OAuth2AuthorizedClientService authorizedClientService) {
    Assert.notNull(delegate, "delegate must not be null");
    Assert.notNull(authorizationService, "authorizationService must not be null");
    Assert.notNull(authorizedClientManager, "authorizedClientManager must not be null");
    this.delegate = delegate;
    this.authorizationService = authorizationService;
    this.authorizedClientManager = authorizedClientManager;
    this.authorizedClientService = authorizedClientService;
  }

  @Override
  public @Nullable Authentication authenticate(Authentication authentication)
      throws AuthenticationException {
    LOGGER.debug("Authenticating OAuth2RefreshTokenAuthenticationToken {}", authentication);

    OAuth2AccessTokenAuthenticationToken updated =
        (OAuth2AccessTokenAuthenticationToken) delegate.authenticate(authentication);

    // update in db
    var tokenFromDb =
        authorizationService.findByToken(
            updated.getAccessToken().getTokenValue(), OAuth2TokenType.ACCESS_TOKEN);
    LOGGER.debug("Found OAuth2RefreshTokenAuthenticationToken {}", tokenFromDb);

    OAuth2AuthorizeRequest authorizeRequest =
        OAuth2AuthorizeRequest.withClientRegistrationId("github")
            // .principal(SecurityContextHolder.getContext().getAuthentication())
            .principal(updated.getName())
            .build();

    OAuth2AccessToken accessToken =
        this.authorizedClientManager.authorize(authorizeRequest).getAccessToken();
    LOGGER.info(
        "Refreshed OAuth2AccessToken issuedAt={}, expiresAt={}, tokenValue={}",
        accessToken.getIssuedAt(),
        accessToken.getExpiresAt(),
        accessToken.getTokenValue());

    //    // check if this access token should be expired
    //    if (accessToken.getIssuedAt().isBefore(Instant.now().minusSeconds(120))) {
    //      LOGGER.info("XXXXXX Expiring Access Token");
    //      authorizedClientService.removeAuthorizedClient(
    //          "github", SecurityContextHolder.getContext().getAuthentication().getName());
    //
    //      accessToken = this.authorizedClientManager.authorize(authorizeRequest).getAccessToken();
    //      LOGGER.info(
    //          "Found NEW OAuth2AccessToken issuedAt={}, expiresAt={}, tokenValue={}",
    //          accessToken.getIssuedAt(),
    //          accessToken.getExpiresAt(),
    //          accessToken.getTokenValue());
    //    }

    String accessTokenValue = accessToken.getTokenValue();

    var tokenToSave =
        OAuth2Authorization.from(tokenFromDb)
            .attributes(attr -> attr.put(ACCESS_TOKEN_KEY, accessTokenValue))
            .build();

    authorizationService.save(tokenToSave);

    return updated;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return delegate.supports(authentication);
  }

  public static ObjectPostProcessor<AuthenticationProvider> postProcessor(
      HttpSecurity http,
      OAuth2AuthorizedClientManager authorizedClientManager,
      OAuth2AuthorizedClientService authorizedClientService) {
    return new ObjectPostProcessor<>() {

      @Override
      public <O extends AuthenticationProvider> O postProcess(O object) {
        // this will only work if we are not using "consent" in the auth-adapter
        if (object instanceof OAuth2RefreshTokenAuthenticationProvider authenticationProvider) {
          return (O)
              new PocOAuth2RefreshTokenAuthenticationProvider(
                  authenticationProvider,
                  http.getSharedObject(OAuth2AuthorizationService.class),
                  authorizedClientManager,
                  authorizedClientService);
        }
        return object;
      }
    };
  }
}
