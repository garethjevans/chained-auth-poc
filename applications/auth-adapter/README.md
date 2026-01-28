# Auth Adapter

The auth-adapter is a Spring Authorization Server that implements **chained authentication**, requiring users to first authenticate with the test-auth-server before accessing the authorization server functionality.

## Chained Authentication

The auth-adapter now requires primary authentication via the **test-auth-server** before issuing JWT tokens. This creates a chained authentication flow:

1. User attempts to access auth-adapter
2. Redirected to test-auth-server for primary authentication
3. After successful login with test-auth-server, user returns to auth-adapter
4. Auth-adapter issues JWT tokens containing the `sub` claim from test-auth-server

See [CHAINED_AUTHENTICATION.md](../../CHAINED_AUTHENTICATION.md) for detailed information about the chained authentication flow.

## Quick Start

### Running with Test-Auth-Server (Recommended)

**Terminal 1: Start test-auth-server**
```bash
./gradlew :applications:test-auth-server:bootRun
```

**Terminal 2: Start auth-adapter**
```bash
./gradlew :applications:auth-adapter:bootRun
```

The auth-adapter will use test-auth-server for authentication. Test users:
- Username: `testuser`, Password: `password`
- Username: `admin`, Password: `admin`

## Configuration

### Test-Auth-Server Integration

The auth-adapter is pre-configured to use test-auth-server as the primary OAuth2 provider:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
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

### JWT Token Claims

Tokens issued by auth-adapter include claims from test-auth-server:
- `sub`: Subject from test-auth-server (e.g., "testuser")
- `test_auth_server_sub`: Explicit test-auth-server subject
- `preferred_username`: Username from test-auth-server
- `name`: Name from test-auth-server

## GitHub OAuth Application Setup (Optional)

To use GitHub as a secondary authentication source, create a GitHub OAuth application:

1. Go to GitHub Settings → Developer settings → OAuth Apps
2. Click "New OAuth App"
3. Fill in the application details:
   - **Application name**: Your application name
   - **Homepage URL**: `http://127.0.0.1:9000`
   - **Authorization callback URL**: `http://127.0.0.1:9000/login/oauth2/code/github`
4. Click "Register application"
5. Copy the **Client ID** and generate a **Client Secret**

## Environment Variables

Set the following environment variables if using GitHub:

```bash
export GITHUB_CLIENT_ID=your-github-client-id
export GITHUB_CLIENT_SECRET=your-github-client-secret
```

Or create an `application-local.yml` file in `src/main/resources/`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: your-github-client-id
            client-secret: your-github-client-secret
```

Then run with the local profile:
```bash
./gradlew :applications:auth-adapter:bootRun --args='--spring.profiles.active=local'
```

## Testing

The auth-adapter runs on port 9000 by default.

**Endpoints:**
- Home: `http://127.0.0.1:9000/`
- User info: `http://127.0.0.1:9000/user`
- Health check: `http://127.0.0.1:9000/actuator/health`
- OAuth2 authorization: `http://127.0.0.1:9000/oauth2/authorize`
- OIDC configuration: `http://127.0.0.1:9000/.well-known/oauth-authorization-server`
- JWK Set: `http://127.0.0.1:9000/oauth2/jwks`

## OAuth2 Client Configuration

Other applications can use this authorization server by configuring their `application.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          auth-adapter:
            client-id: client
            client-secret: secret
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope:
              - openid
              - profile
              - read
        provider:
          auth-adapter:
            issuer-uri: http://127.0.0.1:9000
```

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

## Features

- **Chained Authentication**: Requires test-auth-server authentication first
- **OAuth2 Authorization Server**: Implements OAuth2 authorization code flow
- **OpenID Connect**: Full OIDC support with ID tokens
- **JWT Tokens**: Issues JWT access tokens with custom claims
- **Token Customization**: Includes claims from upstream authentication providers
- **Session Management**: HTTP-compatible session cookies for development

## Security Notes

⚠️ **This configuration is for development/testing only**

- Session cookies work over HTTP (not HTTPS)
- Test-auth-server uses hardcoded credentials
- In-memory storage only (no persistence)
- Not suitable for production use

For production, you should:
- Enable HTTPS and secure cookies
- Use a real identity provider
- Implement persistent storage for tokens and sessions
- Add proper security hardening
