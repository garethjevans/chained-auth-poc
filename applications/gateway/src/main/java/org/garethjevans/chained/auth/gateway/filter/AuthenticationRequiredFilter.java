package org.garethjevans.chained.auth.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Gateway filter that enforces authentication by returning a 401 Unauthorized response with a
 * WWW-Authenticate header when no Authorization header is present in the incoming request. The
 * WWW-Authenticate header is formatted according to RFC 9728 (OAuth 2.0 Protected Resource
 * Metadata).
 */
@Component
public class AuthenticationRequiredFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationRequiredFilter.class);

  private static final String RESOURCE_METADATA_URL_TEMPLATE =
      "https://%s/.well-known/oauth-protected-resource";

  /**
   * Creates a filter function that checks for the presence of an Authorization header. If the
   * header is missing, returns a 401 Unauthorized response with the WWW-Authenticate header set
   * according to the Protected Resource Metadata spec (RFC 9728).
   *
   * @return A HandlerFilterFunction that either allows the request to proceed or returns a 401
   *     response
   */
  public static HandlerFilterFunction<ServerResponse, ServerResponse> requireAuthentication() {
    return (request, next) -> {
      String authHeader = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);

      if (authHeader == null || authHeader.trim().isEmpty()) {
        LOGGER.warn(
            "Request to {} rejected: No Authorization header present", request.uri().getPath());

        // Build the resource metadata URL from the request's host
        String host = request.uri().getHost();
        String resourceMetadataUrl = String.format(RESOURCE_METADATA_URL_TEMPLATE, host);

        LOGGER.info("Setting {} to {}", HttpHeaders.WWW_AUTHENTICATE, resourceMetadataUrl);

        // Return 401 with WWW-Authenticate header as per RFC 9728
        return ServerResponse.status(HttpStatus.UNAUTHORIZED)
            .header(
                HttpHeaders.WWW_AUTHENTICATE,
                String.format("Bearer resource_metadata=\"%s\"", resourceMetadataUrl))
            .build();
      }

      // Authorization header is present, continue processing
      return next.handle(request);
    };
  }
}
