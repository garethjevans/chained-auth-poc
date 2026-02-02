package com.example.chained.auth.githubmcp.tool;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GitHubTools {

  private final WebClient webClient;

  public GitHubTools(WebClient.Builder webClientBuilder) {
    this.webClient =
        webClientBuilder
            .baseUrl("https://api.github.com")
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();
  }

  public Map<String, Object> getMe(String authorizationHeader) {
    try {
      Map<String, Object> result =
          webClient
              .get()
              .uri("/user")
              .header("Authorization", authorizationHeader)
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
}
