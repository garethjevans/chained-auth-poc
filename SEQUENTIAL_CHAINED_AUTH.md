# Sequential Chained Authentication Implementation

## Overview

The auth-adapter now implements **true sequential chained authentication** where users authenticate with **both** test-auth-server and GitHub in sequence, and the resulting JWT token contains claims from **both** authentication sources.

## Authentication Flow

```
┌──────────┐     ┌──────────────┐     ┌─────────────────┐     ┌────────────┐
│ test-app │────>│ auth-adapter │────>│test-auth-server │────>│  GitHub    │
│          │     │              │     │  (step 1)       │     │  (step 2)  │
│          │<────│  JWT with    │<────│                 │<────│            │
│          │     │  combined    │     │                 │     │            │
│          │     │  claims      │     │                 │     │            │
└──────────┘     └──────────────┘     └─────────────────┘     └────────────┘
```

### Step-by-Step Flow

1. **User accesses test-app** → redirected to auth-adapter
2. **Auth-adapter requires authentication** → redirects to test-auth-server login
3. **User logs into test-auth-server** (testuser/password)
4. **ChainedAuthenticationSuccessHandler** intercepts success
   - Stores test-auth-server authentication in session
   - Redirects to GitHub OAuth2 login
5. **User authorizes GitHub** → completes OAuth2 flow
6. **Both authentications complete** → auth-adapter can issue tokens
7. **ChainedAuthTokenCustomizer** merges claims from both sources
8. **JWT token issued** with:
   - Primary identity from test-auth-server (`sub`, `preferred_username`, `name`)
   - Additional data from GitHub (`github_login`, `github_email`, `github_avatar_url`, etc.)

## Implementation Components

### 1. ChainedAuthenticationSuccessHandler

**Purpose**: Orchestrates the sequential authentication flow

**Behavior**:
- After test-auth-server login succeeds → stores auth in session → redirects to GitHub
- After GitHub login succeeds → both auths available → continues normal flow

**Location**: `applications/auth-adapter/src/main/java/com/example/chained/auth/adapter/config/ChainedAuthenticationSuccessHandler.java`

**Key Code**:
```java
if (TEST_AUTH_SERVER_REGISTRATION.equals(registrationId)) {
    // First auth complete - store and redirect to GitHub
    session.setAttribute(TEST_AUTH_SERVER_AUTH_KEY, authentication);
    getRedirectStrategy().sendRedirect(request, response, "/oauth2/authorization/github");
    return;
}
```

### 2. ChainedAuthTokenCustomizer

**Purpose**: Merges claims from both authentication sources into the JWT

**Behavior**:
- Retrieves test-auth-server authentication from session
- Gets current authentication (GitHub) from Security Context
- Adds claims from test-auth-server (primary identity)
- Adds GitHub-specific claims with `github_` prefix

**Location**: `applications/auth-adapter/src/main/java/com/example/chained/auth/adapter/config/ChainedAuthTokenCustomizer.java`

**Claims Added**:

**From test-auth-server (primary identity)**:
- `sub` - User ID from test-auth-server
- `test_auth_server_sub` - Explicit test-auth-server subject
- `preferred_username` - Username
- `name` - Full name

**From GitHub (additional data)**:
- `github_login` - GitHub username
- `github_name` - GitHub display name
- `github_email` - GitHub email
- `github_id` - GitHub user ID
- `github_avatar_url` - Profile picture URL

### 3. AuthorizationServerConfig

**Purpose**: Wires up the custom success handler

**Changes**:
- Constructor injects `ChainedAuthenticationSuccessHandler`
- OAuth2 login configured with `.successHandler(authenticationSuccessHandler)`

## Example JWT Token

After completing both authentications, the JWT payload will look like:

```json
{
  "sub": "testuser",
  "test_auth_server_sub": "testuser",
  "preferred_username": "testuser",
  "name": "Test User",
  "github_login": "johndoe",
  "github_name": "John Doe",
  "github_email": "john@example.com",
  "github_id": 12345678,
  "github_avatar_url": "https://avatars.githubusercontent.com/u/12345678?v=4",
  "authorities": ["SCOPE_openid", "SCOPE_profile", "SCOPE_read"],
  "iss": "http://127.0.0.1:9000",
  "aud": "client",
  "exp": 1738078345,
  "iat": 1738074745,
  "scope": "openid profile read"
}
```

## Testing the Sequential Chain

### Prerequisites

1. **GitHub OAuth App configured** (required for this flow)
   - Go to https://github.com/settings/developers
   - Create new OAuth App
   - Set Authorization callback URL: `http://127.0.0.1:9000/login/oauth2/code/github`
   - Note the Client ID and Client Secret

2. **Update auth-adapter configuration**:
   ```bash
   export GITHUB_CLIENT_ID="your-github-client-id"
   export GITHUB_CLIENT_SECRET="your-github-client-secret"
   ```

### Running the Flow

