# JWT Token Decoding Feature

## Overview

The test-app's `/authenticated` page now decodes and displays the JWT access token claims in a user-friendly table format using Jackson ObjectMapper for reliable JSON parsing.

## Changes Made

### Backend - `HomeController.java`

Added JWT decoding functionality using Jackson:

1. **Constructor Injection** - Injects Spring's `ObjectMapper` bean
   - Leverages the existing Jackson configuration from Spring Boot
   - Ensures consistent JSON parsing across the application

2. **`decodeJwtClaims(String token)`** - Decodes the JWT token and extracts claims
   - Splits the JWT into its three parts (header.payload.signature)
   - Base64 decodes the payload
   - Uses Jackson's `ObjectMapper.readValue()` with `TypeReference` for type-safe parsing
   - Formats arrays as comma-separated strings for display
   - Returns a `LinkedHashMap` to preserve claim order

**Key Implementation Details:**
- Uses `TypeReference<LinkedHashMap<String, Object>>()` for proper generic type handling
- Automatically handles all JSON data types (strings, numbers, booleans, arrays, objects)
- Converts array values to comma-separated strings for clean display
- No manual regex parsing - Jackson handles all edge cases properly

**Note:** This implementation is for **display purposes only** and does **NOT verify the token signature**. The JWT signature should be verified by Spring Security's OAuth2 resource server configuration.

### Build Configuration - `build.gradle.kts`

Added explicit Jackson dependency:
```kotlin
implementation("com.fasterxml.jackson.core:jackson-databind")
```

While Jackson is typically included via `spring-boot-starter-web`, we added it explicitly to ensure it's available for compile-time type checking.

### Frontend - `authenticated.html`

Added a new card section with:

1. **JWT Claims Table** - Displays all decoded claims in a structured format
   - Two-column table: Claim | Value
   - Styled with the same design system as the rest of the page
   - Monospace font for technical data
   - Hover effects for better UX

2. **Visual Styling**
   - Purple header matching the page theme
   - Clean table layout with proper spacing
   - Word-break for long values
   - Responsive design

## What Gets Displayed

The decoded JWT claims typically include:

- **`sub`** - Subject (user identifier from test-auth-server)
- **`iss`** - Issuer (auth-adapter URL)
- **`aud`** - Audience (client ID)
- **`exp`** - Expiration time (Unix timestamp)
- **`iat`** - Issued at time (Unix timestamp)
- **`scope`** - Granted scopes (displayed as: "openid, profile, read")
- **`preferred_username`** - Username from test-auth-server
- **`name`** - User's full name
- **`authorities`** - User's authorities/roles (formatted as comma-separated list)
- **Custom claims** - Any additional claims from `ChainedAuthTokenCustomizer`

## Example Display

```
🔓 Decoded JWT Claims

These are the claims extracted from the JWT access token.
Note: This decoding is for display purposes only and does not verify the token signature.

┌─────────────────────┬────────────────────────────────────────────┐
│ Claim               │ Value                                       │
├─────────────────────┼────────────────────────────────────────────┤
│ sub                 │ testuser                                    │
│ aud                 │ client                                      │
│ iss                 │ http://127.0.0.1:9000                      │
│ exp                 │ 1738078345                                  │
│ iat                 │ 1738074745                                  │
│ scope               │ openid, profile, read                       │
│ preferred_username  │ testuser                                    │
│ name                │ Test User                                   │
│ authorities         │ SCOPE_openid, SCOPE_profile, SCOPE_read     │
└─────────────────────┴────────────────────────────────────────────┘
```

## Usage

1. Start all three applications:
   ```bash
   # Terminal 1 - test-auth-server (port 9001)
   ./gradlew :applications:test-auth-server:bootRun
   
   # Terminal 2 - auth-adapter (port 9000)
   ./gradlew :applications:auth-adapter:bootRun
   
   # Terminal 3 - test-app (port 8080)
   ./gradlew :applications:test-app:bootRun
   ```

2. Navigate to http://127.0.0.1:8080

3. Click "Login with Auth Adapter"

4. Authenticate through the chained auth flow

5. View the `/authenticated` page with:
   - Original JWT token (can be copied)
   - Token metadata (type, issued/expires times, scopes)
   - **NEW: Decoded JWT claims in a table**
   - User information and attributes

## Implementation Evolution

### Initial Approach: Manual Regex Parsing
The first implementation used manual regex-based JSON parsing, which had issues:
- ❌ Arrays with commas were truncated (e.g., `["openid","profile","read"]` became `["read"`)
- ❌ Complex nested structures weren't handled properly
- ❌ Edge cases required custom handling

### Current Approach: Jackson ObjectMapper
Now using Jackson for robust JSON parsing:
- ✅ Handles all JSON data types correctly
- ✅ Properly parses arrays with multiple elements
- ✅ Type-safe with `TypeReference<LinkedHashMap<String, Object>>()`
- ✅ Battle-tested library used throughout Spring ecosystem
- ✅ Cleaner, more maintainable code

## Why Jackson?

**Reliability**: Jackson is the de-facto standard for JSON in the Java ecosystem and is already included in Spring Boot.

**Type Safety**: Using `TypeReference` provides compile-time type checking.

**Maintainability**: Less custom code means fewer bugs and easier maintenance.

**Consistency**: Uses the same JSON parser as the rest of the Spring Boot application.

## Security Note

⚠️ **Important**: The JWT decoding implementation in `HomeController` does **NOT verify the token signature**. It's purely for debugging and display purposes.

For production applications:
- JWT signature verification is handled by Spring Security's OAuth2 resource server
- Don't make authorization decisions based on unverified claims
- This display is safe because the token has already been validated by Spring Security

## Benefits

✅ **Better Debugging** - See exactly what claims are in the token
✅ **Transparency** - Users can verify what data is being shared
✅ **Educational** - Understand JWT structure and contents
✅ **Testing** - Validate that custom claims are being added correctly
✅ **Proper Array Display** - Scopes and other arrays show all values correctly
✅ **Reliable Parsing** - Jackson handles all edge cases

## Testing

Build and run tests:
```bash
./gradlew :applications:test-app:build
```

All tests should pass, confirming the changes don't break existing functionality.

