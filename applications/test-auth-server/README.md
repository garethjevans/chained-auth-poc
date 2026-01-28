# Test Auth Server

A dummy implementation of an OAuth2/OIDC Authorization Server designed specifically for end-to-end testing.

## Overview

This module provides a simple, self-contained authorization server that can be used in automated tests without requiring external dependencies or real OAuth2 providers like GitHub. It implements the OAuth2 Authorization Code flow with OpenID Connect support.

## Features

- **OAuth2 Authorization Server** with OIDC support
- **In-memory user store** with pre-configured test users
- **In-memory client repository** with a test client
- **Form-based login** for test users
- **No external dependencies** - completely self-contained
- **Simple configuration** - ready to use out of the box

## Configuration

- **Port**: 9001 (configurable via `server.port`)
- **Issuer URI**: `http://127.0.0.1:9001`

### Test Users

Two test users are pre-configured:

1. **Regular User**
   - Username: `testuser`
   - Password: `password`
   - Roles: `USER`

2. **Admin User**
   - Username: `admin`
   - Password: `admin`
   - Roles: `USER`, `ADMIN`

### Test Client

A pre-configured OAuth2 client for testing:

- **Client ID**: `test-client`
- **Client Secret**: `test-secret`
- **Redirect URIs**: 
  - `http://127.0.0.1:8080/login/oauth2/code/test-auth-server`
  - `http://localhost:8080/login/oauth2/code/test-auth-server`
- **Scopes**: `openid`, `profile`, `email`, `read`, `write`
- **Grant Types**: `authorization_code`, `refresh_token`, `client_credentials`
- **Authorization Consent**: Disabled (for easier testing)

## Running the Server

### Using Gradle

```bash
./gradlew :applications:test-auth-server:bootRun
```

### Using the JAR

```bash
./gradlew :applications:test-auth-server:bootJar
java -jar applications/test-auth-server/build/libs/test-auth-server.jar
```

## Endpoints

The server exposes standard OAuth2/OIDC endpoints:

- **Authorization Endpoint**: `http://127.0.0.1:9001/oauth2/authorize`
- **Token Endpoint**: `http://127.0.0.1:9001/oauth2/token`
- **JWK Set**: `http://127.0.0.1:9001/oauth2/jwks`
- **User Info**: `http://127.0.0.1:9001/userinfo`
- **OIDC Configuration**: `http://127.0.0.1:9001/.well-known/openid-configuration`
- **Login**: `http://127.0.0.1:9001/login`

## Using in Tests

### Integration Tests

You can use this server in integration tests by starting it as part of your test setup:

```java
@SpringBootTest(
    classes = TestAuthServerApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
class MyIntegrationTest {
    // Your test code here
}
```

### End-to-End Tests

For E2E tests with Selenium or similar tools:

1. Start the test-auth-server on port 9001
2. Configure your application under test to use:
   - Issuer URI: `http://127.0.0.1:9001`
   - Client ID: `test-client`
   - Client Secret: `test-secret`
3. Run your E2E tests
4. The test users can be used to simulate different user scenarios

### Example Client Configuration

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
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope:
              - openid
              - profile
              - read
        provider:
          test-auth-server:
            issuer-uri: http://127.0.0.1:9001
```

## Architecture

The test auth server uses Spring Authorization Server with:

- **In-memory user management** via `UserDetailsService`
- **In-memory client registration** via `RegisteredClientRepository`
- **RSA key pair generation** for JWT signing (generated at startup)
- **Form-based authentication** for test users
- **Standard OAuth2/OIDC flows** without any shortcuts or modifications

## Security Notes

⚠️ **This server is for testing only!**

- Users and passwords are hardcoded
- No password encryption (uses `{noop}` prefix)
- No persistent storage
- Session cookies work over HTTP (not HTTPS)
- RSA keys are generated at startup and not persisted
- Not suitable for production use

## Development

### Building

```bash
./gradlew :applications:test-auth-server:build
```

### Testing

```bash
./gradlew :applications:test-auth-server:test
```

## Troubleshooting

### Port Already in Use

If port 9001 is already in use, you can change it by setting:

```bash
SERVER_PORT=9002 ./gradlew :applications:test-auth-server:bootRun
```

Or in `application.yml`:

```yaml
server:
  port: 9002
```

Make sure to update the issuer URI accordingly.

### Connection Issues

Ensure you're using `127.0.0.1` consistently (not mixing with `localhost`) as OAuth2 redirect URIs must match exactly.
