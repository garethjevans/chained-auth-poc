# Test App

A test application that demonstrates OAuth2 authentication flow with the Auth Adapter.

## Overview

The test-app is a Spring Boot web application that acts as an OAuth2 client, authenticating users through the auth-adapter module. It provides a simple web interface to:

1. Initiate the OAuth2 authentication flow
2. Display the access token received from the auth-adapter
3. Show user information and token details

## Features

- **Beautiful Web UI**: Modern, responsive interface built with Thymeleaf
- **OAuth2 Client**: Fully configured OAuth2 client using Spring Security
- **Token Display**: Shows the complete access token and metadata
- **User Information**: Displays authenticated user details
- **Copy Token**: Easy one-click token copying to clipboard

## Running the Application

### Prerequisites

1. **Auth Adapter must be running** on port 9000
2. Auth Adapter must be configured with GitHub OAuth

### Start the Test App

```bash
./gradlew :applications:test-app:bootRun
```

The application will start on **port 8080**.

### Access the Application

1. Open your browser to: http://127.0.0.1:8080
2. Click "Login with Auth Adapter"
3. You'll be redirected through the authentication flow:
   - Auth Adapter → GitHub → Auth Adapter → Test App
4. View your access token and user information

## Configuration

The test-app is configured to connect to the auth-adapter running on `http://127.0.0.1:9000`.

### OAuth2 Client Configuration

Located in `src/main/resources/application.yml`:

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
            scope:
              - openid
              - profile
              - read
        provider:
          auth-adapter:
            issuer-uri: http://127.0.0.1:9000
```

## Endpoints

- `/` - Home page with login button
- `/authenticated` - Displays access token and user information (requires authentication)
- `/actuator/health` - Health check endpoint

## Architecture

### Authentication Flow

```
User → Test App → Auth Adapter → GitHub
                       ↓
User ← Test App ← Access Token
```

1. User clicks "Login with Auth Adapter"
2. Test app redirects to Auth Adapter authorization endpoint
3. Auth Adapter redirects to GitHub for authentication
4. User authenticates with GitHub
5. GitHub redirects back to Auth Adapter with authorization code
6. Auth Adapter issues access token
7. Test app receives access token
8. Test app displays token and user information

### Components

- **HomeController**: Handles routes and displays token information
- **SecurityConfig**: Configures OAuth2 login and security rules
- **Templates**: Thymeleaf templates for UI rendering
  - `index.html`: Landing page with login button
  - `authenticated.html`: Token display page

## Token Information Displayed

- **Token Value**: The complete JWT access token
- **Token Type**: Typically "Bearer"
- **Issued At**: Timestamp when token was created
- **Expires At**: Timestamp when token expires
- **Scopes**: List of granted OAuth2 scopes
- **Time Until Expiry**: Countdown in seconds

## Development

### Running Both Applications

You need both applications running:

```bash
# Terminal 1: Start auth-adapter
./gradlew :applications:auth-adapter:bootRun

# Terminal 2: Start test-app
./gradlew :applications:test-app:bootRun
```

### Testing the Flow

1. Ensure auth-adapter is properly configured with GitHub OAuth credentials
2. Start both applications
3. Navigate to http://127.0.0.1:8080
4. Test the complete authentication flow
5. Verify token is displayed correctly

## Security Notes

- The client credentials (`client` / `secret`) are configured to match the auth-adapter's registered client
- These are test credentials and should be changed for production use
- Tokens are displayed for testing purposes only
- In production, tokens should be handled securely and not displayed to users
