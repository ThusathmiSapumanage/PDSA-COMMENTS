package com.pdsa.games.common.exception;

import java.sql.DataTruncation;
import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle Spring exceptions that carry an explicit HTTP status.
     *
     * @param ex the exception containing status and reason
     * @param request servlet request used to record the request URI
     * @return problem detail response with the original status code
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request) {
        ProblemDetail body = ProblemDetail.forStatus(ex.getStatusCode());
        body.setTitle(ex.getStatusCode().toString());
        body.setDetail(ex.getReason() != null ? ex.getReason() : "Request could not be processed");
        body.setProperty("timestamp", Instant.now().toString());
        body.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    /**
     * Handle database constraint exceptions and return a user-friendly bad request.
     *
     * @param ex the root exception from database or JPA system errors
     * @param request servlet request used to record the request URI
     * @return problem detail response with HTTP 400 Bad Request
     */
    @ExceptionHandler({DataIntegrityViolationException.class, JpaSystemException.class})
    public ResponseEntity<ProblemDetail> handleDatabaseConstraintErrors(
            RuntimeException ex,
            HttpServletRequest request) {
        Throwable root = getRootCause(ex);
        String rootMessage = root != null && root.getMessage() != null ? root.getMessage() : ex.getMessage();

        ProblemDetail body = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        body.setTitle("Bad Request");

        if (root instanceof DataTruncation || containsIgnoreCase(rootMessage, "Data too long for column")) {
            body.setDetail("One or more fields exceed the allowed length.");
        } else {
            body.setDetail("Request violates database constraints.");
        }

        body.setProperty("timestamp", Instant.now().toString());
        body.setProperty("path", request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Traverse the exception chain to find the original root cause.
     *
     * @param ex exception to inspect
     * @return deepest nested cause, or the original exception if none
     */
    private Throwable getRootCause(Throwable ex) {
        Throwable current = ex;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * Check whether text contains the specified needle, ignoring case.
     *
     * @param text text to search within
     * @param needle substring to look for
     * @return true when needle appears in text, false otherwise
     */
    private boolean containsIgnoreCase(String text, String needle) {
        return text != null && needle != null && text.toLowerCase().contains(needle.toLowerCase());
    }
}
