plugins {
    id("java.conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    // Spring AI BOM for dependency management
    implementation(platform("org.springframework.ai:spring-ai-bom:2.0.0-M2"))
    
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Spring AI MCP Server support
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")
    
    // WebClient for GitHub API calls
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("github-mcp-server.jar")
}
