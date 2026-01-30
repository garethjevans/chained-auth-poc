package com.example.chained.auth.adapter.config;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;

public class PocOAuth2AuthorizationCodeRequestAuthenticationProvider
    implements AuthenticationProvider {

  private static final Logger log =
      LoggerFactory.getLogger(PocOAuth2AuthorizationCodeRequestAuthenticationProvider.class);
  private final OAuth2AuthorizationCodeRequestAuthenticationProvider delegate;
  private final OAuth2AuthorizationService authorizationService;

  public PocOAuth2AuthorizationCodeRequestAuthenticationProvider(
      OAuth2AuthorizationCodeRequestAuthenticationProvider delegate,
      OAuth2AuthorizationService authorizationService) {
    this.delegate = delegate;
    this.authorizationService = authorizationService;
  }

  @Override
  public @Nullable Authentication authenticate(Authentication authentication)
      throws AuthenticationException {
    log.debug(
        "Xxxxxxxxx Authenticating OAuth2AuthorizationCodeRequestAuthenticationProvider {}",
        authentication);
    OAuth2AuthorizationCodeRequestAuthenticationToken updated =
        (OAuth2AuthorizationCodeRequestAuthenticationToken) delegate.authenticate(authentication);

    // update in db
    var tokenFromDb =
        authorizationService.findByToken(
            updated.getAuthorizationCode().getTokenValue(),
            new OAuth2TokenType(OAuth2ParameterNames.CODE));
    log.debug("Xxxxxxxxx Found OAuth2AuthorizationCodeRequestAuthenticationToken {}", tokenFromDb);

    var tokenToSave =
        OAuth2Authorization.from(tokenFromDb)
            .attributes(attr -> attr.put("access_token", "gho_sfsdfsdfsdfsdfsdfsdfsdfsdfsfd"))
            .build();
    authorizationService.save(tokenToSave);

    return updated;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return delegate.supports(authentication);
  }

  public static ObjectPostProcessor<AuthenticationProvider> postProcessor(HttpSecurity http) {
    return new ObjectPostProcessor<AuthenticationProvider>() {

      @Override
      public <O extends AuthenticationProvider> O postProcess(O object) {
        if (object
            instanceof
            OAuth2AuthorizationCodeRequestAuthenticationProvider authenticationProvider) {
          return (O)
              new PocOAuth2AuthorizationCodeRequestAuthenticationProvider(
                  authenticationProvider, http.getSharedObject(OAuth2AuthorizationService.class));
        }
        return object;
      }
    };
  }
}
