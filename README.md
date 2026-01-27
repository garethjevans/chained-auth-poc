# Chained Auth POC

A multi-module Gradle project demonstrating Spring Boot applications with a chained authentication proof of concept.

## Project Structure

```
chained-auth-poc/
├── applications/          # Spring Boot application modules
│   └── auth-adapter/     # Spring Authorization Server (port 9000)
├── gradle/               # Gradle wrapper files
├── build.gradle.kts      # Root build configuration
├── settings.gradle.kts   # Multi-module settings
└── gradlew              # Gradle wrapper script
```

## Technology Stack

- **Gradle**: 9.3.0 (latest version)
- **Spring Boot**: 3.4.2
- **Java**: 21
- **Spring Dependency Management**: 1.1.7

## Applications

### Auth Adapter
- **Port**: 9000
- **Type**: Spring Authorization Server with GitHub OAuth integration
- **Endpoints**:
  - Home: `http://localhost:9000/`
  - User Info: `http://localhost:9000/user`
  - OIDC Config: `http://localhost:9000/.well-known/oauth-authorization-server`
- **Purpose**: Acts as an OAuth2 Authorization Server that federates authentication to GitHub
- See [Auth Adapter README](applications/auth-adapter/README.md) for setup instructions

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

The application exposes actuator endpoints:
- Auth Adapter: `http://localhost:9000/actuator/health`

## CI/CD

The project includes a GitHub Actions workflow that:
- Runs on every pull request and push to main/master
- Builds the project with Gradle
- Runs all tests
- Uploads test results and build artifacts

See [`.github/workflows/gradle-build.yml`](.github/workflows/gradle-build.yml) for details.

## Development

### IDE Setup
Import the project as a Gradle project in your IDE (IntelliJ IDEA, Eclipse, VS Code).

### Gradle Commands

- `./gradlew tasks` - List all available tasks
- `./gradlew clean` - Clean build directories
- `./gradlew build --refresh-dependencies` - Refresh dependencies
- `./gradlew projects` - List all modules

## License

This is a proof of concept project.
