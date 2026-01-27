package com.example.chained.auth.testapp.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Controller
public class HomeController {

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
            long secondsUntilExpiry = accessToken.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond();
            tokenInfo.put("expiresIn", secondsUntilExpiry);
        }

        // User information
        Map<String, Object> userInfo = new HashMap<>();
        if (oauth2User != null) {
            userInfo.put("name", oauth2User.getAttribute("name"));
            userInfo.put("attributes", oauth2User.getAttributes());
        }

        model.addAttribute("tokenInfo", tokenInfo);
        model.addAttribute("userInfo", userInfo);
        model.addAttribute("clientName", authorizedClient.getClientRegistration().getClientName());
        
        return "authenticated";
    }
}
