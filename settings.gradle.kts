rootProject.name = "chained-auth-poc"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// Include application modules
include("platform")
include("applications:auth-adapter")
include("applications:test-app")
include("applications:test-auth-server")
include("applications:github-mcp-server")
include("applications:gateway")