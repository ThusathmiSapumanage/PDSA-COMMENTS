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

	/**
	 * Construct the game service.
	 *
	 * @param gameRepository repository used to access game data
	 */
	public GameService(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	/**
	 * Save a new game after validating the provided payload.
	 *
	 * @param game game data to persist
	 * @return created game with generated id
	 */
	public GameModel saveGame(GameModel game) {
		if (game.getGameId() != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gameId must not be provided when creating a game");
		}

		validateGameName(game.getGameName(), false);

		return gameRepository.save(game);
	}

	/**
	 * Retrieve all games.
	 *
	 * @return list of all games
	 */
	public List<GameModel> getAllGames() {
		return gameRepository.findAll();
	}

	/**
	 * Retrieve a single game by id.
	 *
	 * @param gameId game identifier
	 * @return optional game model
	 */
	public Optional<GameModel> getGameById(Integer gameId) {
		return gameRepository.findById(gameId);
	}

	/**
	 * Replace an existing game with new values.
	 *
	 * @param gameId game identifier from the path
	 * @param game replacement game payload
	 * @return updated game when found, otherwise empty
	 */
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

	/**
	 * Partially update mutable fields of an existing game.
	 *
	 * @param gameId game identifier from the path
	 * @param game game payload containing patch fields
	 * @return updated game when found, otherwise empty
	 */
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

	/**
	 * Validate the game name against required constraints.
	 *
	 * @param gameName name of the game to validate
	 * @param allowNull allow null for patch operations
	 */
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

	/**
	 * Delete a game by its identifier.
	 *
	 * @param gameId identifier of the game to delete
	 */
	public void deleteGame(Integer gameId) {
		gameRepository.deleteById(gameId);
	}
}
