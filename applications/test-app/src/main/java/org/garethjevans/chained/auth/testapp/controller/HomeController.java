package org.garethjevans.chained.auth.testapp.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  private final ObjectMapper objectMapper;

  public HomeController(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @GetMapping("/")
  public String home() {
    return "index";
  }

  @GetMapping("/authenticated")
  public String authenticated(
      @RegisteredOAuth2AuthorizedClient("auth-adapter") OAuth2AuthorizedClient authorizedClient,
      @AuthenticationPrincipal OAuth2User oauth2User,
      Model model) {

    // Get the access token
    OAuth2AccessToken accessToken = authorizedClient.getAccessToken();

    // Prepare token information
    Map<String, Object> tokenInfo = new HashMap<>();
    tokenInfo.put("tokenValue", accessToken.getTokenValue());
    tokenInfo.put("tokenType", accessToken.getTokenType().getValue());
    tokenInfo.put("issuedAt", accessToken.getIssuedAt());
    tokenInfo.put("expiresAt", accessToken.getExpiresAt());
    tokenInfo.put("scopes", accessToken.getScopes());

    // Calculate expiration
    if (accessToken.getExpiresAt() != null) {
      long secondsUntilExpiry =
          accessToken.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond();
      tokenInfo.put("expiresIn", secondsUntilExpiry);
    }

    // Decode JWT token claims
    Map<String, Object> jwtClaims = decodeJwtClaims(accessToken.getTokenValue());

    // User information
    Map<String, Object> userInfo = new HashMap<>();
    if (oauth2User != null) {
      userInfo.put("name", oauth2User.getAttribute("name"));
      userInfo.put("attributes", oauth2User.getAttributes());
    }

    model.addAttribute("tokenInfo", tokenInfo);
    model.addAttribute("jwtClaims", jwtClaims);
    model.addAttribute("userInfo", userInfo);
    model.addAttribute("clientName", authorizedClient.getClientRegistration().getClientName());

    return "authenticated";
  }

  /**
   * Decode JWT token and extract claims from the payload. Note: This does NOT verify the signature
   * - it's only for display purposes.
   *
   * @param token the JWT token string
   * @return a map of claims from the token payload
   */
  private Map<String, Object> decodeJwtClaims(String token) {
    Map<String, Object> claims = new LinkedHashMap<>();

    try {
      // JWT format: header.payload.signature
      String[] parts = token.split("\\.");

      if (parts.length < 2) {
        claims.put("error", "Invalid JWT format");
        return claims;
      }

      // Decode the payload (second part)
      String payload = parts[1];
      byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
      String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);

      // Parse JSON using Jackson ObjectMapper
      claims =
          objectMapper.readValue(
              decodedPayload, new TypeReference<LinkedHashMap<String, Object>>() {});

      // Format lists/arrays for better display
      for (Map.Entry<String, Object> entry : claims.entrySet()) {
        if (entry.getValue() instanceof java.util.List) {
          java.util.List<?> list = (java.util.List<?>) entry.getValue();
          // Join list elements with commas for cleaner display
          entry.setValue(String.join(", ", list.stream().map(Object::toString).toList()));
        }
      }

    } catch (Exception e) {
      claims.put("error", "Failed to decode JWT: " + e.getMessage());
    }

    return claims;
  }
}
