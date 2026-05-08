package com.pdsa.games.knightstour;

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

@RestControllerAdvice(assignableTypes = KnightsTourController.class)
public class KnightsTourExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(KnightsTourExceptionHandler.class);

    /**
     * Handle exceptions that already include an HTTP status.
     *
     * @param ex the response status exception
     * @return structured error body with the original status and reason
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return buildResponse(status, ex.getReason() == null ? status.getReasonPhrase() : ex.getReason());
    }

    /**
     * Handle illegal argument exceptions as bad requests.
     *
     * @param ex the exception raised during argument validation
     * @return structured bad request response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handle malformed JSON or invalid request body payloads.
     *
     * @param ex exception thrown when the request body cannot be parsed
     * @return structured bad request response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed request body.");
    }

    /**
     * Handle database access errors and log the failure.
     *
     * @param ex database exception
     * @return structured internal server error response
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDatabase(DataAccessException ex) {
        logger.error("Database exception in KnightsTour", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "A database error occurred.");
    }

    /**
     * Handle unexpected exceptions not covered by other handlers.
     *
     * @param ex unhandled exception
     * @return structured internal server error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        logger.error("Unhandled exception in KnightsTour", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred.");
    }

    /**
     * Build a uniform error response body for exception handlers.
     *
     * @param status HTTP status to return
     * @param message error message to include
     * @return response entity with structured error payload
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