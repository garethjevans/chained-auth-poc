# Chained Authentication Flow - Sequence Diagram

This document contains a Mermaid sequence diagram illustrating the complete chained authentication flow between the test-app, auth-adapter, and test-auth-server.

## Flow Diagram

```mermaid
sequenceDiagram
    actor User
    participant Browser
    participant TestApp as Test App<br/>(port 8080)
    participant AuthAdapter as Auth Adapter<br/>(port 9000)
    participant TestAuthServer as Test Auth Server<br/>(port 9001)

    %% User initiates authentication
    User->>Browser: Navigate to http://127.0.0.1:8080/authenticated
    Browser->>TestApp: GET /authenticated
    TestApp-->>Browser: 302 Redirect to Auth Adapter
    Note over TestApp,Browser: No authentication present

    %% Test App redirects to Auth Adapter
    Browser->>AuthAdapter: GET /oauth2/authorize<br/>?client_id=client<br/>&redirect_uri=http://127.0.0.1:8080/login/oauth2/code/auth-adapter<br/>&response_type=code<br/>&scope=openid profile read
    Note over AuthAdapter: User not authenticated
    AuthAdapter-->>Browser: 302 Redirect to /oauth2/authorization/test-auth-server
    
    %% Auth Adapter redirects to Test Auth Server
    Browser->>AuthAdapter: GET /oauth2/authorization/test-auth-server
    AuthAdapter-->>Browser: 302 Redirect to Test Auth Server
    Note over AuthAdapter,Browser: Initiates OAuth2 client flow

    Browser->>TestAuthServer: GET /oauth2/authorize<br/>?client_id=test-client<br/>&redirect_uri=http://127.0.0.1:9000/login/oauth2/code/test-auth-server<br/>&response_type=code<br/>&scope=openid profile
    Note over TestAuthServer: User not authenticated
    TestAuthServer-->>Browser: 302 Redirect to /login

    %% User logs in to Test Auth Server
    Browser->>TestAuthServer: GET /login
    TestAuthServer-->>Browser: 200 Login Form
    Browser->>User: Display Login Form
    User->>Browser: Enter credentials<br/>(testuser/password)
    Browser->>TestAuthServer: POST /login<br/>username=testuser<br/>password=password
    
    Note over TestAuthServer: Validate credentials<br/>Create session
    TestAuthServer-->>Browser: 302 Redirect to /oauth2/authorize<br/>(with session cookie)
    Note over TestAuthServer,Browser: TEST_AUTH_SERVER_SESSION cookie set

    %% Authorization flow continues
    Browser->>TestAuthServer: GET /oauth2/authorize<br/>?client_id=test-client...<br/>(authenticated)
    Note over TestAuthServer: User authenticated<br/>Generate authorization code
    TestAuthServer-->>Browser: 302 Redirect to Auth Adapter<br/>http://127.0.0.1:9000/login/oauth2/code/test-auth-server?code=ABC123

    %% Auth Adapter receives authorization code
    Browser->>AuthAdapter: GET /login/oauth2/code/test-auth-server?code=ABC123
    
    AuthAdapter->>TestAuthServer: POST /oauth2/token<br/>grant_type=authorization_code<br/>code=ABC123<br/>client_id=test-client<br/>client_secret=test-secret
    Note over AuthAdapter,TestAuthServer: Exchange code for tokens

    TestAuthServer-->>AuthAdapter: 200 OK<br/>{<br/>  "access_token": "...",<br/>  "id_token": "...",<br/>  "token_type": "Bearer"<br/>}
    
    Note over AuthAdapter: Extract user info from ID token<br/>sub: "testuser"<br/>preferred_username: "testuser"
    
    AuthAdapter-->>Browser: 302 Redirect to original OAuth2 authorize request
    Note over AuthAdapter,Browser: AUTH_ADAPTER_SESSION cookie set<br/>User now authenticated

    %% Test App completes OAuth2 flow with Auth Adapter
    Browser->>AuthAdapter: GET /oauth2/authorize<br/>?client_id=client...<br/>(authenticated)
    
    rect rgb(200, 220, 255)
        Note over AuthAdapter: User authenticated with Test Auth Server<br/>Generate authorization code for Test App
    end
    
    AuthAdapter-->>Browser: 302 Redirect to Test App<br/>http://127.0.0.1:8080/login/oauth2/code/auth-adapter?code=XYZ789

    %% Test App receives authorization code
    Browser->>TestApp: GET /login/oauth2/code/auth-adapter?code=XYZ789
    
    TestApp->>AuthAdapter: POST /oauth2/token<br/>grant_type=authorization_code<br/>code=XYZ789<br/>client_id=client<br/>client_secret=secret
    Note over TestApp,AuthAdapter: Exchange code for tokens

    rect rgb(255, 220, 200)
        Note over AuthAdapter: Generate JWT token<br/>Include claims from Test Auth Server:<br/>sub: "testuser"<br/>test_auth_server_sub: "testuser"<br/>preferred_username: "testuser"
    end

    AuthAdapter-->>TestApp: 200 OK<br/>{<br/>  "access_token": "JWT_TOKEN",<br/>  "id_token": "...",<br/>  "token_type": "Bearer"<br/>}

    TestApp-->>Browser: 302 Redirect to /authenticated
    Note over TestApp,Browser: TEST_APP_SESSION cookie set

    %% User sees authenticated page
    Browser->>TestApp: GET /authenticated
    TestApp-->>Browser: 200 OK<br/>Display user info and JWT token
    Browser->>User: Show authenticated page with token

    %% Highlight the JWT token contents
    rect rgb(200, 255, 200)
        Note over User,TestApp: JWT Token Contains:<br/>{<br/>  "sub": "testuser",<br/>  "test_auth_server_sub": "testuser",<br/>  "preferred_username": "testuser",<br/>  "name": "testuser",<br/>  "iss": "http://127.0.0.1:9000",<br/>  "aud": "client",<br/>  "authorities": ["ROLE_USER"]<br/>}
    end
```

