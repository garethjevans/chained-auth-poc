# Chained Authentication with Auth-Adapter

This document explains how the auth-adapter implements chained authentication, requiring users to first authenticate with the test-auth-server before accessing GitHub authentication.

## Overview

The auth-adapter now implements a **chained authentication** flow where:

1. Users must first authenticate with the **test-auth-server** (primary authentication)
2. The test-auth-server validates credentials (testuser/password or admin/admin)
3. Once authenticated with test-auth-server, users can access the auth-adapter
4. The auth-adapter issues JWT tokens containing the `sub` claim from test-auth-server
5. Optional: Users can also authenticate with GitHub for additional data

## Architecture

```
┌─────────────┐      ┌──────────────────┐      ┌─────────────────┐
│  test-app   │─────>│  auth-adapter    │─────>│test-auth-server │
│ (port 8080) │      │  (port 9000)     │      │  (port 9001)    │
└─────────────┘      └──────────────────┘      └─────────────────┘
                              │
                              └──────────────>┌─────────────────┐
                                               │     GitHub      │
                                               │    (optional)   │
                                               └─────────────────┘
```

## Authentication Flow

### Step 1: User Accesses Test App
1. User navigates to `http://127.0.0.1:8080/authenticated`
2. Test-app redirects to auth-adapter for authentication

### Step 2: Auth-Adapter Requires Test-Auth-Server Login
1. Auth-adapter redirects user to test-auth-server login page
2. User enters credentials:
   - Username: `testuser` / Password: `password` (USER role)
   - OR Username: `admin` / Password: `admin` (USER + ADMIN roles)
3. Test-auth-server validates credentials and issues OIDC token

### Step 3: Auth-Adapter Issues JWT Token
1. Auth-adapter receives OIDC authentication from test-auth-server
2. Custom token customizer extracts the `sub` claim from test-auth-server
3. Auth-adapter issues JWT token with:
   - `sub`: The subject from test-auth-server (e.g., "testuser")
   - `test_auth_server_sub`: Explicit claim with test-auth-server subject
   - `preferred_username`: Username from test-auth-server
   - `name`: Name from test-auth-server
   - Standard OIDC claims (iss, aud, exp, etc.)

### Step 4: Test App Receives Token
1. Test-app receives the JWT token from auth-adapter
2. Token contains the primary identity from test-auth-server
3. Test-app can verify and use the token for authorization

## Configuration

### Auth-Adapter Configuration

The auth-adapter is configured to use test-auth-server as the primary OAuth2 client:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          # Primary authentication
          test-auth-server:
            client-id: test-client
            client-secret: test-secret
            authorization-grant-type: authorization_code
            scope:
              - openid
              - profile
        provider:
          test-auth-server:
            issuer-uri: http://127.0.0.1:9001
