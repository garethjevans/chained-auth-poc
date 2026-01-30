package com.example.chained.auth.adapter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

@Component
public class PocAuthTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PocAuthTokenCustomizer.class);

  @Override
  public void customize(JwtEncodingContext context) {
    // load the token from the db
    String accessToken =
        context
            .getAuthorization()
            .getAttribute(PocOAuth2AuthorizationCodeRequestAuthenticationProvider.ACCESS_TOKEN_KEY);
    LOGGER.debug("XXXXXXXXXXX Loaded raw access token from attributes: {}", accessToken);
    context
        .getClaims()
        .claim(
            PocOAuth2AuthorizationCodeRequestAuthenticationProvider.ACCESS_TOKEN_KEY, accessToken);
  }
}
