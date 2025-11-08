package com.bookverse.bookverse_gateway.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.support.DefaultServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.ConnectException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalErrorWebExceptionHandlerTest {

    private GlobalErrorWebExceptionHandler handler;
    private ApplicationContext context;

    @BeforeEach
    void setUp() {
        ErrorAttributes errorAttributes = new DefaultErrorAttributes();
        WebProperties webProperties = new WebProperties();
        context = new StaticApplicationContext();
        handler = new GlobalErrorWebExceptionHandler(
                errorAttributes,
                webProperties,
                context,
                new DefaultServerCodecConfigurer()
        );
    }

    @Test
    void shouldReturn503ForConnectException() {
        ConnectException error = new ConnectException("Connection refused");
        ErrorAttributes errorAttributes = new DefaultErrorAttributes();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/book").build());
        exchange.getAttributes().put("org.springframework.boot.web.reactive.error.DefaultErrorAttributes.ERROR", error);

        ServerRequest request = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());

        RouterFunction<ServerResponse> routingFunction = handler.getRoutingFunction(errorAttributes);

        Mono<ServerResponse> responseMono = routingFunction.route(request)
                .flatMap(handlerFunction -> handlerFunction.handle(request));

        StepVerifier.create(responseMono)
                .consumeNextWith(response ->
                    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode())
                )
                .verifyComplete();
    }

    @Test
    void shouldReturn503ForRuntimeException() {
        RuntimeException error = new RuntimeException("Unexpected failure");
        ErrorAttributes errorAttributes = new DefaultErrorAttributes();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test").build());
        exchange.getAttributes().put("org.springframework.boot.web.reactive.error.DefaultErrorAttributes.ERROR", error);

        ServerRequest request = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());

        RouterFunction<ServerResponse> routingFunction = handler.getRoutingFunction(errorAttributes);

        Mono<ServerResponse> responseMono = routingFunction.route(request)
                .flatMap(handlerFunction -> handlerFunction.handle(request));

        StepVerifier.create(responseMono)
                .consumeNextWith(response ->
                    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode())
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnCustomStatusAndMessageFromErrorAttributes() {
        IllegalArgumentException error = new IllegalArgumentException("Book not found");

        // Create custom error attributes that return 404 status
        ErrorAttributes customErrorAttributes = new ErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
                Map<String, Object> errorAttributes = new java.util.HashMap<>();
                errorAttributes.put("status", 404);
                errorAttributes.put("message", "Book not found");
                return errorAttributes;
            }

            @Override
            public Throwable getError(ServerRequest request) {
                return error;
            }

            @Override
            public void storeErrorInformation(Throwable error, ServerWebExchange exchange) {
                exchange.getAttributes().putIfAbsent("org.springframework.boot.web.reactive.error.DefaultErrorAttributes.ERROR", error);
            }
        };

        GlobalErrorWebExceptionHandler customHandler = new GlobalErrorWebExceptionHandler(
                customErrorAttributes,
                new WebProperties(),
                context,
                new DefaultServerCodecConfigurer()
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test/custom").build());
        exchange.getAttributes().put("org.springframework.boot.web.reactive.error.DefaultErrorAttributes.ERROR", error);

        ServerRequest request = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());

        RouterFunction<ServerResponse> routingFunction = customHandler.getRoutingFunction(customErrorAttributes);

        Mono<ServerResponse> responseMono = routingFunction.route(request)
                .flatMap(handlerFunction -> handlerFunction.handle(request));

        StepVerifier.create(responseMono)
                .consumeNextWith(response ->
                    assertEquals(HttpStatus.NOT_FOUND, response.statusCode())
                )
                .verifyComplete();
    }
}

