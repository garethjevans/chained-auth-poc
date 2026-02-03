package org.garethjevans.chained.auth.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Gateway filter that handles requests to the OAuth 2.0 Protected Resource Metadata endpoint
 * (/.well-known/oauth-protected-resource) as defined in RFC 9728. Returns metadata about the
 * protected resource including supported authorization servers and scopes.
 */
@Component
public class ProtectedResourceMetadataFilter {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ProtectedResourceMetadataFilter.class);

  private static final String WELL_KNOWN_PATH = "/.well-known/oauth-protected-resource";
  private static final String AUTH_ADAPTER_URL = "http://127.0.0.1:9000";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Creates a filter function that intercepts requests to the well-known OAuth protected resource
   * metadata endpoint and returns the appropriate JSON response.
   *
   * @return A HandlerFilterFunction that either returns metadata or passes through the request
   */
  public static HandlerFilterFunction<ServerResponse, ServerResponse>
      serveProtectedResourceMetadata() {
    return (request, next) -> {
      String path = request.uri().getPath();

      // Check if this is a request to the well-known metadata endpoint
      if (WELL_KNOWN_PATH.equals(path)) {
        LOGGER.info("Serving OAuth protected resource metadata");

        try {
          // Build the resource metadata response per RFC 9728
          Map<String, Object> metadata = new HashMap<>();

          // Resource identifier - the protected resource's URL
          String resource =
              String.format(
                  "%s://%s",
                  request.uri().getScheme(),
                  request.uri().getHost()
                      + (request.uri().getPort() != -1 ? ":" + request.uri().getPort() : ""));
          metadata.put("resource", resource);

          // Authorization servers that can be used with this protected resource
          metadata.put("authorization_servers", List.of(AUTH_ADAPTER_URL));

          // Supported OAuth 2.0 bearer token presentation methods
          metadata.put("bearer_methods_supported", List.of("header"));

          // OAuth 2.0 scope values used to request access to this protected resource
          metadata.put("scopes_supported", List.of("openid", "profile", "email"));

          // Human-readable name of the protected resource
          metadata.put("resource_name", "Gateway Protected Resource");

          // Convert to JSON
          String jsonResponse = objectMapper.writeValueAsString(metadata);
          LOGGER.info("Serving OAuth protected resource metadata: {}", jsonResponse);

          // Return 200 OK with application/json content type
          return ServerResponse.status(HttpStatus.OK)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .body(jsonResponse);

        } catch (Exception e) {
          LOGGER.error("Failed to generate protected resource metadata", e);
          return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
      }

      // Not a metadata request, continue processing
      return next.handle(request);
    };
  }
}
