plugins {
    `java-gradle-plugin`
    `jvm-test-suite`
}

repositories {
    gradlePluginPortal()
}

group = "io.spring.ai.conventions"
version = "1.0.0"

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.2.1" )
    implementation("com.gorylenko.gradle-git-properties:gradle-git-properties:2.5.4")
    implementation("de.skuzzle.restrictimports:restrict-imports-gradle-plugin:3.0.0")
    implementation("com.github.zafarkhaja:java-semver:0.10.2")

    testImplementation("org.assertj:assertj-core:3.27.7")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}

gradlePlugin {
    plugins.create("java.conventions") {
        id = name
        implementationClass = "io.spring.ai.plugins.java.JavaConventionsPlugin"
    }
}