package com.bookverse.bookverse_gateway.exception;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(-1)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(
            ErrorAttributes errorAttributes,
            WebProperties webProperties,
            ApplicationContext applicationContext,
            ServerCodecConfigurer serverCodecConfigurer) {

        super(errorAttributes, webProperties.getResources(), applicationContext);
        this.setMessageWriters(serverCodecConfigurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);
        Map<String, Object> errorPropertiesMap = getErrorAttributes(request, ErrorAttributeOptions.defaults());

        int status = (int) errorPropertiesMap.getOrDefault("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        String errorMessage = (String) errorPropertiesMap.getOrDefault("message", "An unexpected error occurred.");

        if (isConnectionError(error)) {
            errorMessage = "Gateway could not connect to the downstream service. Please try again.";
            status = HttpStatus.SERVICE_UNAVAILABLE.value();
        } else if (status == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            errorMessage = "Gateway could not connect to the downstream service. Please try again.";
            status = HttpStatus.SERVICE_UNAVAILABLE.value();
        }

        Map<String, Object> customError = new HashMap<>();
        customError.put("timestamp", new Date());
        customError.put("status", status);
        customError.put("error", HttpStatus.valueOf(status).getReasonPhrase());
        customError.put("message", errorMessage);
        customError.put("path", request.path());

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(customError);
    }
    private boolean isConnectionError(Throwable error) {
        if (error == null) return false;

        String errorClass = error.getClass().getName();
        return errorClass.contains("ConnectException") ||
                errorClass.contains("TimeoutException") ||
                error.getMessage() != null && error.getMessage().contains("Connection refused");
    }
}
