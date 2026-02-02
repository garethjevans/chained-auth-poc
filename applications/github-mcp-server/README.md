# GitHub MCP Server

A Spring Boot application that provides GitHub API access through the Model Context Protocol (MCP) using Spring AI 2.0.0-M2.

## Overview

This application is a Spring Boot-based MCP server that exposes GitHub API functionality through HTTP endpoints following the Model Context Protocol. It leverages Spring AI's MCP support to automatically configure MCP endpoints and tool discovery.

## Features

- **Spring AI 2.0.0-M2**: Uses the latest milestone release of Spring AI with built-in MCP support
- **Stateless HTTP Transport**: HTTP-based MCP transport for stateless operation  
- **Bearer Token Passthrough**: Forwards the Bearer token from incoming requests to GitHub API calls
- **Auto-configured MCP Server**: Spring Boot auto-configuration handles MCP endpoint setup
- **get_me Tool**: Returns details about the currently authenticated GitHub user

## MCP Tool

### get_me

Returns information about the currently authenticated GitHub user.

**Parameters**: None

**Returns**: A JSON object containing the user's GitHub profile information, including:
- `login`: GitHub username
- `id`: GitHub user ID
- `name`: Display name
- `email`: Primary email address
- `avatar_url`: Profile picture URL
- And other GitHub user fields

## Implementation Details

This MCP server uses Spring AI's MCP support which automatically:
- Configures MCP HTTP endpoints
- Discovers and registers MCP tools
- Handles MCP protocol communication

The `GitHubTools` component implements a Function interface that Spring AI can discover and expose as an MCP tool.

## Configuration

The server runs on port **8084** by default and is configured for HTTP-based MCP transport.

Key configuration properties in `application.yml`:

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true
        transport: http

server:
  port: 8084
```

## Running the Server

### Using Gradle

```bash
./gradlew :applications:github-mcp-server:bootRun
```

### Building a JAR

```bash
./gradlew :applications:github-mcp-server:bootJar
java -jar applications/github-mcp-server/build/libs/github-mcp-server.jar
```

## Usage

Once running, the MCP server will be available at `http://localhost:8084` with Spring AI's auto-configured MCP endpoints.

### Calling Tools via MCP

The exact endpoint structure depends on Spring AI's MCP server implementation. Typically:

- MCP server endpoints are exposed under `/mcp/` or similar paths
- Tools can be discovered via the MCP protocol's list tools endpoint
- Tools can be called via the MCP protocol's call tool endpoint

### Authentication

This server passes through the Bearer token from the incoming HTTP request's `Authorization` header to the GitHub API. Ensure you have a valid GitHub Personal Access Token with appropriate scopes.

To create a GitHub Personal Access Token:
1. Go to GitHub Settings > Developer settings > Personal access tokens
2. Generate a new token with required scopes (e.g., `read:user`)
3. Use this token in the Authorization header when calling the MCP server

**Example header**:
```
Authorization: Bearer ghp_xxxxxxxxxxxxx
```

## Dependencies

- Spring Boot 4.0.2
- **Spring AI 2.0.0-M2** (with MCP support)
  - spring-ai-starter-mcp-server-webmvc
  - spring-ai-mcp-annotations
- Spring Boot Starter WebFlux (for WebClient)
- Spring Boot Actuator

## API Endpoints

The exact MCP endpoints are auto-configured by Spring AI. Common endpoints include:

- **Health Check**: `GET /actuator/health`
- **Info**: `GET /actuator/info`
- **MCP Endpoints**: Auto-configured by Spring AI (check logs on startup for exact paths)

## Development Notes

This implementation uses Spring AI 2.0.0-M2 which is a milestone release. The MCP annotation support is provided through the `spring-ai-mcp-annotations` module which includes the community `mcp-annotations` library (version 0.8.0).

Currently, the tool is implemented as a Spring Function component to ensure compatibility. Future versions may use direct MCP annotations once the package structure is stabilized.
