package io.spring.ai.plugins.java;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

class VersioningConventions {

    public void apply(Project project) {
        project.setVersion(SemVer.toMavenVersion(getVersionFromGit(project.getLogger())));

        project.getLogger().info("Setting project version conventions configured");
    }

    private String getVersionFromGit(Logger logger) {
        return execute(logger, "git", "describe", "--tags", "--dirty");
    }

    private String execute(Logger logger, String ... commandAndArgs) {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(commandAndArgs);

            Process process = builder.start();

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                List<String> errors = new BufferedReader(new InputStreamReader(process.getErrorStream())).lines().toList();
                logger.warn("Unable to determine version: Error while executing command: {} exit code {}: {}. Defaulting to 0.1.0-SNAPSHOT", String.join(" ", commandAndArgs), exitCode, String.join("\n", errors));

                return "0.0.0-SNAPSHOT";
            }

            return new BufferedReader(new InputStreamReader(process.getInputStream())).lines().toList().get(0);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
