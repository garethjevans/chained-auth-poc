# GitHub Access Token in JWT Claims

## Overview

The `ChainedAuthTokenCustomizer` now includes the raw GitHub OAuth2 access token as a claim (`github_access_token`) in the JWT issued to the test-app. This allows the test-app to make authenticated API calls to GitHub on behalf of the user.

## Changes Made

### ChainedAuthTokenCustomizer

**Added**:
1. **Constructor injection** of `OAuth2AuthorizedClientService`
2. **`getGitHubAccessToken()` method** - Retrieves the GitHub access token
3. **`github_access_token` claim** - Added to JWT when GitHub authentication is present

### How It Works

1. **User completes chained auth** (test-auth-server → GitHub)
2. **GitHub OAuth2 flow stores access token** in `OAuth2AuthorizedClientService`
3. **Token customizer retrieves access token** using:
   - Client registration ID: `"github"`
   - Principal name from `OAuth2AuthenticationToken`
4. **Access token added to JWT** as `github_access_token` claim
5. **Test-app receives JWT** with GitHub access token included

## JWT Token Structure

### Before (without GitHub access token)

```json
{
  "sub": "testuser",
  "test_auth_server_sub": "testuser",
  "preferred_username": "testuser",
  "name": "Test User",
  "github_login": "johndoe",
  "github_name": "John Doe",
  "github_email": "john@example.com",
  "github_id": 12345,
  "github_avatar_url": "https://avatars.githubusercontent.com/u/12345?v=4",
  "iss": "http://127.0.0.1:9000",
  "aud": "client",
  "exp": 1738078345,
  "iat": 1738074745
}
```

### After (with GitHub access token)

```json
{
  "sub": "testuser",
  "test_auth_server_sub": "testuser",
  "preferred_username": "testuser",
  "name": "Test User",
  "github_login": "johndoe",
  "github_name": "John Doe",
  "github_email": "john@example.com",
  "github_id": 12345,
  "github_avatar_url": "https://avatars.githubusercontent.com/u/12345?v=4",
  "github_access_token": "gho_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "iss": "http://127.0.0.1:9000",
  "aud": "client",
  "exp": 1738078345,
  "iat": 1738074745
}
```

## Using the GitHub Access Token

### In test-app

The test-app can extract the `github_access_token` claim from the JWT and use it to make authenticated GitHub API calls:

```java
// Extract github_access_token from JWT claims
String githubAccessToken = jwt.getClaim("github_access_token");

// Use it to call GitHub API
HttpHeaders headers = new HttpHeaders();
headers.setBearerAuth(githubAccessToken);
headers.set("Accept", "application/vnd.github+json");

// Example: Get user's repositories
RestTemplate restTemplate = new RestTemplate();
HttpEntity<String> entity = new HttpEntity<>(headers);
ResponseEntity<String> response = restTemplate.exchange(
    "https://api.github.com/user/repos",
    HttpMethod.GET,
    entity,
    String.class
);
```

### Example GitHub API Calls

With the access token, the test-app can call any GitHub API that the user has authorized:

**Get user profile**:
```bash
curl -H "Authorization: Bearer ${GITHUB_ACCESS_TOKEN}" \
     -H "Accept: application/vnd.github+json" \
     https://api.github.com/user
```

**Get user repositories**:
```bash
curl -H "Authorization: Bearer ${GITHUB_ACCESS_TOKEN}" \
     -H "Accept: application/vnd.github+json" \
     https://api.github.com/user/repos
```

**Get user organizations**:
```bash
curl -H "Authorization: Bearer ${GITHUB_ACCESS_TOKEN}" \
     -H "Accept: application/vnd.github+json" \
     https://api.github.com/user/orgs
```

## Logging

The feature includes comprehensive logging:

```log
INFO  ChainedAuthTokenCustomizer - Adding GitHub claims to JWT
DEBUG ChainedAuthTokenCustomizer - Processing GitHub claims
DEBUG ChainedAuthTokenCustomizer - Attempting to retrieve GitHub access token
DEBUG ChainedAuthTokenCustomizer - Retrieved GitHub access token: type=Bearer, expires=2024-01-30T12:00:00Z
INFO  ChainedAuthTokenCustomizer - Adding github_access_token to JWT (length: 40)
INFO  ChainedAuthTokenCustomizer - Successfully added 6 GitHub claims: ..., access_token=[present]
```

**If access token not available**:
```log
WARN  ChainedAuthTokenCustomizer - No OAuth2AuthorizedClient found for GitHub
WARN  ChainedAuthTokenCustomizer - GitHub access token not available - will not be included in JWT
```

## Security Considerations

### ⚠️ Important Security Notes

**1. Token Sensitivity**
- The GitHub access token is a sensitive credential
- Anyone with this token can make API calls on behalf of the user
- Treat it with the same security as a password

**2. JWT Size**
- Adding the access token increases JWT size (~40-100 characters)
- Monitor JWT size to ensure it stays within limits (typically 4KB for cookies)

**3. Token Expiration**
- GitHub access tokens can expire or be revoked
- JWT expiration should be shorter than GitHub token expiration
- Consider implementing refresh logic

**4. Scope Limitations**
- Access token only has permissions for scopes granted during OAuth flow
- Current scopes: `user:email`, `read:user`
- Additional scopes require updating OAuth2 client registration

