package org.garethjevans.chained.auth.testapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity(debug = true)
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(
                        "/", "/favicon.ico", "/error", "/webjars/**", "/actuator/**", "/logout")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2Login(oauth2 -> oauth2.defaultSuccessUrl("/authenticated", true))
        .oauth2Client(oauth2 -> {})
        .logout(
            logout ->
                logout
                    .logoutSuccessUrl("/")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies(
                        "JSESSIONID",
                        "TEST_APP_SESSION",
                        "TEST_AUTH_SERVER_SESSION",
                        "AUTH_ADAPTER_SESSION"));

    return http.build();
  }
}
