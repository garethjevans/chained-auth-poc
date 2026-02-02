rootProject.name = "chained-auth-poc"

// Include application modules
include("applications:auth-adapter")
include("applications:test-app")
include("applications:test-auth-server")
include("applications:github-mcp-server")

// You can add more modules here as needed
// For example: shared libraries, common modules, etc.
// include("libraries:common")