1. **Start all services**:
   ```bash
   # Terminal 1
   ./gradlew :applications:test-auth-server:bootRun
   
   # Terminal 2  
   ./gradlew :applications:auth-adapter:bootRun
   
   # Terminal 3
   ./gradlew :applications:test-app:bootRun
   ```

2. **Navigate to**: `http://127.0.0.1:8080/authenticated`

3. **First Login - test-auth-server**:
   - You'll be redirected to test-auth-server login page
   - Enter credentials: `testuser` / `password`
   - Click "Sign in"

4. **Automatic Redirect to GitHub**:
   - After test-auth-server login, you'll be automatically redirected to GitHub
   - You may see "Authorize [App Name]" if it's your first time
   - Click "Authorize" to complete GitHub authentication

5. **View Combined Token**:
   - After both logins complete, you'll see the test-app authenticated page
   - The JWT token now contains claims from both test-auth-server and GitHub
   - Check the "Decoded JWT Claims" table to see:
     - `sub` = testuser (from test-auth-server)
     - `github_login` = your GitHub username
     - `github_email` = your GitHub email
     - `github_avatar_url` = your GitHub profile picture

## Configuration Requirements

### GitHub OAuth App Settings

**Application name**: Any name you choose (e.g., "Chained Auth PoC")

**Homepage URL**: `http://127.0.0.1:9000`

**Authorization callback URL**: `http://127.0.0.1:9000/login/oauth2/code/github`

### Environment Variables

Set these before starting auth-adapter:

```bash
export GITHUB_CLIENT_ID="your_actual_client_id"
export GITHUB_CLIENT_SECRET="your_actual_client_secret"
```

Or use application.yml:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: your_actual_client_id
            client-secret: your_actual_client_secret
```

## Troubleshooting

### Issue: "The request was rejected because the URL contained a potentially malicious String '//'"

**Symptom**: Auth-adapter rejects requests with error about double slashes

**Cause**: Spring Security's `StrictHttpFirewall` blocks URLs containing `//` by default. OAuth2 redirects (especially to external providers like GitHub) can contain double slashes in the URL.

**Solution**: This has been fixed by configuring `HttpFirewallConfig` to allow URL-encoded double slashes:

```java
firewall.setAllowUrlEncodedDoubleSlash(true);
```

**If you still see this error**:
1. Ensure you're running the latest version with updated `HttpFirewallConfig`
2. Restart the auth-adapter service after updating
3. Clear browser cache and cookies

### Issue: GitHub Login Not Triggered

**Symptom**: After test-auth-server login, not redirected to GitHub

**Causes**:
1. `ChainedAuthenticationSuccessHandler` not being invoked
2. Success handler not properly registered
3. Session not being maintained

**Solution**:
- Check auth-adapter logs for "ChainedAuthenticationSuccessHandler" invocation
- Verify success handler is injected in `AuthorizationServerConfig`
- Ensure session cookie is being set and maintained

### Issue: "Invalid Client" Error from GitHub

**Symptom**: GitHub returns error about invalid client

**Causes**:
1. Client ID/Secret not set or incorrect
2. Redirect URI mismatch

**Solution**:
- Verify `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` are set
- Check GitHub OAuth app settings match redirect URI exactly
- Ensure using `127.0.0.1` not `localhost`

### Issue: JWT Missing GitHub Claims

**Symptom**: Token has test-auth-server claims but no `github_*` claims

**Causes**:
1. GitHub authentication failed or skipped
2. `ChainedAuthTokenCustomizer` not detecting GitHub auth

**Solution**:
- Verify both logins completed successfully
- Check that current authentication registration ID is "github"
- Add logging to `ChainedAuthTokenCustomizer` to debug

### Issue: Session Lost Between Logins

**Symptom**: After GitHub login, test-auth-server auth not found in session

**Causes**:
1. Session cookies not being maintained
2. Different session for each redirect

**Solution**:
- Check that `AUTH_ADAPTER_SESSION` cookie persists across redirects
- Verify session timeout is sufficient
- Check browser developer tools → Application → Cookies

## Benefits of Sequential Chained Authentication

✅ **Rich User Profile**: Combines identity from test-auth-server with data from GitHub
✅ **Primary Identity Preserved**: The `sub` claim always comes from test-auth-server
✅ **Flexible Claims**: Can add claims from multiple sources without conflicts (using prefixes)
✅ **Auditability**: Can track which external accounts are linked to internal identities
✅ **Enhanced Data**: Access to GitHub profile data (avatar, repos, email, etc.)

## Security Considerations

⚠️ **Session Security**: Test-auth-server authentication is stored in session - ensure proper session management

⚠️ **Claim Trust**: GitHub claims are from a third party - validate and sanitize as needed

⚠️ **Token Size**: More claims = larger JWT - monitor token size

⚠️ **Revocation**: If GitHub access is revoked, the JWT may still be valid until expiration

## Next Steps

Consider implementing:
- Storing the GitHub-test-auth-server mapping in a database
- Allowing users to unlink GitHub accounts
- Refreshing GitHub data periodically
- Adding more OAuth providers (Google, Microsoft, etc.)
