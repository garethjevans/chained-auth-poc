# Logout Feature

## Overview

The test-app's `/authenticated` page now includes a logout button that properly clears the user session and removes all authentication data.

## What Was Added

### 1. Logout Button in UI

**Location**: `applications/test-app/src/main/resources/templates/authenticated.html`

**Features**:
- 🚪 Red logout button with hover effect
- Positioned next to "Back to Home" button
- CSRF token included for security
- Form-based POST request to `/logout` endpoint

**Visual Design**:
- Red background (`#ef4444`) to distinguish from navigation
- Darker red on hover (`#dc2626`)
- Smooth transition animation
- Responsive layout with flex button group

### 2. Logout Configuration

**Location**: `applications/test-app/src/main/java/com/example/chained/auth/testapp/config/SecurityConfig.java`

**Configuration**:
```java
.logout(logout -> logout
    .logoutSuccessUrl("/")                  // Redirect to home after logout
    .invalidateHttpSession(true)            // Destroy the HTTP session
    .clearAuthentication(true)              // Clear Spring Security context
    .deleteCookies("JSESSIONID", "TEST_APP_SESSION")  // Remove session cookies
)
```

## How It Works

### User Flow

1. **User clicks "Logout" button** on `/authenticated` page
2. **Browser sends POST request** to `/logout` with CSRF token
3. **Spring Security processes logout**:
   - Invalidates HTTP session
   - Clears authentication from Security Context
   - Deletes session cookies (`JSESSIONID`, `TEST_APP_SESSION`)
4. **User redirected to home page** (`/`)
5. **Session fully cleared** - user must re-authenticate to access protected pages

### Security Features

✅ **CSRF Protection**: Logout requires valid CSRF token (prevents CSRF attacks)

✅ **Session Invalidation**: HTTP session is completely destroyed

✅ **Cookie Deletion**: Session cookies removed from browser

✅ **Authentication Cleared**: Spring Security context cleared

✅ **POST-Only**: Logout only accepts POST requests (not GET)

## Testing the Logout

### Manual Testing

1. **Start all services** and navigate to `http://127.0.0.1:8080/authenticated`

2. **Login** through the OAuth2 flow (test-auth-server → GitHub)

3. **View authenticated page** with your JWT token and user info

4. **Click the "🚪 Logout" button**

5. **Verify logout**:
   - ✅ Redirected to home page (`/`)
   - ✅ Session cookie deleted (check browser DevTools → Application → Cookies)
   - ✅ Attempting to access `/authenticated` requires re-login

6. **Try to access protected page**:
   - Navigate to `http://127.0.0.1:8080/authenticated`
   - Should be redirected to login (not cached)

### Browser DevTools Verification

**Before Logout**:
```
Application → Cookies → http://127.0.0.1:8080
✓ TEST_APP_SESSION: <session-id>
✓ JSESSIONID: <session-id>
```

**After Logout**:
```
Application → Cookies → http://127.0.0.1:8080
✗ TEST_APP_SESSION: (deleted)
✗ JSESSIONID: (deleted)
```

## UI Preview

The authenticated page now has two buttons in the footer:

```
┌──────────────────────────────────────┐
│                                      │
│  [← Back to Home]  [🚪 Logout]      │
│                                      │
└──────────────────────────────────────┘
```

**Button Styling**:
- **Back to Home**: Purple gradient (navigation)
- **Logout**: Red (action/warning)

## Configuration Details

### Logout Endpoint

**URL**: `POST /logout`

**Parameters**: CSRF token (required)

**Success**: Redirect to `/`

**Session Changes**:
- HTTP session invalidated
- Spring Security authentication cleared
- Cookies deleted

### Cookie Deletion

The logout configuration deletes:
- `JSESSIONID` - Default Spring session cookie
- `TEST_APP_SESSION` - Custom session cookie (configured in `application.yml`)

### CSRF Token

The logout form includes a hidden CSRF token field:
```html
<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />
```

This token is automatically validated by Spring Security.

## Benefits

✅ **Clean Session Termination**: Properly clears all authentication data

✅ **Security**: Prevents session fixation and replay attacks

✅ **User Control**: Users can explicitly end their session

✅ **Cookie Cleanup**: Removes session cookies from browser

✅ **Full Logout**: Clears both application and Spring Security state

## Troubleshooting

### Issue: Logout Button Not Working

**Symptom**: Clicking logout doesn't clear session

**Solutions**:
1. Check browser console for CSRF errors
2. Verify CSRF token is included in form
3. Clear browser cache and cookies manually
4. Check that logout configuration is in SecurityConfig

### Issue: Still Authenticated After Logout

**Symptom**: Can access `/authenticated` without re-login

**Solutions**:
1. Clear all browser cookies for `127.0.0.1`
2. Check session timeout settings
3. Verify `invalidateHttpSession(true)` is set
4. Try incognito/private browsing mode

### Issue: CSRF Token Missing

**Symptom**: "Invalid CSRF Token" error on logout

**Solutions**:
1. Ensure Thymeleaf CSRF token is properly included
2. Verify Spring Security CSRF protection is enabled (default)
3. Check that form method is POST

## Integration with Chained Auth

When using sequential chained authentication (test-auth-server → GitHub):

**Logout behavior**:
- ✅ Clears test-app session
- ✅ Removes OAuth2 tokens cached by test-app
- ⚠️ Does NOT logout from test-auth-server (separate session)
- ⚠️ Does NOT revoke GitHub OAuth token

**To fully logout**:
1. Logout from test-app (this button)
2. Clear test-auth-server session (navigate to `http://127.0.0.1:9001/logout`)
3. Revoke GitHub OAuth app access (GitHub Settings → Applications)

## Future Enhancements

Consider implementing:
- Single Sign-Out (SSO) to logout from all services
- Logout from auth-adapter automatically
- Revoke OAuth2 tokens on logout
- Logout confirmation dialog
- "Logout from all devices" option
