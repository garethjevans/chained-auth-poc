package io.spring.ai.plugins.java;

import com.github.zafarkhaja.semver.Version;

public class SemVer {

    public static String toMavenVersion(String gitVersion) {
        gitVersion = gitVersion.trim().replaceAll("^v", "");
        boolean isSnapshot = gitVersion.contains("-");

        Version version = Version.parse(gitVersion);
        if (isSnapshot) {
            return version.nextMinorVersion().toString() + "-SNAPSHOT";
        }

        return version.toString();
    }

}
