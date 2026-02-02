package com.example.chained.auth.adapter.config;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.util.Assert;

public class PocOAuth2AuthorizationCodeRequestAuthenticationProvider
    implements AuthenticationProvider {

  public static final String ACCESS_TOKEN_KEY = "access_token";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PocOAuth2AuthorizationCodeRequestAuthenticationProvider.class);

  private final OAuth2AuthorizationCodeRequestAuthenticationProvider delegate;
  private final OAuth2AuthorizationService authorizationService;
  private final OAuth2AuthorizedClientManager authorizedClientManager;

  public PocOAuth2AuthorizationCodeRequestAuthenticationProvider(
      OAuth2AuthorizationCodeRequestAuthenticationProvider delegate,
      OAuth2AuthorizationService authorizationService,
      OAuth2AuthorizedClientManager authorizedClientManager) {
    Assert.notNull(delegate, "delegate must not be null");
    Assert.notNull(authorizationService, "authorizationService must not be null");
    Assert.notNull(authorizedClientManager, "authorizedClientManager must not be null");
    this.delegate = delegate;
    this.authorizationService = authorizationService;
    this.authorizedClientManager = authorizedClientManager;
  }

  @Override
  public @Nullable Authentication authenticate(Authentication authentication)
      throws AuthenticationException {
    LOGGER.debug(
        "Xxxxxxxxx Authenticating OAuth2AuthorizationCodeRequestAuthenticationProvider {}",
        authentication);
    OAuth2AuthorizationCodeRequestAuthenticationToken updated =
        (OAuth2AuthorizationCodeRequestAuthenticationToken) delegate.authenticate(authentication);

    // update in db
    var tokenFromDb =
        authorizationService.findByToken(
            updated.getAuthorizationCode().getTokenValue(),
            new OAuth2TokenType(OAuth2ParameterNames.CODE));
    LOGGER.debug(
        "Xxxxxxxxx Found OAuth2AuthorizationCodeRequestAuthenticationToken {}", tokenFromDb);

    OAuth2AuthorizeRequest authorizeRequest =
        OAuth2AuthorizeRequest.withClientRegistrationId("github")
            .principal(SecurityContextHolder.getContext().getAuthentication())
            .build();
    OAuth2AccessToken accessToken =
        this.authorizedClientManager.authorize(authorizeRequest).getAccessToken();

    var tokenToSave =
        OAuth2Authorization.from(tokenFromDb)
            .attributes(attr -> attr.put(ACCESS_TOKEN_KEY, accessToken.getTokenValue()))
            .build();

    authorizationService.save(tokenToSave);

    return updated;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return delegate.supports(authentication);
  }

  public static ObjectPostProcessor<AuthenticationProvider> postProcessor(
      HttpSecurity http, OAuth2AuthorizedClientManager authorizedClientManager) {
    return new ObjectPostProcessor<>() {

      @Override
      public <O extends AuthenticationProvider> O postProcess(O object) {
        if (object
            instanceof
            OAuth2AuthorizationCodeRequestAuthenticationProvider authenticationProvider) {
          return (O)
              new PocOAuth2AuthorizationCodeRequestAuthenticationProvider(
                  authenticationProvider,
                  http.getSharedObject(OAuth2AuthorizationService.class),
                  authorizedClientManager);
        }
        return object;
      }
    };
  }
}
