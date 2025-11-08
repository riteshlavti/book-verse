package com.bookverse.userservice.exception;

import com.bookverse.userservice.exception.serviceLevel.AuthServiceException;
import com.bookverse.userservice.exception.serviceLevel.UserServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserServiceException.class)
    public ResponseEntity<Map<String, Object>> handleUserServiceException(UserServiceException ex) {
        Map<String, Object> body = new HashMap<>();

        body.put("message", ex.getMessage());
        if( ex.getMessage().toLowerCase().contains("not found")) {
            body.put("status", HttpStatus.NOT_FOUND.value());
            body.put("error", "Resource Not Found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "User Service Error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(AuthServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAuthServiceException(AuthServiceException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", ex.getMessage());
        if( ex.getMessage().toLowerCase().contains("not found")) {
            body.put("status", HttpStatus.NOT_FOUND.value());
            body.put("error", "Resource Not Found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Auth Service Error");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Unexpected Error");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex){
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));

        body.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Access Denied");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
}
