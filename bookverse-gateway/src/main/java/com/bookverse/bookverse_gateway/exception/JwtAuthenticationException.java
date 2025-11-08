package com.bookverse.bookverse_gateway.exception;

import org.springframework.web.server.ResponseStatusException;

public class JwtAuthenticationException extends ResponseStatusException{
    public JwtAuthenticationException(String reason) {
        super(org.springframework.http.HttpStatus.UNAUTHORIZED, reason);
    }
}
