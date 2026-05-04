package com.pdsa.games.mincost;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * MIN COST EXCEPTION HANDLER:
 * This is a professional "Global Error Handler".
 * It catches errors for the whole MinCost package and returns them as clean JSON.
 * This removes the need for messy try-catch blocks in the Controller.
 */
@RestControllerAdvice(basePackages = "com.pdsa.games.mincost")
public class MinCostExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        // Return 400 Bad Request if it's a validation error
        return ResponseEntity.status(400).body(Map.of(
                "status", "error",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception ex) {
        // Return 500 for any other unexpected server errors
        return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "An unexpected system error occurred!"
        ));
    }
}
