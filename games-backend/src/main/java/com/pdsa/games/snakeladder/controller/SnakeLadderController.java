package com.pdsa.games.snakeladder.controller;

import com.pdsa.games.snakeladder.model.AlgorithmResult;
import com.pdsa.games.snakeladder.model.SnakeLadderItem;
import com.pdsa.games.snakeladder.service.SnakeLadderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST Controller for the Snake and Ladder game.
 *
 * Endpoints:
 * - POST /api/snake-ladder/start → Start a new round
 * - POST /api/snake-ladder/answer → Submit player answer
 */
@RestController
@RequestMapping("/api/snake-ladder")
@CrossOrigin
@Tag(name = "Snake and Ladder", description = "Snake and Ladder game endpoints")
public class SnakeLadderController {

    private final SnakeLadderService service;
    private final Map<Integer, Integer> sessionAnswers = new ConcurrentHashMap<>();

    /**
     * Constructs the controller with the SnakeLadderService dependency.
     *
     * @param service service layer for Snake and Ladder game operations
     */
    public SnakeLadderController(SnakeLadderService service) {
        this.service = service;
    }

    /**
     * Starts a new Snake and Ladder round by generating a random board, running
     * both solution algorithms, saving the game session, and returning answer choices.
     *
     * @param request round start request payload containing playerId and board size
     * @return response with session data, board items, algorithm timing, and choices
     */
    @PostMapping("/start")
    public ResponseEntity<StartRoundResponse> startRound(@RequestBody StartRoundRequest request) {
        try {
            service.validateN(request.getN());
            int playerId = request.getPlayerId();
            int n = request.getN();
            int totalCells = n * n;

            // Generate the random board (pass 0 as placeholder — will be re-stamped in
            // saveGameSession)
            List<SnakeLadderItem> items = service.generateBoard(n, 0);

            // Run both algorithms
            List<AlgorithmResult> algorithmResults = service.runAlgorithms(items, totalCells);

            int correctAnswer = algorithmResults.get(0).getMinimumThrows();

            // Save to DB — this creates the Game_Session and returns the real sessionId
            int sessionId = service.saveGameSession(
                    playerId, n, items, algorithmResults, correctAnswer);

            // Store correct answer server-side under the real sessionId
            sessionAnswers.put(sessionId, correctAnswer);

            List<Integer> choices = service.generateChoices(correctAnswer, items, n);

            StartRoundResponse response = new StartRoundResponse(
                    sessionId,
                    n,
                    items,
                    algorithmResults,
                    choices);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            // Temporarily print the real error so we can see what's failing
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/answer")
    @Operation(summary = "Submit player's answer", description = "Submit the player's choice and get WIN/LOSE/DRAW feedback.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Answer processed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubmitAnswerResponse.class), examples = @ExampleObject(name = "Win response", value = "{\"outcome\": \"WIN\", \"message\": \"Correct!\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    /**
     * Submits the player's selected answer and returns whether it was correct.
     *
     * The real correct answer is stored server-side so the submitted request body
     * cannot be trusted.
     *
     * @param request answer submission payload containing sessionId and playerAnswer
     * @return response with WIN/DRAW/LOSE outcome and user-facing message
     */
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(@RequestBody SubmitAnswerRequest request) {
        try {
            int sessionId = request.getSessionId();
            int playerAnswer = request.getPlayerAnswer();

            // Load the correct answer server-side (don't trust the request body)
            int correctAnswer = sessionAnswers.getOrDefault(sessionId, -1);
            if (correctAnswer == -1) {
                return ResponseEntity.badRequest().build(); // session not found
            }

            // Remove it so it can't be reused
            sessionAnswers.remove(sessionId);

            String outcomeStr = service.determineOutcome(playerAnswer, correctAnswer);
            GameOutcome outcome = GameOutcome.valueOf(outcomeStr);

            String message = switch (outcomeStr) {
                case "WIN" -> "Correct! The minimum throws is " + correctAnswer + ".";
                case "DRAW" -> "Close! You were only 1 throw off. The correct answer was " + correctAnswer + ".";
                case "LOSE" -> "Incorrect. The correct answer was " + correctAnswer
                        + " but you chose " + playerAnswer
                        + ". You were off by "
                        + Math.abs(playerAnswer - correctAnswer) + " throws.";
                default -> "Unknown outcome.";
            };

            SubmitAnswerResponse response = new SubmitAnswerResponse(outcome, message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================================
    // Request/Response DTOs
    // =========================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartRoundRequest {
        private int playerId;
        private int n;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartRoundResponse {
        private int sessionId;
        private int n;
        private List<SnakeLadderItem> items;
        private List<AlgorithmResult> algorithmResults;
        private List<Integer> choices;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitAnswerRequest {
        private int sessionId;
        private int playerAnswer;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitAnswerResponse {
        private GameOutcome outcome;
        private String message;
    }

    /**
     * Possible outcomes after a player submits their answer.
     */
    public enum GameOutcome {
        WIN, // player chose the correct minimum throws
        LOSE, // player was wrong
        DRAW // player was off by only 1
    }

    // =========================================================================
    // Global Exception Handler
    // =========================================================================

    /**
     * Handles invalid JSON payloads and returns a structured bad request error.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleJsonParseError(HttpMessageNotReadableException ex) {
        ex.printStackTrace();
        System.err.println("JSON DESERIALIZATION ERROR: " + ex.getMessage());
        Throwable cause = ex.getCause();
        if (cause != null) {
            System.err.println("Root cause: " + cause.getMessage());
            cause.printStackTrace();
        }
        return ResponseEntity.badRequest().body(
                Map.of("error", "Invalid JSON: " + ex.getMessage()));
    }
}