## Flow Description

### Phase 1: Initial Request (Steps 1-3)
1. User navigates to the test-app's protected page
2. Test-app detects no authentication and redirects to auth-adapter
3. User is sent to auth-adapter's OAuth2 authorization endpoint

### Phase 2: Chained Authentication (Steps 4-6)
4. Auth-adapter detects user is not authenticated
5. Auth-adapter redirects to test-auth-server (as OAuth2 client)
6. Test-auth-server requires authentication and shows login form

### Phase 3: User Authentication (Steps 7-11)
7. User sees test-auth-server login form
8. User enters credentials (testuser/password)
9. Test-auth-server validates credentials and creates session
10. Test-auth-server redirects back to OAuth2 authorization flow
11. Authorization code is generated and sent to auth-adapter

### Phase 4: Auth Adapter Token Exchange (Steps 12-14)
12. Auth-adapter receives authorization code from test-auth-server
13. Auth-adapter exchanges code for access token and ID token
14. Auth-adapter extracts user identity (sub: "testuser") from ID token
15. User is now authenticated in auth-adapter

### Phase 5: Test App Authorization (Steps 15-18)
16. Auth-adapter generates authorization code for test-app
17. Test-app exchanges code for JWT token
18. Auth-adapter creates JWT with test-auth-server identity claims

### Phase 6: Display Result (Steps 19-20)
19. Test-app receives JWT token containing test-auth-server identity
20. User sees authenticated page with token details

## Key Points

- **Primary Identity**: Comes from test-auth-server (testuser)
- **Chained Flow**: Test-app → Auth-adapter → Test-auth-server
- **Three Sessions**: Each service maintains its own session cookie
- **JWT Token**: Contains the `sub` claim from test-auth-server
- **No GitHub**: GitHub authentication is optional/secondary in this flow

## Token Customization

The auth-adapter uses `ChainedAuthTokenCustomizer` to ensure the JWT token contains:
- `sub`: Primary identity from test-auth-server
- `test_auth_server_sub`: Backup claim for traceability
- `preferred_username`: Username from test-auth-server
- `authorities`: User roles from test-auth-server

This ensures the test-auth-server identity is preserved throughout the entire chain.
