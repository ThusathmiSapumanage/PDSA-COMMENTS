package com.pdsa.games.queenspuzzle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for Sixteen Queens controller errors.
 *
 * Converts common runtime and data access exceptions into structured JSON responses.
 */
@RestControllerAdvice(assignableTypes = QueensController.class)
public class QueensExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(QueensExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    /**
     * Handles bad request validation failures.
     */
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    /**
     * Handles invalid JSON or malformed request payloads.
     */
    public ResponseEntity<Map<String, Object>> handleMalformedBody(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed request body.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    /**
     * Handles exceptions that already carry a specific HTTP status.
     */
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return buildResponse(status, ex.getReason() != null ? ex.getReason() : status.getReasonPhrase());
    }

    @ExceptionHandler(IllegalStateException.class)
    /**
     * Handles internal illegal state conditions thrown by the game service.
     */
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        logger.warn("Illegal state in Sixteen Queens", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    /**
     * Handles database access errors from JDBC operations.
     */
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException ex) {
        logger.error("Database error in Sixteen Queens", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "A database error occurred while processing the request.");
    }

    @ExceptionHandler(Exception.class)
    /**
     * Handles unexpected exceptions as generic internal server errors.
     */
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        logger.error("Unhandled exception in Sixteen Queens", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred.");
    }

    /**
     * Builds a consistent JSON error payload for exception responses.
     *
     * @param status HTTP status to return
     * @param message error message to include
     * @return response entity with structured error body
     */
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
