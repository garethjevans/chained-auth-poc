plugins {
    id("java.conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    // Spring AI BOM for dependency management
    implementation(platform(libs.spring.ai.bom))
    
    // Spring Boot Starters
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    
    // Spring AI MCP Server support
    implementation(libs.spring.ai.starter.mcp.server.webmvc)
    
    // WebClient for GitHub API calls
    implementation(libs.spring.boot.starter.webflux)
    
    // Testing
    testImplementation(libs.spring.boot.starter.test)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("github-mcp-server.jar")
}
