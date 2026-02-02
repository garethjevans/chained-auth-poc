package com.example.chained.auth.githubmcp.tool;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * GitHub API integration tool for MCP server. Provides a get_me method to fetch current user
 * information from GitHub.
 *
 * <p>Note: This will be auto-discovered by Spring AI MCP if annotations are properly configured.
 * For now, this is a simple component that can be manually registered or used via Spring AI's
 * Function calling interface.
 */
@Component("get_me")
public class GitHubTools implements Function<Map<String, Object>, Map<String, Object>> {

  private final WebClient webClient;

  public GitHubTools(WebClient.Builder webClientBuilder) {
    this.webClient =
        webClientBuilder
            .baseUrl("https://api.github.com")
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();
  }

  @Override
  public Map<String, Object> apply(Map<String, Object> input) {
    return getMe();
  }

  public Map<String, Object> getMe() {
    // Extract Bearer token from the current request context
    String authHeader = extractAuthorizationHeader();

    try {
      Map<String, Object> result =
          webClient
              .get()
              .uri("/user")
              .header("Authorization", authHeader)
              .retrieve()
              .bodyToMono(Map.class)
              .block();

      return result != null ? result : new HashMap<>();
    } catch (Exception error) {
      // Return error details as a map
      Map<String, Object> errorResult = new HashMap<>();
      errorResult.put("error", "Failed to fetch user details");
      errorResult.put("message", error.getMessage());
      return errorResult;
    }
  }

  private String extractAuthorizationHeader() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        String authHeader = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        return authHeader != null ? authHeader : "";
      }
    } catch (Exception e) {
      // If we can't get the request context, return empty string
    }
    return "";
  }
}
