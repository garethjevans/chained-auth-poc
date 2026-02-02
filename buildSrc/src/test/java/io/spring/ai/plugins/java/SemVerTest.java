package io.spring.ai.plugins.java;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SemVerTest {

    @Test
    public void canNormalizeVersion() {
        assertThat(SemVer.toMavenVersion("v0.33.0-48-g43d77f3e-dirty")).isEqualTo("0.34.0-SNAPSHOT");
        assertThat(SemVer.toMavenVersion("v0.33.0-48-g43d77f3e")).isEqualTo("0.34.0-SNAPSHOT");
        assertThat(SemVer.toMavenVersion("v0.33.0")).isEqualTo("0.33.0");
    }
}
