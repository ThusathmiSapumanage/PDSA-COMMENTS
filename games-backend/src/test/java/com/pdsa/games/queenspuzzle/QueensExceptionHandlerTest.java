package com.pdsa.games.queenspuzzle;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class QueensExceptionHandlerTest {

    private final QueensExceptionHandler handler = new QueensExceptionHandler();

    @Test
    void handleBadRequest_shouldReturn400WithMessage() {
        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(
                new IllegalArgumentException("Board size must be between 4 and 16."));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertEquals("Bad Request", body.get("error"));
        assertEquals("Board size must be between 4 and 16.", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void handleMalformedBody_shouldReturn400() {
        ResponseEntity<Map<String, Object>> response = handler.handleMalformedBody(
                new HttpMessageNotReadableException("bad json", mock(HttpInputMessage.class)));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed request body.", response.getBody().get("message"));
    }

    @Test
    void handleResponseStatus_shouldForwardStatusAndReason() {
        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Session not found", response.getBody().get("message"));
    }

    @Test
    void handleIllegalState_shouldReturn500() {
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalState(
                new IllegalStateException("Game configuration missing"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Game configuration missing", response.getBody().get("message"));
    }

    @Test
    void handleDataAccess_shouldReturn500WithGenericMessage() {
        ResponseEntity<Map<String, Object>> response = handler.handleDataAccess(
                new DataAccessException("DB down") {});

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(((String) response.getBody().get("message")).contains("database error"));
    }

    @Test
    void handleGeneric_shouldReturn500() {
        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(
                new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An internal server error occurred.", response.getBody().get("message"));
    }
}