```

### Security Filter Chain

The auth-adapter's security configuration redirects unauthenticated users to test-auth-server:

```java
.exceptionHandling((exceptions) ->
    exceptions.defaultAuthenticationEntryPointFor(
        new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/test-auth-server"),
        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
```

### Token Customization

The `ChainedAuthTokenCustomizer` extracts claims from the test-auth-server OIDC token and includes them in the JWT issued by auth-adapter:

```java
@Component
public class ChainedAuthTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {
  @Override
  public void customize(JwtEncodingContext context) {
    // Extract sub from test-auth-server OIDC user
    String testAuthServerSub = oidcUser.getSubject();
    
    // Add to JWT token
    context.getClaims()
        .claim("test_auth_server_sub", testAuthServerSub)
        .claim("sub", testAuthServerSub); // Override sub with test-auth-server sub
  }
}
```

## JWT Token Claims

The JWT token issued by auth-adapter includes:

| Claim | Description | Example |
|-------|-------------|---------|
| `sub` | Subject from test-auth-server | `"testuser"` |
| `test_auth_server_sub` | Explicit test-auth-server subject | `"testuser"` |
| `preferred_username` | Username from test-auth-server | `"testuser"` |
| `name` | Name from test-auth-server | `"testuser"` |
| `iss` | Issuer (auth-adapter) | `"http://127.0.0.1:9000"` |
| `aud` | Audience (client ID) | `"client"` |
| `exp` | Expiration time | Unix timestamp |
| `iat` | Issued at time | Unix timestamp |
| `authorities` | User authorities/roles | `["ROLE_USER"]` |

## Running the Chained Authentication Flow

### Prerequisites

1. Java 21 installed
2. All three applications built: `./gradlew build`

### Start All Services

**Terminal 1: Start test-auth-server**
```bash
./gradlew :applications:test-auth-server:bootRun
```
Wait for: `Started TestAuthServerApplication in X seconds (process running for Y)`

**Terminal 2: Start auth-adapter**
```bash
./gradlew :applications:auth-adapter:bootRun
```
Wait for: `Started AuthAdapterApplication in X seconds (process running for Y)`

**Terminal 3: Start test-app**
```bash
./gradlew :applications:test-app:bootRun
```
Wait for: `Started TestAppApplication in X seconds (process running for Y)`

### Test the Flow

1. Open browser to `http://127.0.0.1:8080/authenticated`
2. You'll be redirected to auth-adapter
3. Auth-adapter redirects to test-auth-server login page
4. Enter credentials:
   - Username: `testuser`
   - Password: `password`
5. After successful login, you'll be redirected back to test-app
6. The test-app displays your JWT token and user information
7. Verify the token contains `sub: testuser` from test-auth-server

## Verifying Token Claims

### Using JWT.io

1. Copy the access token from the test-app page
2. Navigate to https://jwt.io
3. Paste the token in the "Encoded" section
4. Verify the decoded payload contains:
   ```json
   {
     "sub": "testuser",
     "test_auth_server_sub": "testuser",
     "preferred_username": "testuser",
     "name": "testuser",
     "iss": "http://127.0.0.1:9000",
     ...
   }
   ```

### Using curl

```bash
# Get the token (after completing the flow in browser)
TOKEN="<your-access-token>"

# Decode the token payload (requires jq)
echo $TOKEN | cut -d'.' -f2 | base64 -d | jq
```

## Test Users

The test-auth-server provides two pre-configured users:

| Username | Password | Roles | Description |
|----------|----------|-------|-------------|
| testuser | password | USER | Regular user for testing |
| admin | admin | USER, ADMIN | Admin user for testing |

## Security Considerations

⚠️ **For Testing Only**

This configuration is designed for local development and testing:

- Passwords are hardcoded and simple
- Session cookies work over HTTP (not HTTPS)
- No persistent storage (in-memory only)
- RSA keys generated at startup (not persisted)
- Authorization consent is disabled

**Do not use in production!**

## Troubleshooting

### Issue: "The request was rejected because the URL contained a potentially malicious String ';'"

**Symptom:** Error message: `The request was rejected because the URL contained a potentially malicious String ";"`

**Root Cause:** Spring Security's `StrictHttpFirewall` blocks URLs containing semicolons by default. OAuth2 authorization requests can contain semicolons in:
- JSESSIONID cookies in URLs
- Encoded scope parameters
- Other OAuth2 state parameters

**Solution:** This has been fixed by configuring a custom `HttpFirewall` in both auth-adapter and test-auth-server that allows semicolons while maintaining other security restrictions.

**Configuration Applied:**
```java
@Bean
public HttpFirewall allowSemicolonHttpFirewall() {
  StrictHttpFirewall firewall = new StrictHttpFirewall();
  firewall.setAllowSemicolon(true);
  // Other security settings remain strict
  return firewall;
}
```

**If you still see this error:**
1. Ensure you're running the latest version with `HttpFirewallConfig`
2. Restart all services after updating
3. Clear browser cookies and cache

### Issue: Redirect Loop

**Symptom:** Browser keeps redirecting between services

**Solution:** 
- Clear browser cookies for all three services
- Ensure all services are running
- Use `127.0.0.1` consistently (not mixing with `localhost`)

### Issue: "invalid_request" - OAuth 2.0 Parameter: redirect_uri

**Symptom:** Error message: `Authorization request failed: [invalid_request] OAuth 2.0 Parameter: redirect_uri`

**Root Cause:** The redirect URI sent in the OAuth2 request doesn't match what's registered in the authorization server.

**Common Causes:**
1. **Mixing `localhost` and `127.0.0.1`**: Browser accessing via `localhost` but redirect URI uses `127.0.0.1` (or vice versa)
2. **Port mismatch**: Service running on different port than configured
3. **Path mismatch**: Typo in the redirect path

**Solution:**
1. **Always use `127.0.0.1` consistently** - Update all URLs to use `127.0.0.1`:
   ```yaml
   # auth-adapter application.yml
   redirect-uri: "http://127.0.0.1:9000/login/oauth2/code/test-auth-server"
   ```

2. **Verify registered URIs match** - Check test-auth-server configuration:
   ```java
   .redirectUri("http://127.0.0.1:9000/login/oauth2/code/test-auth-server")
   ```

3. **Access services via `127.0.0.1`**:
   - Test-app: `http://127.0.0.1:8080`
   - Auth-adapter: `http://127.0.0.1:9000`
   - Test-auth-server: `http://127.0.0.1:9001`

4. **Clear browser cache and cookies** after configuration changes

5. **Check logs** for the actual redirect URI being sent:
   ```bash
   # Look for the redirect_uri parameter in auth-adapter logs
   # with logging.level.org.springframework.security.oauth2: TRACE
   ```

**Verification:**
```bash
# Check the authorization request parameters
# The redirect_uri parameter should exactly match a registered URI
curl -v 'http://127.0.0.1:9001/oauth2/authorize?client_id=test-client&redirect_uri=http://127.0.0.1:9000/login/oauth2/code/test-auth-server&response_type=code&scope=openid%20profile'
```

### Issue: Token Doesn't Contain Test-Auth-Server Sub

**Symptom:** JWT token's `sub` claim is not from test-auth-server

**Solution:**
- Check that you logged in via test-auth-server (not GitHub directly)
- Verify `ChainedAuthTokenCustomizer` is being invoked
- Check auth-adapter logs for token customization

### Issue: Cannot Connect to Test-Auth-Server

**Symptom:** Auth-adapter shows connection errors

**Solution:**
- Ensure test-auth-server is running on port 9001
- Check `http://127.0.0.1:9001/.well-known/openid-configuration` is accessible
- Verify firewall is not blocking port 9001

## Additional Resources

- [Spring Authorization Server Documentation](https://docs.spring.io/spring-authorization-server/reference/)
- [OAuth 2.0 Authorization Code Flow](https://datatracker.ietf.org/doc/html/rfc6749#section-4.1)
- [OpenID Connect Core](https://openid.net/specs/openid-connect-core-1_0.html)
