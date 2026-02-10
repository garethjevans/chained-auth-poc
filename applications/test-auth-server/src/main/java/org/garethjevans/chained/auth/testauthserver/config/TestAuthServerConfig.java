package org.garethjevans.chained.auth.testauthserver.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Dummy Authorization Server configuration for end-to-end testing. This provides a simple
 * OAuth2/OIDC server with hardcoded test users.
 */
@Configuration
@EnableWebSecurity(debug = true)
public class TestAuthServerConfig {

  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
      throws Exception {
    http.oauth2AuthorizationServer(
            (authorizationServer) -> {
              http.securityMatcher(authorizationServer.getEndpointsMatcher());
              authorizationServer.oidc(Customizer.withDefaults()); // Enable OpenID Connect 1.0
            })
        .authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
        // Redirect to the login page when not authenticated from the
        // authorization endpoint
        .exceptionHandling(
            (exceptions) ->
                exceptions.defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            (authorize) ->
                authorize
                    .requestMatchers("/actuator/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        // Form login handles the redirect to the login page from the
        // authorization server filter chain
        .formLogin(Customizer.withDefaults());

    return http.build();
  }

  @Bean
  public RegisteredClientRepository registeredClientRepository() {
    // Create a test client that can be used in end-to-end tests
    RegisteredClient testClient =
        RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("test-client")
            .clientSecret("{noop}test-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            // Auth-adapter redirect URIs
            .redirectUri("http://127.0.0.1:9000/login/oauth2/code/test-auth-server")
            .redirectUri("http://127.0.0.1:9000/login/oauth2/code/github")
            // Test-app redirect URIs
            .redirectUri("http://127.0.0.1:8080/login/oauth2/code/auth-adapter")
            .postLogoutRedirectUri("http://127.0.0.1:8080/")
            .postLogoutRedirectUri("http://127.0.0.1:9000/")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope(OidcScopes.EMAIL)
            .scope("read")
            .scope("write")
            .clientSettings(
                ClientSettings.builder()
                    .requireAuthorizationConsent(false) // Skip consent for testing
                    .build())
            .build();

    RegisteredClient fakeGithubClient =
        RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("fake-github-client")
            .clientSecret("{noop}fake-github-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            // .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            // .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            // Auth-adapter redirect URIs
            .redirectUri("http://127.0.0.1:9000/authorize/oauth2/code/github")

            //                    .scope(OidcScopes.OPENID)
            //                    .scope(OidcScopes.PROFILE)
            //                    .scope(OidcScopes.EMAIL)
            .scope("read:user")
            .scope("user:email")
            .clientSettings(
                ClientSettings.builder()
                    .requireAuthorizationConsent(false) // Skip consent for testing
                    .build())
            .tokenSettings(
                TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofMinutes(1))
                    .refreshTokenTimeToLive(Duration.ofMinutes(10))
                    .reuseRefreshTokens(false)
                    .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                    .build())
            .build();

    return new InMemoryRegisteredClientRepository(testClient, fakeGithubClient);
  }

  @Bean
  public UserDetailsService userDetailsService() {
    // Create test users for end-to-end testing
    UserDetails testUser =
        User.builder().username("testuser").password("{noop}password").roles("USER").build();

    UserDetails adminUser =
        User.builder().username("admin").password("{noop}admin").roles("USER", "ADMIN").build();

    return new InMemoryUserDetailsManager(testUser, adminUser);
  }

  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    KeyPair keyPair = generateRsaKey();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    RSAKey rsaKey =
        new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return new ImmutableJWKSet<>(jwkSet);
  }

  private static KeyPair generateRsaKey() {
    KeyPair keyPair;
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      keyPair = keyPairGenerator.generateKeyPair();
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
    return keyPair;
  }

  @Bean
  public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return NimbusJwtDecoder.withJwkSetUri("http://127.0.0.1:9001/oauth2/jwks").build();
  }

  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder().issuer("http://127.0.0.1:9001").build();
  }
}
