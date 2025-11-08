package com.bookverse.bookverse_gateway;

import com.bookverse.bookverse_gateway.config.PublicEndpointConfig;
import com.bookverse.bookverse_gateway.exception.JwtAuthenticationException;
import com.bookverse.bookverse_gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class AuthenticationFilter implements GlobalFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PublicEndpointConfig publicEndpointsConfig;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();
        log.info("Filtering request to path: {} with method: {}", path, method);
        if (isPublicEndpoint(path, method)) {
            log.info("Public endpoint accessed: {} {}", method, path);
            return chain.filter(exchange);
        }
        if (!request.getHeaders().containsKey("Authorization")) {
            throw new JwtAuthenticationException("Missing Authorization Header");
        }
        String authHeader = request.getHeaders().getFirst("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        if (token == null) {
            throw new JwtAuthenticationException("Missing or Malformed Authorization Header");
        }
        if (!jwtUtil.validateToken(token)) {
            throw new JwtAuthenticationException("Invalid Authorization Token");
        }
        ServerHttpRequest mutatedRequest = request.mutate().header("X-User-ID", jwtUtil.extractUsername(token)).
                header("X-User-Roles", String.join(",", jwtUtil.extractRoles(token))).
                build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicEndpoint(String path, HttpMethod method) {
        if (publicEndpointsConfig.getPublicEndpoints() == null) {
            log.info("No public endpoints configured.");
            return false;
        }

        String methodName = method.name();
        List<String> patterns = publicEndpointsConfig.getPublicEndpoints().get(methodName);

        if (patterns == null || patterns.isEmpty()) {
            return false;
        }

        return patterns.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}

