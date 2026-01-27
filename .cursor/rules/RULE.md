# Project Rules

## Code Validation

Before suggesting or making changes to the codebase, always validate the project by running:

```bash
./gradlew test
```

All tests must pass before:
- Committing code changes
- Creating pull requests
- Suggesting code modifications to the user

## Build Standards

- The project uses Gradle 9.3.0
- All modules must build successfully with `./gradlew build`
- Spring Boot applications should follow standard Spring conventions
- Java version: 21

## Testing Requirements

- Every application module must have test classes
- Tests should use JUnit 5 (JUnit Platform)
- Spring Boot tests should use `@SpringBootTest` annotation
- Run full test suite with `./gradlew test` before code changes

## Module Structure

- All Spring Boot applications should be placed in the `applications/` directory
- Each application should have its own `build.gradle.kts`
- Applications should use the Spring Boot plugin
- Common dependencies should be managed at the root level

## Workflow

1. Make code changes
2. Run `./gradlew test` to validate
3. Fix any failing tests
4. Commit only when all tests pass
