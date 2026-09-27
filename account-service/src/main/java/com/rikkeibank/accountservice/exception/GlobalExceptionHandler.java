package com.rikkeibank.accountservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.contains("Access denied")) {
            return buildError(HttpStatus.FORBIDDEN, msg);
        }
        if (msg.toLowerCase().contains("not found")) {
            return buildError(HttpStatus.NOT_FOUND, msg);
        }
        return buildError(HttpStatus.BAD_REQUEST, msg);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        Map<String, Object> body = Map.of("timestamp", LocalDateTime.now().toString(),
                "status", 400, "error", "Validation Failed", "fieldErrors", errors);
        return ResponseEntity.badRequest().body(body);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }
    private ResponseEntity<Map<String, Object>> buildError(HttpStatus s, String msg) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", s.value()); body.put("error", s.getReasonPhrase()); body.put("message", msg);
        return ResponseEntity.status(s).body(body);
    }
}
