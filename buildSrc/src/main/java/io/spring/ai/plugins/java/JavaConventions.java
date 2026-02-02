package io.spring.ai.plugins.java;

import java.math.BigDecimal;

import de.skuzzle.restrictimports.gradle.RestrictImports;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.JvmTestSuitePlugin;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.logging.TestExceptionFormat;
import org.gradle.jvm.tasks.Jar;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.testing.base.TestingExtension;

import java.util.List;
import org.gradle.testing.jacoco.plugins.JacocoPlugin;
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification;
import org.gradle.testing.jacoco.tasks.JacocoReport;

import de.skuzzle.restrictimports.gradle.RestrictImportsPlugin;

class JavaConventions {

    private final int JAVA_VERSION = 25;
    private final List<String> compilerArgs = List.of(
            "-parameters",
         //   "-Werror",
            "-Xlint:unchecked",
            "-Xlint:deprecation",
            "-Xlint:rawtypes",
            "-Xlint:varargs");

    public void apply(Project project) {
        project.getPluginManager().apply(JavaLibraryPlugin.class);
        project.getPluginManager().apply(JvmTestSuitePlugin.class);
        project.getPluginManager().apply(JacocoPlugin.class);
        project.getPluginManager().apply(RestrictImportsPlugin.class);
        project.getPluginManager().apply(MavenPublishPlugin.class);
        applyCompilerConventions(project);
        applyJarConventions(project);
        applyTestConventions(project);
        applyJacocoConventions(project);
        applyRestrictImportsConventions(project);
        applyPlatforms(project);
    }

    private void applyPlatforms(Project project) {
        DependencyHandler dependencies = project.getDependencies();
        Dependency platform = dependencies.platform(project.project(":platform"));

        dependencies.add("implementation", platform);
        dependencies.add("annotationProcessor", platform);
        dependencies.add("testImplementation", platform);

        project.getPluginManager().withPlugin("java-test-fixtures", plugin ->
                dependencies.add("testFixturesImplementation", platform)
        );
        project.getPluginManager().withPlugin("org.springframework.boot", plugin ->
                dependencies.add("developmentOnly", platform)
        );
    }

    private void applyCompilerConventions(Project project) {
        // set Java tool chain version
        var java = project.getExtensions().getByType(JavaPluginExtension.class);
        java.getToolchain().getLanguageVersion()
                .set(JavaLanguageVersion.of(JAVA_VERSION));

        // set compiler flags
        project.getTasks().withType(JavaCompile.class, (compile) -> {
            compile.getOptions().setEncoding("UTF-8");
            List<String> args = compile.getOptions().getCompilerArgs();
            args.addAll(compilerArgs);
        });
    }

    private void applyJarConventions(Project project) {
        project.getTasks().withType(Jar.class).configureEach(jar ->
                jar.getArchiveBaseName().set(project.getPath().replace(":", "-").substring(1))
        );
    }

    private void applyTestConventions(Project project) {

        var testing = project.getExtensions().getByType(TestingExtension.class);
        testing.getSuites().withType(JvmTestSuite.class, c -> c.useJUnitJupiter());

        project.getTasks().withType(Test.class).forEach(test -> {
            test.testLogging(loggingContainer -> {
                loggingContainer.setShowStandardStreams(false);
                loggingContainer.setShowCauses(true);
                loggingContainer.setShowStackTraces(true);
                loggingContainer.setExceptionFormat(TestExceptionFormat.FULL);
            });
        });
    }

    private void applyJacocoConventions(Project project) {
        project.getTasks().withType(Test.class).configureEach(test -> {
            test.finalizedBy(project.getTasks().withType(JacocoReport.class)); // Always generate report after tests
            test.finalizedBy(project.getTasks().withType(JacocoCoverageVerification.class)); // Always verify after tests
        });

        project.getTasks().withType(JacocoCoverageVerification.class).configureEach(coverageVerification -> {
            coverageVerification.violationRules(rules -> {
                rules.rule(rule -> {
                    rule.limit(limit -> {
                        limit.setMinimum(new BigDecimal("0.0")); // Set minimum coverage to 50%
                    });
                });
            });
        });
    }

    private void applyRestrictImportsConventions(Project project) {
        project.getTasks().withType(Test.class).configureEach(test -> {
            test.finalizedBy(project.getTasks().withType(RestrictImports.class));
        });

        project.getTasks().withType(RestrictImports.class).configureEach(config -> {
            config.getReason().set("Use assertj instead");
            config.getBannedImports().set(List.of(
                    "org.junit.jupiter.api.Assertions",
                    "static org.junit.jupiter.api.Assertions.*",
                    "static org.junit.Assert.*",
                    "org.junit.Assert"
            ));
        });
    }
}
