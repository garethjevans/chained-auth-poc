package org.garethjevans.chained.auth.gateway.filter;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

/**
 * Gateway filter that modifies the Bearer token in the Authorization header. Extracts the JWT from
 * the incoming request, logs the "sub" claim, and replaces the Bearer token with the value from the
 * "access_token" claim for downstream requests.
 */
@Component
public class JwtTokenModifierFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenModifierFilter.class);

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String ACCESS_TOKEN_CLAIM = "access_token";
  private static final String SUB_CLAIM = "sub";

  /**
   * Creates a before filter function that modifies the Authorization header.
   *
   * @return A function that processes the ServerRequest and returns a modified ServerRequest
   */
  public static Function<ServerRequest, ServerRequest> modifyBearerToken() {
    return request -> {
      String authHeader = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);

      if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
        LOGGER.debug("No Bearer token found in Authorization header");
        return request;
      }

      try {
        String token = authHeader.substring(BEARER_PREFIX.length());
        SignedJWT jwt = SignedJWT.parse(token);
        JWTClaimsSet claims = jwt.getJWTClaimsSet();

        // Log the subject claim
        String subject = claims.getStringClaim(SUB_CLAIM);
        LOGGER.info("Processing JWT with subject: {}", subject);

        // Extract the access_token claim
        String accessToken = claims.getStringClaim(ACCESS_TOKEN_CLAIM);

        if (accessToken == null || accessToken.isEmpty()) {
          LOGGER.warn(
              "No access_token claim found in JWT for subject: {}, using original token", subject);
          return request;
        }

        // Replace the Authorization header with the new access token
        LOGGER.debug("Replacing Bearer token with access_token from JWT claims");
        return ServerRequest.from(request)
            .headers(
                httpHeaders -> {
                  httpHeaders.remove(HttpHeaders.AUTHORIZATION);
                  httpHeaders.add(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken);
                })
            .build();

      } catch (ParseException e) {
        LOGGER.error("Failed to parse JWT token: {}", e.getMessage(), e);
        return request;
      }
    };
  }
}
