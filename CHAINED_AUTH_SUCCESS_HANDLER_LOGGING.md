# ChainedAuthenticationSuccessHandler Logging

## Overview

Comprehensive logging has been added to the `ChainedAuthenticationSuccessHandler` class to provide complete visibility into the chained authentication flow. This logging helps debug authentication issues and understand the sequential authentication process (test-auth-server → GitHub).

## Log Levels

The logging is structured across multiple levels:

- **INFO** - High-level flow tracking and key decisions
- **DEBUG** - Detailed state information and attribute values
- **WARN** - Unexpected conditions that may indicate problems
- **ERROR** - Critical failures in the authentication flow

## Logging Sections

### 1. Handler Invocation

**Logged when**: Handler is invoked after successful OAuth2 authentication

```log
INFO  ChainedAuthenticationSuccessHandler - === Authentication Success Handler Invoked ===
DEBUG ChainedAuthenticationSuccessHandler - Request URI: /login/oauth2/code/test-auth-server, Method: GET, Remote Address: 127.0.0.1
```

**Information captured**:
- Request URI (OAuth2 callback URL)
- HTTP method
- Remote client address

### 2. Authentication Details

**Logged when**: Processing the authentication object

```log
INFO  ChainedAuthenticationSuccessHandler - Authentication type: OAuth2AuthenticationToken, Principal: testuser, Authenticated: true
DEBUG ChainedAuthenticationSuccessHandler - Authorities: [ROLE_USER, SCOPE_openid, SCOPE_profile]
```

**Information captured**:
- Authentication class type
- Principal name (username)
- Authentication status
- Granted authorities

**Warnings**:
```log
WARN  ChainedAuthenticationSuccessHandler - Authentication object is null!
```

### 3. OAuth2 User Details

**Logged when**: Extracting OAuth2 user information

```log
INFO  ChainedAuthenticationSuccessHandler - OAuth2 authentication detected: registrationId=test-auth-server
DEBUG ChainedAuthenticationSuccessHandler - OAuth2 User attributes: {sub=testuser, name=Test User, preferred_username=testuser, ...}
DEBUG ChainedAuthenticationSuccessHandler - OAuth2 User name attribute: Test User
```

**Information captured**:
- OAuth2 client registration ID (test-auth-server or github)
- All user attributes from the OAuth2 provider
- Specific name attribute value

**Warnings**:
```log
WARN  ChainedAuthenticationSuccessHandler - OAuth2User principal is null
```

### 4. Session Management

**Logged when**: Accessing HTTP session

```log
DEBUG ChainedAuthenticationSuccessHandler - Session ID: 1234567890ABCDEF, IsNew: false, MaxInactiveInterval: 1800s
```

**Information captured**:
- Session ID
- Whether session is new
- Session timeout value (in seconds)

### 5. Test-Auth-Server Authentication (Step 1)

**Logged when**: User completes test-auth-server authentication

```log
INFO  ChainedAuthenticationSuccessHandler - === Step 1: test-auth-server authentication complete ===
INFO  ChainedAuthenticationSuccessHandler - Storing test-auth-server authentication in session with key: TEST_AUTH_SERVER_AUTHENTICATION
DEBUG ChainedAuthenticationSuccessHandler - Successfully stored test-auth-server authentication in session: type=OAuth2AuthenticationToken
INFO  ChainedAuthenticationSuccessHandler - === Redirecting to GitHub authentication: /oauth2/authorization/github ===
```

**Information captured**:
- Confirmation of test-auth-server authentication
- Session storage operation
- Verification of stored authentication
- Redirect target URL

**Errors**:
```log
ERROR ChainedAuthenticationSuccessHandler - Failed to store test-auth-server authentication in session!
```

### 6. GitHub Authentication (Step 2)

**Logged when**: User completes GitHub authentication

```log
INFO  ChainedAuthenticationSuccessHandler - === Step 2: GitHub authentication complete ===
INFO  ChainedAuthenticationSuccessHandler - Confirmed: test-auth-server authentication found in session: type=OAuth2AuthenticationToken
DEBUG ChainedAuthenticationSuccessHandler - Test-auth-server user: testuser
INFO  ChainedAuthenticationSuccessHandler - === Both authentications complete, continuing normal flow ===
```