**5. Token Leakage**
- Be careful not to log the full token value
- Don't expose JWT in URLs or unsecured storage
- Use HTTPS in production

**6. Revocation**
- If GitHub token is revoked, JWT may still be valid
- Implement token validation/verification in test-app
- Consider shorter JWT expiration times

### Best Practices

✅ **DO**:
- Use the token only for intended GitHub API calls
- Validate token before each use
- Log token usage for audit purposes
- Implement error handling for expired/revoked tokens
- Use HTTPS for all GitHub API calls

❌ **DON'T**:
- Log the full token value
- Store token in browser localStorage (use httpOnly cookies)
- Share token with third parties
- Use token beyond its intended scope
- Expose JWT in URLs or client-side code

## Token Refresh

GitHub access tokens may need to be refreshed:

### Current Behavior
- Access token is retrieved once during JWT creation
- Token is embedded in JWT and reused until JWT expires

### Future Enhancement
Consider implementing:
1. **Token refresh flow** - Refresh GitHub token when expired
2. **Token validation** - Verify GitHub token is still valid
3. **Dynamic token retrieval** - Fetch fresh token for each request

## Troubleshooting

### Issue: `github_access_token` not in JWT

**Log Pattern**:
```
WARN  ChainedAuthTokenCustomizer - GitHub access token not available - will not be included in JWT
```

**Possible Causes**:
1. GitHub authentication not completed
2. `OAuth2AuthorizedClientService` not storing token
3. Client registration ID mismatch

**Solutions**:
- Verify GitHub OAuth2 flow completed successfully
- Check that `authorizedClientService` is injected
- Confirm registration ID is "github"

### Issue: Invalid GitHub Token

**Symptom**: GitHub API returns 401 Unauthorized

**Possible Causes**:
1. Token expired
2. Token revoked by user
3. Insufficient scopes for API call

**Solutions**:
- Check token expiration in JWT
- Verify user hasn't revoked access in GitHub settings
- Request additional scopes if needed

### Issue: JWT Too Large

**Symptom**: Cookie errors or JWT rejected

**Cause**: GitHub access token increases JWT size

**Solutions**:
- Consider storing GitHub token separately (not in JWT)
- Use shorter JWT expiration times
- Compress JWT if possible

## Configuration

No additional configuration is required. The feature automatically activates when:

1. ✅ GitHub OAuth2 client is configured in `application.yml`
2. ✅ User completes GitHub authentication
3. ✅ `OAuth2AuthorizedClientService` is available (auto-configured by Spring)

## Testing

### Manual Testing

1. **Complete chained auth flow**:
   - Login to test-auth-server
   - Authorize GitHub
   
2. **View JWT token** on `/authenticated` page in test-app

3. **Check for `github_access_token` claim** in decoded JWT table

4. **Test GitHub API call**:
   ```bash
   # Extract token from JWT (decode JWT first)
   TOKEN="<github_access_token_from_jwt>"
   
   # Test API call
   curl -H "Authorization: Bearer $TOKEN" \
        -H "Accept: application/vnd.github+json" \
        https://api.github.com/user
   ```

### Expected Result

✅ JWT contains `github_access_token` claim
✅ GitHub API accepts the token
✅ API returns user data successfully

## API Scope Requirements

The GitHub access token has limited scopes based on OAuth2 configuration:

**Current Scopes** (from `application.yml`):
- `user:email` - Access to user's email addresses
- `read:user` - Read user profile data

**Available APIs** with current scopes:
- ✅ Get user profile: `/user`
- ✅ Get user emails: `/user/emails`
- ✅ Get public repositories: `/user/repos?visibility=public`
- ❌ Create repository: (requires `public_repo` or `repo`)
- ❌ Access private repos: (requires `repo`)
- ❌ Manage organizations: (requires `admin:org`)

**To add more scopes**, update `application.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            scope:
              - user:email
              - read:user
              - repo              # Add repository access
              - admin:org         # Add organization access
```

## Benefits

✅ **Enables GitHub API Integration** - Test-app can make authenticated GitHub API calls
✅ **Unified Authentication** - Single JWT contains both identities and GitHub access
✅ **Seamless User Experience** - No separate GitHub token management needed
✅ **Audit Trail** - Token usage tracked via JWT claims
✅ **Flexible** - Test-app decides how to use GitHub access token

## Limitations

⚠️ **Token Lifetime** - GitHub token may expire before JWT
⚠️ **No Refresh** - Token not automatically refreshed
⚠️ **Size Impact** - Increases JWT size
⚠️ **Security Risk** - Sensitive token embedded in JWT
⚠️ **Scope Bound** - Limited to granted OAuth2 scopes

## Alternative Approaches

Consider these alternatives based on your security requirements:

**1. Store GitHub token separately**
- JWT contains only a reference/ID
- Look up actual token from secure storage
- Better security, more complex implementation

**2. Use GitHub App installation tokens**
- More granular permissions
- Better for organization-level access
- Requires GitHub App setup

**3. Proxy GitHub API calls through auth-adapter**
- Test-app doesn't handle tokens directly
- Auth-adapter makes GitHub API calls
- Better security, adds latency

## Related Documentation

- [GitHub REST API Documentation](https://docs.github.com/en/rest)
- [GitHub OAuth Scopes](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/scopes-for-oauth-apps)
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
