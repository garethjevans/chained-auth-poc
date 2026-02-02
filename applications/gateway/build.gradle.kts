plugins {
    id("java.conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    // Spring Cloud Gateway with explicit version
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc")
    
    // Spring Boot Actuator
    implementation(libs.spring.boot.starter.actuator)
    
    // Testing
    testImplementation(libs.spring.boot.starter.test)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("gateway.jar")
}
