package com.example.chained.auth.githubmcp.config;

import com.example.chained.auth.githubmcp.tool.GitHubTools;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class McpServerConfig {

  @Bean
  public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
  }

  @Bean
  public GitHubTools gitHubTools(WebClient.Builder webClientBuilder) {
    return new GitHubTools(webClientBuilder);
  }
}
