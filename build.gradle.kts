plugins {
    java
    id("org.springframework.boot") version "4.0.2" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.diffplug.spotless") version "8.2.1" apply false
}

allprojects {
    group = "com.example.chained.auth"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")
    
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // Configure Spotless for code formatting
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            // Use Google Java Format
            googleJavaFormat("1.25.2")
            
            // Format all Java files
            target("src/**/*.java")
            
            // Remove unused imports
            removeUnusedImports()
            
            // Ensure files end with a newline
            endWithNewline()
            
            // Trim trailing whitespace
            trimTrailingWhitespace()
        }
    }
}