**Information captured**:
- Confirmation of GitHub authentication
- Verification that test-auth-server auth is still in session
- Test-auth-server username
- Flow completion status

**Errors**:
```log
ERROR ChainedAuthenticationSuccessHandler - ERROR: test-auth-server authentication NOT found in session! Chained auth may fail!
```

### 7. Unknown Registrations

**Logged when**: Unknown OAuth2 provider

```log
WARN  ChainedAuthenticationSuccessHandler - Unknown OAuth2 registration: custom-provider
```

### 8. Non-OAuth2 Authentication

**Logged when**: Authentication is not OAuth2

```log
WARN  ChainedAuthenticationSuccessHandler - Authentication is not OAuth2AuthenticationToken: org.springframework.security.authentication.UsernamePasswordAuthenticationToken
```

### 9. Parent Handler Delegation

**Logged when**: Delegating to parent handler

```log
INFO  ChainedAuthenticationSuccessHandler - Delegating to parent SavedRequestAwareAuthenticationSuccessHandler
INFO  ChainedAuthenticationSuccessHandler - === Authentication Success Handler Completed Successfully ===
```

**Errors**:
```log
ERROR ChainedAuthenticationSuccessHandler - Error in parent authentication success handler
<exception stack trace>
```

## Complete Flow Examples

### Successful Chained Authentication

#### Step 1: Test-Auth-Server Login

```log
INFO  ChainedAuthenticationSuccessHandler - === Authentication Success Handler Invoked ===
DEBUG ChainedAuthenticationSuccessHandler - Request URI: /login/oauth2/code/test-auth-server, Method: GET, Remote Address: 127.0.0.1
INFO  ChainedAuthenticationSuccessHandler - Authentication type: OAuth2AuthenticationToken, Principal: testuser, Authenticated: true
DEBUG ChainedAuthenticationSuccessHandler - Authorities: [ROLE_USER, SCOPE_openid, SCOPE_profile]
INFO  ChainedAuthenticationSuccessHandler - OAuth2 authentication detected: registrationId=test-auth-server
DEBUG ChainedAuthenticationSuccessHandler - OAuth2 User attributes: {sub=testuser, preferred_username=testuser, name=Test User, email=test@example.com}
DEBUG ChainedAuthenticationSuccessHandler - OAuth2 User name attribute: Test User
DEBUG ChainedAuthenticationSuccessHandler - Session ID: ABC123, IsNew: false, MaxInactiveInterval: 1800s
INFO  ChainedAuthenticationSuccessHandler - === Step 1: test-auth-server authentication complete ===
INFO  ChainedAuthenticationSuccessHandler - Storing test-auth-server authentication in session with key: TEST_AUTH_SERVER_AUTHENTICATION
DEBUG ChainedAuthenticationSuccessHandler - Successfully stored test-auth-server authentication in session: type=OAuth2AuthenticationToken
INFO  ChainedAuthenticationSuccessHandler - === Redirecting to GitHub authentication: /oauth2/authorization/github ===
```

#### Step 2: GitHub Login

```log
INFO  ChainedAuthenticationSuccessHandler - === Authentication Success Handler Invoked ===
DEBUG ChainedAuthenticationSuccessHandler - Request URI: /login/oauth2/code/github, Method: GET, Remote Address: 127.0.0.1
INFO  ChainedAuthenticationSuccessHandler - Authentication type: OAuth2AuthenticationToken, Principal: johndoe, Authenticated: true
DEBUG ChainedAuthenticationSuccessHandler - Authorities: [ROLE_USER, SCOPE_user:email, SCOPE_read:user]
INFO  ChainedAuthenticationSuccessHandler - OAuth2 authentication detected: registrationId=github
DEBUG ChainedAuthenticationSuccessHandler - OAuth2 User attributes: {login=johndoe, id=12345, name=John Doe, email=john@example.com, avatar_url=https://...}
DEBUG ChainedAuthenticationSuccessHandler - OAuth2 User name attribute: John Doe
DEBUG ChainedAuthenticationSuccessHandler - Session ID: ABC123, IsNew: false, MaxInactiveInterval: 1800s
INFO  ChainedAuthenticationSuccessHandler - === Step 2: GitHub authentication complete ===
INFO  ChainedAuthenticationSuccessHandler - Confirmed: test-auth-server authentication found in session: type=OAuth2AuthenticationToken
DEBUG ChainedAuthenticationSuccessHandler - Test-auth-server user: testuser
INFO  ChainedAuthenticationSuccessHandler - === Both authentications complete, continuing normal flow ===
INFO  ChainedAuthenticationSuccessHandler - Delegating to parent SavedRequestAwareAuthenticationSuccessHandler
INFO  ChainedAuthenticationSuccessHandler - === Authentication Success Handler Completed Successfully ===
```

