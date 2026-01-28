# Chained Auth POC

A multi-module Gradle project demonstrating Spring Boot applications with a chained authentication proof of concept.

## Project Structure

```
chained-auth-poc/
├── applications/             # Spring Boot application modules
│   ├── auth-adapter/        # Spring Authorization Server (port 9000)
│   ├── test-app/            # OAuth2 test client application (port 8080)
│   └── test-auth-server/    # Dummy auth server for E2E testing (port 9001)
├── gradle/                  # Gradle wrapper files
├── build.gradle.kts         # Root build configuration
├── settings.gradle.kts      # Multi-module settings
├── FORMATTING.md            # Code formatting guide
└── gradlew                  # Gradle wrapper script
```

## Technology Stack

- **Gradle**: 9.3.0 (latest version)
- **Spring Boot**: 4.0.2
- **Spring Security**: 7.0 (with integrated OAuth2 Authorization Server)
- **Java**: 21
- **Spring Dependency Management**: 1.1.7

## Applications

### Auth Adapter
- **Port**: 9000
- **Type**: Spring Authorization Server with GitHub OAuth integration
- **Endpoints**:
  - Home: `http://127.0.0.1:9000/`
  - User Info: `http://127.0.0.1:9000/user`
  - OIDC Config: `http://127.0.0.1:9000/.well-known/oauth-authorization-server`
- **Purpose**: Acts as an OAuth2 Authorization Server that federates authentication to GitHub
- See [Auth Adapter README](applications/auth-adapter/README.md) for setup instructions

### Test App
- **Port**: 8080
- **Type**: OAuth2 Client application for testing authentication flow
- **Endpoints**:
  - Home: `http://127.0.0.1:8080/`
  - Authenticated: `http://127.0.0.1:8080/authenticated` (requires login)
- **Purpose**: Demonstrates OAuth2 authentication flow and displays access tokens
- See [Test App README](applications/test-app/README.md) for details

## Getting Started

### Prerequisites
- Java 21 or higher

### Build the Project

```bash
./gradlew build
```

### Run Applications

Run Auth Adapter (requires GitHub OAuth setup):
```bash
./gradlew :applications:auth-adapter:bootRun
```

Run Test App:
```bash
./gradlew :applications:test-app:bootRun
```

**Testing the Complete Flow:**
1. Start auth-adapter on port 9000
2. Start test-app on port 8080
3. Open http://127.0.0.1:8080 in your browser
4. Click "Login with Auth Adapter"
5. Authenticate with GitHub (via auth-adapter)
6. View your access token and user information

### Run Tests

```bash
./gradlew test
```

### Build JAR Files

```bash
./gradlew bootJar
```

The JAR files will be available at:
- `applications/auth-adapter/build/libs/auth-adapter.jar`
- `applications/test-app/build/libs/test-app.jar`

## Adding New Applications

To add a new application to the `applications` folder:

1. Add the module to `settings.gradle.kts`:
   ```kotlin
   include("applications:app3")
   ```

2. Create the module directory structure:
   ```
   applications/app3/
   ├── build.gradle.kts
   └── src/
       ├── main/
       │   ├── java/
       │   └── resources/
       └── test/
           └── java/
   ```

3. Copy and modify the `build.gradle.kts` from an existing application.

## Health Checks

The applications expose actuator endpoints:
- Auth Adapter: `http://127.0.0.1:9000/actuator/health`
- Test App: `http://127.0.0.1:8080/actuator/health`

## CI/CD

The project includes a GitHub Actions workflow that:
- Runs on every pull request and push to main/master
- **Validates code formatting** with Spotless
- Builds the project with Gradle
- Runs all tests
- Uploads test results and build artifacts

See [`.github/workflows/gradle-build.yml`](.github/workflows/gradle-build.yml) for details.

## Development

### Code Formatting

This project uses **Spotless** with **Google Java Format** for consistent code formatting.

**Check formatting:**
```bash
./gradlew spotlessCheck
```

**Apply formatting:**
```bash
./gradlew spotlessApply
```

See [FORMATTING.md](FORMATTING.md) for detailed information on code formatting, IDE integration, and best practices.

### IDE Setup
Import the project as a Gradle project in your IDE (IntelliJ IDEA, Eclipse, VS Code).

### Gradle Commands

- `./gradlew tasks` - List all available tasks
- `./gradlew clean` - Clean build directories
- `./gradlew build --refresh-dependencies` - Refresh dependencies
- `./gradlew projects` - List all modules
- `./gradlew spotlessCheck` - Check code formatting
- `./gradlew spotlessApply` - Apply code formatting

## License

This is a proof of concept project.
