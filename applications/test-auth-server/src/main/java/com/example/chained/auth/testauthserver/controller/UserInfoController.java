package com.example.chained.auth.testauthserver.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple controller to expose user information for testing purposes.
 */
@RestController
public class UserInfoController {

    @GetMapping("/userinfo")
    public Map<String, Object> userInfo(Authentication authentication) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("sub", authentication.getName());
        userInfo.put("name", authentication.getName());
        userInfo.put("preferred_username", authentication.getName());
        userInfo.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        return userInfo;
    }
}
