package org.garethjevans.chained.auth.testauthserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

/**
 * Configures the HTTP firewall to allow OAuth2-specific URL patterns. The default
 * StrictHttpFirewall blocks semicolons which can appear in OAuth2 authorization requests (e.g., in
 * JSESSIONID or encoded parameters).
 */
@Configuration
public class HttpFirewallConfig {

  @Bean
  public HttpFirewall allowSemicolonHttpFirewall() {
    StrictHttpFirewall firewall = new StrictHttpFirewall();

    // Allow semicolons in URLs (needed for OAuth2 flows with JSESSIONID)
    firewall.setAllowSemicolon(true);

    // Keep other strict security settings enabled
    firewall.setAllowUrlEncodedSlash(false);
    firewall.setAllowBackSlash(false);
    firewall.setAllowUrlEncodedPercent(false);
    firewall.setAllowUrlEncodedPeriod(false);

    return firewall;
  }
}
