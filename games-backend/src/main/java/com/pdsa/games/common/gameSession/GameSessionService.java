package com.pdsa.games.common.gameSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class GameSessionService {
    @Autowired
    private GameSessionRepository gameSessionRepository;

    /**
     * Save a new game session after validating required fields.
     *
     * @param gameSession game session payload to persist
     * @return persisted game session model
     */
    public GameSessionModel saveGameSession(GameSessionModel gameSession) {
        if (gameSession.getGameId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game ID is required");
        }
        if (gameSession.getPlayerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player ID is required");
        }

        if (gameSession.getCreatedAt() == null) {
            gameSession.setCreatedAt(LocalDateTime.now());
        }

        return gameSessionRepository.save(gameSession);
    }

    /**
     * Retrieve all stored game sessions.
     *
     * @return list of game sessions
     */
    public List<GameSessionModel> getAllGameSessions() {
        return gameSessionRepository.findAll();
    }

    /**
     * Retrieve a game session by its session ID.
     *
     * @param sessionId identifier of the game session
     * @return optional game session model
     */
    public Optional<GameSessionModel> getGameSessionById(Integer sessionId) {
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session ID is required");
        }
        return gameSessionRepository.findById(sessionId);
    }

    /**
     * Retrieve sessions for a specific game.
     *
     * @param gameId game identifier to filter sessions
     * @return list of matching game sessions
     */
    public List<GameSessionModel> getGameSessionsByGameId(Integer gameId) {
        if (gameId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game ID is required");
        }
        return gameSessionRepository.findAllByGameId(gameId);
    }

    /**
     * Retrieve sessions for a specific player.
     *
     * @param playerId player identifier to filter sessions
     * @return list of matching game sessions
     */
    public List<GameSessionModel> getGameSessionsByPlayerId(Integer playerId) {
        if (playerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player ID is required");
        }
        return gameSessionRepository.findAllByPlayerId(playerId);
    }

    /**
     * Retrieve sessions for a specific game and player.
     *
     * @param gameId   game identifier
     * @param playerId player identifier
     * @return list of matching game sessions
     */
    public List<GameSessionModel> getGameSessionsByGameIdAndPlayerId(Integer gameId, Integer playerId) {
        if (gameId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game ID is required");
        }
        if (playerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player ID is required");
        }
        return gameSessionRepository.findAllByGameIdAndPlayerId(gameId, playerId);
    }

    /**
     * Fully update an existing game session.
     *
     * @param sessionId   identifier of the session to update
     * @param gameSession payload containing replacement session values
     * @return updated game session model
     */
    public GameSessionModel updateGameSession(Integer sessionId, GameSessionModel gameSession) {
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session ID is required");
        }
        if (gameSession.getGameId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game ID is required");
        }
        if (gameSession.getPlayerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player ID is required");
        }

        Optional<GameSessionModel> existingSession = gameSessionRepository.findById(sessionId);
        if (existingSession.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found with ID: " + sessionId);
        }

        GameSessionModel session = existingSession.get();
        session.setGameId(gameSession.getGameId());
        session.setPlayerId(gameSession.getPlayerId());

        return gameSessionRepository.save(session);
    }

    /**
     * Partially update an existing game session.
     *
     * @param sessionId   identifier of the session to patch
     * @param gameSession payload containing fields to update
     * @return patched game session model
     */
    public GameSessionModel patchGameSession(Integer sessionId, GameSessionModel gameSession) {
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session ID is required");
        }

        Optional<GameSessionModel> existingSession = gameSessionRepository.findById(sessionId);
        if (existingSession.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found with ID: " + sessionId);
        }

        if (gameSession.getGameId() == null && gameSession.getPlayerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one field (gameId or playerId) must be provided for update");
        }

        GameSessionModel session = existingSession.get();
        if (gameSession.getGameId() != null) {
            session.setGameId(gameSession.getGameId());
        }
        if (gameSession.getPlayerId() != null) {
            session.setPlayerId(gameSession.getPlayerId());
        }

        return gameSessionRepository.save(session);
    }

    /**
     * Delete a game session by its session ID.
     *
     * @param sessionId identifier of the session to delete
     */
    public void deleteGameSession(Integer sessionId) {
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session ID is required");
        }

        Optional<GameSessionModel> existingSession = gameSessionRepository.findById(sessionId);
        if (existingSession.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found with ID: " + sessionId);
        }

        gameSessionRepository.deleteById(sessionId);
    }
}
