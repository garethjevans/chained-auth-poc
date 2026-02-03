package org.garethjevans.chained.auth.gateway.config;

import static org.garethjevans.chained.auth.gateway.filter.JwtTokenModifierFilter.modifyBearerToken;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/** Configuration for Gateway routes with JWT token modification filter. */
@Configuration
public class GatewayRouteConfig {

  @Bean
  public RouterFunction<ServerResponse> githubMcpServerRoute() {
    return route("github-mcp-server")
        .route(path("/mcp/**").or(path("/mcp")), http())
        .before(uri("http://localhost:8084"))
        .before(modifyBearerToken())
        .build();
  }
}