### Failed Chained Authentication - Missing Session

```log
INFO  ChainedAuthenticationSuccessHandler - === Authentication Success Handler Invoked ===
DEBUG ChainedAuthenticationSuccessHandler - Request URI: /login/oauth2/code/github, Method: GET, Remote Address: 127.0.0.1
INFO  ChainedAuthenticationSuccessHandler - Authentication type: OAuth2AuthenticationToken, Principal: johndoe, Authenticated: true
INFO  ChainedAuthenticationSuccessHandler - OAuth2 authentication detected: registrationId=github
DEBUG ChainedAuthenticationSuccessHandler - Session ID: ABC123, IsNew: false, MaxInactiveInterval: 1800s
INFO  ChainedAuthenticationSuccessHandler - === Step 2: GitHub authentication complete ===
ERROR ChainedAuthenticationSuccessHandler - ERROR: test-auth-server authentication NOT found in session! Chained auth may fail!
INFO  ChainedAuthenticationSuccessHandler - === Both authentications complete, continuing normal flow ===
INFO  ChainedAuthenticationSuccessHandler - Delegating to parent SavedRequestAwareAuthenticationSuccessHandler
INFO  ChainedAuthenticationSuccessHandler - === Authentication Success Handler Completed Successfully ===
```

## Configuration

### Enable Full Logging

To see all log levels, update `application.yml`:

```yaml
logging:
  level:
    com.example.chained.auth.adapter.config.ChainedAuthenticationSuccessHandler: DEBUG
```

### Production Configuration

For production, use INFO level:

```yaml
logging:
  level:
    com.example.chained.auth.adapter.config.ChainedAuthenticationSuccessHandler: INFO
```

### Minimal Logging

For minimal output:

```yaml
logging:
  level:
    com.example.chained.auth.adapter.config.ChainedAuthenticationSuccessHandler: WARN
```

## Troubleshooting

### Issue: Test-auth-server auth not found in session (Step 2)

**Log Pattern**:
```log
ERROR ChainedAuthenticationSuccessHandler - ERROR: test-auth-server authentication NOT found in session! Chained auth may fail!
```

**Possible Causes**:
1. Session expired between Step 1 and Step 2
2. Session not persisted correctly
3. Different session used for Step 2
4. Session attribute cleared by another component

**Solutions**:
- Check session timeout configuration
- Verify session cookie is being sent
- Ensure consistent session ID between steps
- Check for session clearing in other components

### Issue: Unknown OAuth2 registration

**Log Pattern**:
```log
WARN  ChainedAuthenticationSuccessHandler - Unknown OAuth2 registration: custom-provider
```

**Possible Causes**:
1. New OAuth2 provider added but not handled
2. Configuration mismatch

**Solutions**:
- Add handling for new provider
- Verify OAuth2 client configuration
- Check registration ID matches configuration

### Issue: Authentication object is null

**Log Pattern**:
```log
WARN  ChainedAuthenticationSuccessHandler - Authentication object is null!
```

**Possible Causes**:
1. Security filter chain misconfiguration
2. Authentication filter not working
3. Handler invoked incorrectly

**Solutions**:
- Review security configuration
- Check filter chain order
- Verify OAuth2 client setup

### Issue: OAuth2User principal is null

**Log Pattern**:
```log
WARN  ChainedAuthenticationSuccessHandler - OAuth2User principal is null
```

**Possible Causes**:
1. OAuth2 user info endpoint failed
2. Token parsing issue
3. Provider configuration error

**Solutions**:
- Check OAuth2 provider configuration
- Verify user-info-uri is correct
- Check provider's response format

