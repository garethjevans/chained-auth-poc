# ChainedAuthTokenCustomizer Logging Guide

## Overview

Comprehensive logging has been added to `ChainedAuthTokenCustomizer` to help debug the sequential chained authentication flow and JWT token claim customization.

## Log Levels

The logging uses SLF4J with the following levels:

- **INFO** - High-level flow and successful operations
- **DEBUG** - Detailed information about claims processing
- **WARN** - Missing data or skipped operations
- **ERROR** - Critical issues that prevent proper token customization

## Log Output Examples

### Successful Chained Authentication (test-auth-server → GitHub)

```
INFO  ChainedAuthTokenCustomizer - === Starting JWT Token Customization ===
DEBUG ChainedAuthTokenCustomizer - Current authentication: type=OAuth2AuthenticationToken, principal=githubuser
INFO  ChainedAuthTokenCustomizer - Found test-auth-server user in session: testuser
INFO  ChainedAuthTokenCustomizer - Current OAuth2 registration: github
DEBUG ChainedAuthTokenCustomizer - Current user attributes: {login=githubuser, id=12345, email=user@example.com...}
INFO  ChainedAuthTokenCustomizer - Adding test-auth-server claims to JWT
DEBUG ChainedAuthTokenCustomizer - Processing test-auth-server claims
DEBUG ChainedAuthTokenCustomizer - Extracted sub from OidcUser: testuser
INFO  ChainedAuthTokenCustomizer - Setting primary subject (sub) to: testuser
DEBUG ChainedAuthTokenCustomizer - Adding preferred_username: testuser
DEBUG ChainedAuthTokenCustomizer - Adding name: testuser
INFO  ChainedAuthTokenCustomizer - Successfully added test-auth-server claims: sub=testuser, preferred_username=testuser, name=testuser
INFO  ChainedAuthTokenCustomizer - Adding GitHub claims to JWT
DEBUG ChainedAuthTokenCustomizer - Processing GitHub claims
DEBUG ChainedAuthTokenCustomizer - Adding github_login: githubuser
DEBUG ChainedAuthTokenCustomizer - Adding github_name: GitHub User
DEBUG ChainedAuthTokenCustomizer - Adding github_email: user@example.com
DEBUG ChainedAuthTokenCustomizer - Adding github_id: 12345
DEBUG ChainedAuthTokenCustomizer - Adding github_avatar_url: [present]
INFO  ChainedAuthTokenCustomizer - Successfully added 5 GitHub claims: login=githubuser, name=GitHub User, email=[present], id=12345, avatar=[present]
DEBUG ChainedAuthTokenCustomizer - Adding authorities: [SCOPE_openid, SCOPE_profile, SCOPE_read:user, SCOPE_user:email]
INFO  ChainedAuthTokenCustomizer - === Completed JWT Token Customization ===
```

### Only test-auth-server (GitHub not configured/used)

```
INFO  ChainedAuthTokenCustomizer - === Starting JWT Token Customization ===
INFO  ChainedAuthTokenCustomizer - Found test-auth-server user in session: testuser
INFO  ChainedAuthTokenCustomizer - Current OAuth2 registration: test-auth-server
INFO  ChainedAuthTokenCustomizer - Adding test-auth-server claims to JWT
INFO  ChainedAuthTokenCustomizer - Successfully added test-auth-server claims: sub=testuser, preferred_username=testuser, name=testuser
DEBUG ChainedAuthTokenCustomizer - Skipping GitHub claims - currentUser=present, registration=test-auth-server
INFO  ChainedAuthTokenCustomizer - === Completed JWT Token Customization ===
```

### Missing test-auth-server Session

```
INFO  ChainedAuthTokenCustomizer - === Starting JWT Token Customization ===
DEBUG ChainedAuthTokenCustomizer - Attempting to retrieve test-auth-server user from session
WARN  ChainedAuthTokenCustomizer - No 'TEST_AUTH_SERVER_AUTHENTICATION' attribute found in session - test-auth-server authentication not stored
WARN  ChainedAuthTokenCustomizer - No test-auth-server user found in session!
WARN  ChainedAuthTokenCustomizer - Skipping test-auth-server claims - no test-auth-server user available in session
ERROR ChainedAuthTokenCustomizer - Failed to extract test-auth-server sub claim - JWT will not have proper identity!
INFO  ChainedAuthTokenCustomizer - === Completed JWT Token Customization ===
```

## What Gets Logged

### Entry Point (`customize()` method)

**INFO**: Start/completion markers
- `=== Starting JWT Token Customization ===`
- `=== Completed JWT Token Customization ===`

**DEBUG**: Current authentication details
- Authentication type and principal name
- Current OAuth2 registration ID
- User attributes (if available)

**INFO/WARN**: Session state
- Whether test-auth-server user found in session
- Current OAuth2 registration being processed

### test-auth-server Claims (`addTestAuthServerClaims()`)

**DEBUG**: Processing steps
- Whether extracting from OidcUser or OAuth2User
- Each claim being added (sub, preferred_username, name)

**INFO**: Success summary
- Primary subject being set
- All claims added with their values

**WARN**: Missing data
- No preferred_username found
- No name found

**ERROR**: Critical failures
- Unable to extract sub claim

### GitHub Claims (`addGitHubClaims()`)

**DEBUG**: Each claim addition
- github_login
- github_name
- github_email
- github_id
- github_avatar_url

