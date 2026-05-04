package com.pdsa.games.common.game;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
public class GameService {
	private static final int GAME_NAME_MAX_LENGTH = 25;

	private final GameRepository gameRepository;

	public GameService(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	public GameModel saveGame(GameModel game) {
		if (game.getGameId() != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gameId must not be provided when creating a game");
		}

		validateGameName(game.getGameName(), false);

		return gameRepository.save(game);
	}

	public List<GameModel> getAllGames() {
		return gameRepository.findAll();
	}

	public Optional<GameModel> getGameById(Integer gameId) {
		return gameRepository.findById(gameId);
	}

	public Optional<GameModel> updateGame(Integer gameId, GameModel game) {
		if (game.getGameId() != null && !gameId.equals(game.getGameId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gameId in path and body must match");
		}

		validateGameName(game.getGameName(), false);

		return gameRepository.findById(gameId)
				.map(existing -> {
					existing.setGameName(game.getGameName());
					return gameRepository.save(existing);
				});
	}

	public Optional<GameModel> patchGame(Integer gameId, GameModel game) {
		if (game.getGameId() != null && !gameId.equals(game.getGameId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gameId in path and body must match");
		}

		validateGameName(game.getGameName(), true);

		return gameRepository.findById(gameId)
				.map(existing -> {
					if (game.getGameName() != null) {
						existing.setGameName(game.getGameName());
					}
					return gameRepository.save(existing);
				});
	}

	private void validateGameName(String gameName, boolean allowNull) {
		if (gameName == null) {
			if (allowNull) {
				return;
			}
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gameName is required");
		}

		if (gameName.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gameName must not be blank");
		}

		if (gameName.length() > GAME_NAME_MAX_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"gameName must be at most " + GAME_NAME_MAX_LENGTH + " characters");
		}
	}

	public void deleteGame(Integer gameId) {
		gameRepository.deleteById(gameId);
	}
}