### Issue: Session attributes not persisting

**Log Pattern**:
```log
ERROR ChainedAuthenticationSuccessHandler - Failed to store test-auth-server authentication in session!
```

**Possible Causes**:
1. Session store not configured
2. Attribute serialization issue
3. Session read-only mode

**Solutions**:
- Verify session configuration
- Check session store (Redis, JDBC, etc.)
- Ensure attributes are serializable

## Monitoring

### Key Metrics to Track

Based on the logs, monitor:

**Success Rate**:
- Count of "Step 1: test-auth-server authentication complete"
- Count of "Step 2: GitHub authentication complete"
- Ratio should be close to 1:1

**Session Issues**:
- Count of "ERROR: test-auth-server authentication NOT found in session"
- High count indicates session problems

**Redirect Success**:
- Count of "Redirecting to GitHub authentication"
- Should match Step 1 completions

**Handler Completions**:
- Count of "Authentication Success Handler Completed Successfully"
- Should match total authentication attempts

### Log Aggregation Queries

**Elasticsearch/Splunk**:

```
# Count successful Step 1 completions
"Step 1: test-auth-server authentication complete"

# Count successful Step 2 completions
"Step 2: GitHub authentication complete"

# Find session errors
"ERROR: test-auth-server authentication NOT found in session"

# Find handler errors
"Error in parent authentication success handler"
```

**Grep** (for local log files):

```bash
# Count Step 1 completions
grep "Step 1: test-auth-server authentication complete" auth-adapter.log | wc -l

# Count Step 2 completions
grep "Step 2: GitHub authentication complete" auth-adapter.log | wc -l

# Find errors
grep "ERROR" auth-adapter.log | grep "ChainedAuthenticationSuccessHandler"
```

## Debugging Checklist

When debugging chained authentication issues, verify:

### Step 1 (test-auth-server)
- [ ] Handler invoked with registrationId=test-auth-server
- [ ] OAuth2User attributes present
- [ ] Session ID logged
- [ ] Authentication stored in session successfully
- [ ] Redirect to GitHub initiated

### Step 2 (GitHub)
- [ ] Handler invoked with registrationId=github
- [ ] OAuth2User attributes present
- [ ] Same session ID as Step 1
- [ ] Test-auth-server auth found in session
- [ ] Both authentications available
- [ ] Parent handler delegation successful

### Common Patterns
- [ ] No null warnings in logs
- [ ] No ERROR messages
- [ ] Session ID consistent across both steps
- [ ] Both registration IDs recognized
- [ ] Handler completes successfully

## Related Configuration

### Session Configuration

```yaml
server:
  servlet:
    session:
      cookie:
        name: AUTH_ADAPTER_SESSION
        secure: false  # For development
      timeout: 30m     # Session timeout
```

### Security Debug Mode

For more detailed Spring Security logs:

```yaml
logging:
  level:
    org.springframework.security: TRACE
```

## Benefits

✅ **Complete Visibility** - See every step of the chained authentication
✅ **Easy Debugging** - Clear log messages identify issues quickly
✅ **Session Tracking** - Monitor session state and persistence
✅ **Flow Verification** - Confirm both authentication steps complete
✅ **Error Detection** - Errors logged with context for troubleshooting
✅ **Production Ready** - Configurable log levels for different environments

## Performance Considerations

**Log Volume**:
- INFO level: ~10 log lines per authentication
- DEBUG level: ~15-20 log lines per authentication

**Impact**:
- Minimal performance impact
- Log I/O is asynchronous (default)
- String formatting only executed when level enabled

**Recommendations**:
- Use DEBUG in development
- Use INFO in staging
- Use WARN in production (or INFO for critical apps)

## Related Documentation

- [CHAINED_AUTH_TOKEN_CUSTOMIZER_LOGGING.md](CHAINED_AUTH_TOKEN_CUSTOMIZER_LOGGING.md) - Token customizer logging
- [SEQUENTIAL_CHAINED_AUTH.md](SEQUENTIAL_CHAINED_AUTH.md) - Sequential authentication flow
- [AUTHENTICATION_FLOW_DIAGRAM.md](AUTHENTICATION_FLOW_DIAGRAM.md) - Visual flow diagram
