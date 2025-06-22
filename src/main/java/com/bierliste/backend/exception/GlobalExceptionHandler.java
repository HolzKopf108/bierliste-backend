package com.bierliste.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.servlet.ServletException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String,String> errs = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
          .forEach(e -> errs.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errs);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String,String>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String,String> errs = new HashMap<>();
        ex.getConstraintViolations()
          .forEach(v -> errs.put(v.getPropertyPath().toString(), v.getMessage()));
        return ResponseEntity.badRequest().body(errs);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> handleStatus(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleServlet(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Email oder Passwort ist falsch"));
    }

    @ExceptionHandler(ServletException.class)
    public ResponseEntity<Map<String, String>> handleServlet(ServletException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Email oder Passwort ist falsch"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,String>> handleAll(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error: " + ex.getClass()));
    }
}