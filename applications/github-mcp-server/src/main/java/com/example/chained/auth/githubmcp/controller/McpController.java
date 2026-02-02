package com.example.chained.auth.githubmcp.controller;

import com.example.chained.auth.githubmcp.tool.GitHubTools;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mcp")
public class McpController {

  private final GitHubTools gitHubTools;

  public McpController(GitHubTools gitHubTools) {
    this.gitHubTools = gitHubTools;
  }

  @PostMapping("/tools/call")
  public ResponseEntity<Map<String, Object>> callTool(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
      @RequestBody Map<String, Object> request) {

    String toolName = (String) request.get("name");

    if ("get_me".equals(toolName)) {
      Map<String, Object> result = gitHubTools.getMe(authorizationHeader);
      return ResponseEntity.ok(result);
    }

    return ResponseEntity.badRequest().body(Map.of("error", "Unknown tool: " + toolName));
  }

  @GetMapping("/tools/list")
  public ResponseEntity<Map<String, Object>> listTools() {
    return ResponseEntity.ok(
        Map.of(
            "tools",
            java.util.List.of(
                Map.of(
                    "name",
                    "get_me",
                    "description",
                    "Get details about the currently logged in GitHub user",
                    "inputSchema",
                    Map.of(
                        "type", "object",
                        "properties", Map.of(),
                        "required", java.util.List.of())))));
  }
}
