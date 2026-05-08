package com.pdsa.games.common.gameSession;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/game-sessions")
@Tag(name = "Game Sessions", description = "API for managing game sessions")
public class GameSessionController {
    @Autowired
    private GameSessionService gameSessionService;

    /**
     * Create a new game session.
     *
     * @param gameSession game session payload to store
     * @return created game session with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new game session", description = "Creates a new game session with the provided game ID and player ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Game session created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameSessionModel.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input provided"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<GameSessionModel> createGameSession(@RequestBody GameSessionModel gameSession) {
        GameSessionModel createdSession = gameSessionService.saveGameSession(gameSession);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSession);
    }

    /**
     * Retrieve all game sessions.
     *
     * @return list of all stored game sessions
     */
    @GetMapping
    @Operation(summary = "Get all game sessions", description = "Retrieves all game sessions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of game sessions retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameSessionModel.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<GameSessionModel>> getAllGameSessions() {
        List<GameSessionModel> sessions = gameSessionService.getAllGameSessions();
        return ResponseEntity.ok(sessions);
    }

    /**
     * Retrieve a game session by its unique session ID.
     *
     * @param sessionId unique session identifier
     * @return the matching game session or 404 if not found
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "Get game session by ID", description = "Retrieves a specific game session by its session ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game session retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameSessionModel.class))),
            @ApiResponse(responseCode = "404", description = "Game session not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<GameSessionModel> getGameSessionById(@PathVariable Integer sessionId) {
        Optional<GameSessionModel> session = gameSessionService.getGameSessionById(sessionId);
        return session.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieve all sessions associated with a specific game.
     *
     * @param gameId game identifier to filter sessions
     * @return list of sessions for the given game or 404 when none exist
     */
    @GetMapping("/game/{gameId}")
    @Operation(summary = "Get game sessions by game ID", description = "Retrieves all game sessions for a specific game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game sessions retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameSessionModel.class))),
            @ApiResponse(responseCode = "404", description = "No sessions found for the game"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<GameSessionModel>> getGameSessionsByGameId(@PathVariable Integer gameId) {
        List<GameSessionModel> sessions = gameSessionService.getGameSessionsByGameId(gameId);
        if (sessions.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sessions);
    }

    /**
     * Retrieve all sessions associated with a specific player.
     *
     * @param playerId player identifier to filter sessions
     * @return list of sessions for the player or 404 when none exist
     */
    @GetMapping("/player/{playerId}")
    @Operation(summary = "Get game sessions by player ID", description = "Retrieves all game sessions for a specific player")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game sessions retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameSessionModel.class))),
            @ApiResponse(responseCode = "404", description = "No sessions found for the player"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<GameSessionModel>> getGameSessionsByPlayerId(@PathVariable Integer playerId) {
        List<GameSessionModel> sessions = gameSessionService.getGameSessionsByPlayerId(playerId);
        if (sessions.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sessions);
    }

    /**
     * Retrieve all sessions for a given game and player combination.
     *
     * @param gameId   game identifier
     * @param playerId player identifier
     * @return matching sessions or 404 when none exist
     */
    @GetMapping("/game/{gameId}/player/{playerId}")
    @Operation(summary = "Get game sessions by game ID and player ID", description = "Retrieves game sessions for a specific game and player combination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game sessions retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameSessionModel.class))),
            @ApiResponse(responseCode = "404", description = "No sessions found for the game and player combination"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<GameSessionModel>> getGameSessionsByGameIdAndPlayerId(@PathVariable Integer gameId, @PathVariable Integer playerId) {
        List<GameSessionModel> sessions = gameSessionService.getGameSessionsByGameIdAndPlayerId(gameId, playerId);
        if (sessions.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sessions);
    }

    /**
     * Fully replace an existing game session.
     *
     * @param sessionId   identifier of the session to update
     * @param gameSession replacement payload for the session
     * @return updated game session when successful
     */
    @PutMapping("/{sessionId}")
    @Operation(summary = "Update game session", description = "Fully updates a game session with provided game ID and player ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game session updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameSessionModel.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input provided"),
            @ApiResponse(responseCode = "404", description = "Game session not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<GameSessionModel> updateGameSession(@PathVariable Integer sessionId, @RequestBody GameSessionModel gameSession) {
        GameSessionModel updatedSession = gameSessionService.updateGameSession(sessionId, gameSession);
        return ResponseEntity.ok(updatedSession);
    }

    /**
     * Partially update an existing game session.
     *
     * @param sessionId   identifier of the session to patch
     * @param gameSession payload containing fields to update
     * @return patched game session when successful
     */
    @PatchMapping("/{sessionId}")
    @Operation(summary = "Partially update game session", description = "Partially updates a game session with provided fields (gameId and/or playerId)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game session patched successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameSessionModel.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input provided"),
            @ApiResponse(responseCode = "404", description = "Game session not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<GameSessionModel> patchGameSession(@PathVariable Integer sessionId, @RequestBody GameSessionModel gameSession) {
        GameSessionModel patchedSession = gameSessionService.patchGameSession(sessionId, gameSession);
        return ResponseEntity.ok(patchedSession);
    }

    /**
     * Delete a game session by its session ID.
     *
     * @param sessionId identifier of the session to remove
     * @return HTTP 204 when deletion succeeds
     */
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Delete game session", description = "Deletes a game session by its session ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Game session deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Game session not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteGameSession(@PathVariable Integer sessionId) {
        gameSessionService.deleteGameSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
