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

    private Throwable getRootCause(Throwable ex) {
        Throwable current = ex;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private boolean containsIgnoreCase(String text, String needle) {
        return text != null && needle != null && text.toLowerCase().contains(needle.toLowerCase());
    }
}
