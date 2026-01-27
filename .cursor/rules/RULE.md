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

## Git Workflow

### Branch Strategy

**All code changes MUST be done on a fresh branch and submitted as a Pull Request.**

1. **Create a new branch** from `main` with a descriptive name:
   ```bash
   git checkout -b feature/descriptive-name
   # or
   git checkout -b fix/bug-description
   # or
   git checkout -b chore/task-description
   ```

2. **Make code changes** on the new branch

3. **Run tests** to validate:
   ```bash
   ./gradlew test
   ```

4. **Fix any failing tests** before proceeding

5. **Commit changes** only when all tests pass:
   - Use clear, descriptive commit messages
   - Follow conventional commit format: `type(scope): description`
   - Examples: `feat(auth): add GitHub OAuth`, `fix(app1): resolve startup issue`

6. **Push the branch** to remote:
   ```bash
   git push -u origin branch-name
   ```

7. **Create a Pull Request** on GitHub:
   - Use `gh pr create` command or GitHub web interface
   - Include a clear title and description
   - Explain what changes were made and why
   - Add test plan if applicable

8. **NEVER commit directly to main branch**

### Commit Message Guidelines

Use conventional commit format:
- `feat:` - New features
- `fix:` - Bug fixes
- `chore:` - Maintenance tasks
- `docs:` - Documentation updates
- `test:` - Test additions or updates
- `refactor:` - Code refactoring
