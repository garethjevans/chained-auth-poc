# GitHub MCP Server

A basic Model Context Protocol (MCP) server implementation that provides GitHub API access through MCP tools.

## Overview

This application is a Spring Boot-based MCP server that exposes GitHub API functionality through HTTP endpoints following the Model Context Protocol conventions. It provides a simple REST API that allows clients to call the `get_me` tool to fetch information about the currently authenticated GitHub user.

## Features

- **Stateless HTTP Transport**: Uses HTTP-based MCP transport for stateless operation
- **Bearer Token Passthrough**: Forwards the Bearer token from incoming requests to GitHub API calls
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

This MCP server implements a basic HTTP endpoint structure compatible with MCP:

- `POST /mcp/tools/call` - Executes a tool call
- `GET /mcp/tools/list` - Lists available tools

## Configuration

The server runs on port **8084** by default.

Key configuration properties in `application.yml`:

```yaml
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

Once running, the MCP server will be available at `http://localhost:8084`. 

### Calling the get_me Tool

**Endpoint**: `POST /mcp/tools/call`

**Headers**:
```
Authorization: Bearer ghp_xxxxxxxxxxxxx
Content-Type: application/json
```

**Request Body**:
```json
{
  "name": "get_me"
}
```

**Example using curl**:
```bash
curl -X POST http://localhost:8084/mcp/tools/call \
  -H "Authorization: Bearer ghp_xxxxxxxxxxxxx" \
  -H "Content-Type: application/json" \
  -d '{"name": "get_me"}'
```

### Listing Available Tools

**Endpoint**: `GET /mcp/tools/list`

**Example**:
```bash
curl http://localhost:8084/mcp/tools/list
```

## Authentication

This server passes through the Bearer token from the incoming HTTP request's `Authorization` header to the GitHub API. Ensure you have a valid GitHub Personal Access Token with appropriate scopes for the API endpoints you want to access.

To create a GitHub Personal Access Token:
1. Go to GitHub Settings > Developer settings > Personal access tokens
2. Generate a new token with required scopes (e.g., `read:user`)
3. Use this token in the Authorization header when calling the MCP server

## Dependencies

- Spring Boot 4.0.2
- Spring Boot Starter Web
- Spring Boot Starter WebFlux (for WebClient)
- Spring Boot Actuator
- Jackson (for JSON processing)

## API Endpoints

- **MCP Tool Call**: `POST /mcp/tools/call`
- **MCP Tool List**: `GET /mcp/tools/list`
- **Health Check**: `GET /actuator/health`
- **Info**: `GET /actuator/info`
