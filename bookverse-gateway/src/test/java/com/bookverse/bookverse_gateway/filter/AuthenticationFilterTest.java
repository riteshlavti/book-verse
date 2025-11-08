package com.bookverse.bookverse_gateway.filter;

import com.bookverse.bookverse_gateway.AuthenticationFilter;
import com.bookverse.bookverse_gateway.config.PublicEndpointConfig;
import com.bookverse.bookverse_gateway.exception.JwtAuthenticationException;
import com.bookverse.bookverse_gateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private PublicEndpointConfig publicEndpointsConfig;
    @Mock private GatewayFilterChain chain;

    @InjectMocks private AuthenticationFilter filter;

    @BeforeEach
    void setup() {
        // lenient avoids "Unnecessary stubbing" warnings in tests that never reach chain.filter()
        lenient().when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        lenient().when(publicEndpointsConfig.getPublicEndpoints()).thenReturn(Map.of());
    }

    @Test
    void shouldAllowPublicEndpoint() {
        Map<String, List<String>> publicEndpoints = new HashMap<>();
        publicEndpoints.put("GET", List.of("/api/public/**"));
        when(publicEndpointsConfig.getPublicEndpoints()).thenReturn(publicEndpoints);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/public/health").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(any());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void shouldThrowWhenMissingAuthorizationHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/secure/book").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(Mono.fromCallable(() -> {
                    filter.filter(exchange, chain).block(); // trigger synchronous throw
                    return null;
                }))
                .expectErrorSatisfies(ex -> {
                    assert ex instanceof JwtAuthenticationException;
                    JwtAuthenticationException jwtEx = (JwtAuthenticationException) ex;
                    assert jwtEx.getStatusCode().equals(HttpStatus.UNAUTHORIZED);
                    assert jwtEx.getReason().contains("Missing Authorization Header");
                })
                .verify();
    }

    @Test
    void shouldThrowWhenMalformedHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/book/1")
                .header(HttpHeaders.AUTHORIZATION, "InvalidTokenHeader")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(Mono.fromCallable(() -> {
                    filter.filter(exchange, chain).block();
                    return null;
                }))
                .expectErrorSatisfies(ex -> {
                    assert ex instanceof JwtAuthenticationException;
                    JwtAuthenticationException jwtEx = (JwtAuthenticationException) ex;
                    assert jwtEx.getStatusCode().equals(HttpStatus.UNAUTHORIZED);
                    assert jwtEx.getReason().contains("Malformed Authorization Header");
                })
                .verify();

        verify(chain, never()).filter(any());
    }

    @Test
    void shouldThrowWhenInvalidToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/book/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalidToken")
                .build();
        when(jwtUtil.validateToken("invalidToken")).thenReturn(false);
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(Mono.fromCallable(() -> {
                    filter.filter(exchange, chain).block();
                    return null;
                }))
                .expectErrorSatisfies(ex -> {
                    assert ex instanceof JwtAuthenticationException;
                    JwtAuthenticationException jwtEx = (JwtAuthenticationException) ex;
                    assert jwtEx.getStatusCode().equals(HttpStatus.UNAUTHORIZED);
                    assert jwtEx.getReason().contains("Invalid Authorization Token");
                })
                .verify();

        verify(chain, never()).filter(any());
    }

    @Test
    void shouldPassWhenValidToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/book/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer validToken")
                .build();

        when(jwtUtil.validateToken("validToken")).thenReturn(true);
        when(jwtUtil.extractUsername("validToken")).thenReturn("user123");
        when(jwtUtil.extractRoles("validToken")).thenReturn(List.of("ROLE_USER"));

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(any());
        verify(jwtUtil).validateToken("validToken");
        verify(jwtUtil).extractUsername("validToken");
        verify(jwtUtil).extractRoles("validToken");
    }
}
