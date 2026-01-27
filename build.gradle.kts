plugins {
    java
    id("org.springframework.boot") version "3.4.2" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
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
    
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
