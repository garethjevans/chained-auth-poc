# Code Formatting with Spotless

This project uses [Spotless](https://github.com/diffplug/spotless) with [Google Java Format](https://github.com/google/google-java-format) to ensure consistent code formatting across the codebase.

## What is Spotless?

Spotless is a code formatter that can check and automatically fix code formatting issues. It's configured to run as part of the Gradle build process.

## Configuration

The Spotless plugin is configured in the root `build.gradle.kts` file and applies to all subprojects. It uses:

- **Google Java Format 1.25.2** - Google's code formatter for Java
- **Remove unused imports** - Automatically removes unused import statements
- **Trim trailing whitespace** - Removes whitespace at the end of lines
- **End with newline** - Ensures all files end with a newline character

## Usage

### Check Code Formatting

To check if your code is properly formatted without making changes:

```bash
./gradlew spotlessCheck
```

This command will:
- Check all Java files against the formatting rules
- Report any violations
- Exit with a non-zero status if violations are found

### Apply Code Formatting

To automatically format your code:

```bash
./gradlew spotlessApply
```

This command will:
- Format all Java files according to the Google Java Format style
- Remove unused imports
- Fix trailing whitespace
- Ensure files end with newlines

### Format Specific Module

To format only a specific module:

```bash
./gradlew :applications:auth-adapter:spotlessApply
./gradlew :applications:test-app:spotlessApply
./gradlew :applications:test-auth-server:spotlessApply
```

## CI/CD Integration

Code formatting is automatically checked on every pull request via GitHub Actions. The workflow:

1. Runs `spotlessCheck` before building
2. Fails the build if formatting violations are found
3. Provides detailed output showing what needs to be fixed

If the CI check fails due to formatting issues, simply run:

```bash
./gradlew spotlessApply
```

Then commit and push the formatted code.

## IDE Integration

### IntelliJ IDEA

1. Install the "google-java-format" plugin from the IntelliJ marketplace
2. Go to **Settings → Other Settings → google-java-format Settings**
3. Enable "Enable google-java-format"
4. Restart IntelliJ IDEA

### VS Code

1. Install the "Language Support for Java(TM) by Red Hat" extension
2. Install the "google-java-format Formatter" extension
3. Configure it as your default Java formatter

### Eclipse

1. Download the google-java-format Eclipse plugin from the [releases page](https://github.com/google/google-java-format/releases)
2. Install the plugin in Eclipse
3. Configure it in **Window → Preferences → Java → Code Style → Formatter**

## Pre-commit Hook (Optional)

To automatically format code before each commit, create a `.git/hooks/pre-commit` file:

```bash
#!/bin/sh
./gradlew spotlessApply
git add -u
```

Make it executable:

```bash
chmod +x .git/hooks/pre-commit
```

## Common Issues

### "The following files had format violations"

**Solution:** Run `./gradlew spotlessApply` to automatically fix the violations.

### Import order issues

**Solution:** The formatter will automatically organize imports according to Google's style. Just run `spotlessApply`.

### Formatting conflicts after merge

**Solution:** After merging branches, run `./gradlew spotlessApply` to ensure consistent formatting.

## Best Practices

1. **Run `spotlessApply` before committing** - This prevents CI failures due to formatting
2. **Don't disable formatting** - Keep code style consistent across the team
3. **Use IDE integration** - Configure your IDE to format on save
4. **Check before pushing** - Run `./gradlew spotlessCheck` to verify formatting

## Gradle Tasks Reference

| Task | Description |
|------|-------------|
| `spotlessCheck` | Check if code is formatted correctly |
| `spotlessApply` | Format code automatically |
| `spotlessJavaCheck` | Check Java files only |
| `spotlessJavaApply` | Format Java files only |

## Resources

- [Spotless Documentation](https://github.com/diffplug/spotless)
- [Google Java Format](https://github.com/google/google-java-format)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
