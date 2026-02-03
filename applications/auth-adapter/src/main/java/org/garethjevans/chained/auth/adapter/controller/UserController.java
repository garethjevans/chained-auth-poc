package org.garethjevans.chained.auth.adapter.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

  @GetMapping("/user")
  public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
    Map<String, Object> userInfo = new HashMap<>();
    if (principal != null) {
      userInfo.put("name", principal.getAttribute("name"));
      userInfo.put("login", principal.getAttribute("login"));
      userInfo.put("email", principal.getAttribute("email"));
      userInfo.put("avatar_url", principal.getAttribute("avatar_url"));
    }
    return userInfo;
  }

  @GetMapping("/")
  public String index(Authentication authentication) {
    if (authentication != null && authentication.isAuthenticated()) {
      return "Welcome! You are authenticated as: " + authentication.getName();
    }
    return "Welcome to Auth Adapter!";
  }
}