**INFO**: Summary
- Number of claims added
- Overview of claim values (emails/avatars shown as [present] for privacy)

### Session Retrieval (`getTestAuthServerUser()`)

**DEBUG**: Retrieval process
- Session ID
- Checking for ServletRequestAttributes
- Checking for HttpSession

**INFO**: Success
- Retrieved user details
- Registration ID and username

**WARN**: Missing components
- No ServletRequestAttributes available
- No HTTP session
- No TEST_AUTH_SERVER_AUTHENTICATION in session

**ERROR**: Type mismatch
- Session attribute is not OAuth2AuthenticationToken

## Enabling/Adjusting Log Levels

### Current Configuration

The auth-adapter already has logging enabled in `application.yml`:

```yaml
logging:
  level:
    org.springframework.security: TRACE
    org.springframework.security.oauth2: TRACE
    org.springframework.web.cors: DEBUG
```

### Adding ChainedAuthTokenCustomizer Logging

To adjust logging specifically for the token customizer:

```yaml
logging:
  level:
    org.springframework.security: TRACE
    org.springframework.security.oauth2: TRACE
    org.springframework.web.cors: DEBUG
    com.example.chained.auth.adapter.config.ChainedAuthTokenCustomizer: DEBUG  # Or INFO, WARN
```

### Log Level Recommendations

**Development/Debugging**:
```yaml
com.example.chained.auth.adapter.config.ChainedAuthTokenCustomizer: DEBUG
```
- Shows all claim processing details
- Useful for diagnosing missing claims

**Production**:
```yaml
com.example.chained.auth.adapter.config.ChainedAuthTokenCustomizer: INFO
```
- Shows high-level flow and success/failure
- Less verbose, easier to monitor

**Troubleshooting**:
```yaml
com.example.chained.auth.adapter.config.ChainedAuthTokenCustomizer: TRACE
```
- Would show everything (if TRACE logging was added)
- Currently DEBUG is the most verbose level

## Common Issues and Log Patterns

### Issue: JWT Missing test-auth-server Claims

**Log Pattern**:
```
WARN  ChainedAuthTokenCustomizer - No test-auth-server user found in session!
WARN  ChainedAuthTokenCustomizer - Skipping test-auth-server claims - no test-auth-server user available in session
```

**Cause**: `ChainedAuthenticationSuccessHandler` didn't store test-auth-server auth in session

**Solution**: Check that success handler is properly configured and invoked

### Issue: JWT Missing GitHub Claims

**Log Pattern**:
```
DEBUG ChainedAuthTokenCustomizer - Skipping GitHub claims - currentUser=present, registration=test-auth-server
```

**Cause**: Current authentication is test-auth-server, not GitHub (sequential chain not completed)

**Solution**: Verify GitHub authentication flow completed after test-auth-server

### Issue: No Session Available

**Log Pattern**:
```
WARN  ChainedAuthTokenCustomizer - No HTTP session available - user may not have completed test-auth-server login
```

**Cause**: Session not created or session expired

**Solution**: Check session configuration and timeout settings

### Issue: Wrong Type in Session

**Log Pattern**:
```
ERROR ChainedAuthTokenCustomizer - Session attribute 'TEST_AUTH_SERVER_AUTHENTICATION' is not OAuth2AuthenticationToken: SomeOtherType
```

**Cause**: Something else overwrote the session attribute

**Solution**: Check for conflicts with session attribute names

## Monitoring in Production

### Key Metrics to Monitor

1. **Success Rate**: Count of `=== Completed JWT Token Customization ===`
2. **Error Rate**: Count of ERROR level logs
3. **Missing Session**: Count of `No test-auth-server user found in session`
4. **GitHub Integration**: Count of `Adding GitHub claims to JWT`

### Alerting Thresholds

**High Error Rate**:
- Alert if ERROR logs > 5% of total customizations
- Indicates systematic issue with session management

**Missing test-auth-server Data**:
- Alert if WARN about missing session > 10% of requests
- Indicates success handler not working

**Low GitHub Integration**:
- Monitor if GitHub claims added < expected percentage
- May indicate GitHub auth not completing

## Performance Impact

**Logging Overhead**:
- INFO level: Negligible (~1-2% overhead)
- DEBUG level: Low (~5-10% overhead)
- Recommendation: Use INFO in production, DEBUG for debugging

**Log Volume**:
- INFO: ~5-10 log lines per token customization
- DEBUG: ~15-25 log lines per token customization

## Example Log Search Queries

### Find Failed Customizations
```
level:ERROR AND logger:ChainedAuthTokenCustomizer
```

### Find Successful GitHub Integrations
```
level:INFO AND "Successfully added" AND "GitHub claims"
```

### Find Missing Sessions
```
level:WARN AND "No test-auth-server user found in session"
```

### Track Specific User
```
logger:ChainedAuthTokenCustomizer AND "testuser"
```

## Debugging Checklist

When JWT tokens don't have expected claims, check logs for:

1. ✅ `=== Starting JWT Token Customization ===` - Customizer invoked?
2. ✅ `Found test-auth-server user in session` - Session populated?
3. ✅ `Adding test-auth-server claims` - Primary identity added?
4. ✅ `Adding GitHub claims` - Secondary data added?
5. ✅ `Successfully added X claims` - Claims successfully set?
6. ✅ `=== Completed JWT Token Customization ===` - Process completed?

Any WARN or ERROR indicates where the flow is breaking.
