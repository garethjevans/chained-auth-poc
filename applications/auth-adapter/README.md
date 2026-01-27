# Auth Adapter Configuration

## GitHub OAuth Application Setup

To use the auth-adapter module with GitHub authentication, you need to create a GitHub OAuth application:

1. Go to GitHub Settings → Developer settings → OAuth Apps
2. Click "New OAuth App"
3. Fill in the application details:
   - **Application name**: Your application name
   - **Homepage URL**: `http://localhost:9000`
   - **Authorization callback URL**: `http://localhost:9000/login/oauth2/code/github`
4. Click "Register application"
5. Copy the **Client ID** and generate a **Client Secret**

## Environment Variables

Set the following environment variables before running the auth-adapter:

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
- Home: `http://localhost:9000/`
- User info: `http://localhost:9000/user`
- Health check: `http://localhost:9000/actuator/health`
- OAuth2 authorization: `http://localhost:9000/oauth2/authorize`
- OIDC configuration: `http://localhost:9000/.well-known/oauth-authorization-server`

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
            redirect-uri: http://127.0.0.1:8080/login/oauth2/code/auth-adapter
            scope: openid,profile,read
        provider:
          auth-adapter:
            issuer-uri: http://localhost:9000
```

**Note**: Update the `redirect-uri` to match your application's port and callback path.
