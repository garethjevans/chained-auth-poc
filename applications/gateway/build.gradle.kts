plugins {
    id("java.conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    // Spring Cloud Gateway
    implementation(libs.spring.cloud.gateway.mvc)
    
    // Spring Boot Actuator
    implementation(libs.spring.boot.starter.actuator)

    // Spring Boot Web (includes Jackson)
    implementation(libs.spring.boot.starter.web)
    
    // Jackson for JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind")
    
    // JWT support
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
    
    // Testing
    testImplementation(libs.spring.boot.starter.test)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("gateway.jar")
}
