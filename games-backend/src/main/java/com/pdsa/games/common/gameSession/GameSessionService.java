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

    // Create
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

    // Read All
    public List<GameSessionModel> getAllGameSessions() {
        return gameSessionRepository.findAll();
    }

    // Read by ID
    public Optional<GameSessionModel> getGameSessionById(Integer sessionId) {
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session ID is required");
        }
        return gameSessionRepository.findById(sessionId);
    }

    // Read by Game ID
    public List<GameSessionModel> getGameSessionsByGameId(Integer gameId) {
        if (gameId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game ID is required");
        }
        return gameSessionRepository.findAllByGameId(gameId);
    }

    // Read by Player ID
    public List<GameSessionModel> getGameSessionsByPlayerId(Integer playerId) {
        if (playerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player ID is required");
        }
        return gameSessionRepository.findAllByPlayerId(playerId);
    }

    // Read by Game ID and Player ID
    public List<GameSessionModel> getGameSessionsByGameIdAndPlayerId(Integer gameId, Integer playerId) {
        if (gameId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game ID is required");
        }
        if (playerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player ID is required");
        }
        return gameSessionRepository.findAllByGameIdAndPlayerId(gameId, playerId);
    }

    // Update (full replacement)
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

    // Patch (partial update)
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

    // Delete
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
