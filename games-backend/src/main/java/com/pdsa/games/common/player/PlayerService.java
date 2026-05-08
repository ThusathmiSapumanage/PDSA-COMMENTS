package com.pdsa.games.common.player;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlayerService {
	private static final int PLAYER_NAME_MAX_LENGTH = 45;
	private static final int PLAYER_EMAIL_MIN_LENGTH = 3;
	private static final int PLAYER_EMAIL_MAX_LENGTH = 45;
	private static final int PLAYER_PASSWORD_MIN_LENGTH = 6;
	private static final int PLAYER_PASSWORD_MAX_LENGTH = 255;
	private static final Pattern PLAYER_EMAIL_REGEX = Pattern
			.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

	private final PlayerRepository playerRepository;
	private final PasswordEncoder passwordEncoder;

	public PlayerService(PlayerRepository playerRepository) {
		this.playerRepository = playerRepository;
		this.passwordEncoder = new BCryptPasswordEncoder();
	}

	/**
	 * Save a new player after validating the provided profile data.
	 *
	 * @param player player profile to create
	 * @return created player model with encoded password
	 */
	public PlayerModel savePlayer(PlayerModel player) {
		if (player.getPlayerId() != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Invalid request: player id is generated automatically.");
		}

		validatePlayerName(player.getPlayerName(), false);
		validatePlayerEmail(player.getPlayerEmail(), false);
		validatePlayerPassword(player.getPlayerPassword(), false);

		player.setPlayerPassword(passwordEncoder.encode(player.getPlayerPassword()));

		return playerRepository.save(player);
	}

	public List<PlayerModel> getAllPlayers() {
		return playerRepository.findAll();
	}

	/**
	 * Retrieve all players.
	 *
	 * @return list of player models
	 */
	public Optional<PlayerModel> getPlayerById(Integer playerId) {
		return playerRepository.findById(playerId);
	}

	/**
	 * Authenticate a player by email and password.
	 *
	 * @param playerEmail player login email
	 * @param playerPassword player login password
	 * @return optional player model when authentication succeeds
	 */
	public Optional<PlayerModel> loginPlayer(String playerEmail, String playerPassword) {
		if (playerEmail == null || playerEmail.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter your email address.");
		}

		if (playerPassword == null || playerPassword.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter your password.");
		}

		return playerRepository.findByPlayerEmail(playerEmail)
				.filter(player -> passwordEncoder.matches(playerPassword, player.getPlayerPassword()));
	}

	/**
	 * Fully update an existing player profile.
	 *
	 * @param playerId identifier of the player to update
	 * @param player replacement player payload
	 * @return optional updated player model
	 */
	public Optional<PlayerModel> updatePlayer(Integer playerId, PlayerModel player) {
		if (player.getPlayerId() != null && !playerId.equals(player.getPlayerId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Invalid request: path id and payload id do not match.");
		}

		validatePlayerName(player.getPlayerName(), false);
		validatePlayerEmail(player.getPlayerEmail(), false);
		validatePlayerPassword(player.getPlayerPassword(), false);

		return playerRepository.findById(playerId)
				.map(existing -> {
					existing.setPlayerName(player.getPlayerName());
					existing.setPlayerEmail(player.getPlayerEmail());
					existing.setPlayerPassword(passwordEncoder.encode(player.getPlayerPassword()));
					return playerRepository.save(existing);
				});
	}

	/**
	 * Partially update fields on an existing player profile.
	 *
	 * @param playerId identifier of the player to patch
	 * @param player patch payload containing updated fields
	 * @return optional patched player model
	 */
	public Optional<PlayerModel> patchPlayer(Integer playerId, PlayerModel player) {
		if (player.getPlayerId() != null && !playerId.equals(player.getPlayerId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Invalid request: path id and payload id do not match.");
		}

		validatePlayerName(player.getPlayerName(), true);
		validatePlayerEmail(player.getPlayerEmail(), true);
		validatePlayerPassword(player.getPlayerPassword(), true);

		return playerRepository.findById(playerId)
				.map(existing -> {
					if (player.getPlayerName() != null) {
						existing.setPlayerName(player.getPlayerName());
					}
					if (player.getPlayerEmail() != null) {
						existing.setPlayerEmail(player.getPlayerEmail());
					}
					if (player.getPlayerPassword() != null) {
						existing.setPlayerPassword(passwordEncoder.encode(player.getPlayerPassword()));
					}
					return playerRepository.save(existing);
				});
	}

	/**
	 * Validate the provided player name.
	 *
	 * @param playerName name to validate
	 * @param allowNull whether null is permitted for patch operations
	 */
	private void validatePlayerName(String playerName, boolean allowNull) {
		if (playerName == null) {
			if (allowNull) {
				return;
			}
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter your name.");
		}

		if (playerName.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name cannot be empty.");
		}

		if (playerName.length() > PLAYER_NAME_MAX_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Name must be at most " + PLAYER_NAME_MAX_LENGTH + " characters.");
		}
	}

	/**
	 * Validate the provided player email address.
	 *
	 * @param playerEmail email to validate
	 * @param allowNull whether null is permitted for patch operations
	 */
	private void validatePlayerEmail(String playerEmail, boolean allowNull) {
		if (playerEmail == null) {
			if (allowNull) {
				return;
			}
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter your email address.");
		}

		if (playerEmail.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email address cannot be empty.");
		}

		if (playerEmail.length() < PLAYER_EMAIL_MIN_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Email must be at least " + PLAYER_EMAIL_MIN_LENGTH + " characters.");
		}

		if (playerEmail.length() > PLAYER_EMAIL_MAX_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Email must be at most " + PLAYER_EMAIL_MAX_LENGTH + " characters.");
		}

		if (!PLAYER_EMAIL_REGEX.matcher(playerEmail).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Please enter a valid email address (example: name@example.com).");
		}
	}

	/**
	 * Validate the provided player password.
	 *
	 * @param playerPassword password to validate
	 * @param allowNull whether null is permitted for patch operations
	 */
	private void validatePlayerPassword(String playerPassword, boolean allowNull) {
		if (playerPassword == null) {
			if (allowNull) {
				return;
			}
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter your password.");
		}

		if (playerPassword.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty.");
		}

		if (playerPassword.length() < PLAYER_PASSWORD_MIN_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Password must be at least " + PLAYER_PASSWORD_MIN_LENGTH + " characters.");
		}

		if (playerPassword.length() > PLAYER_PASSWORD_MAX_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Password must be at most " + PLAYER_PASSWORD_MAX_LENGTH + " characters.");
		}
	}

	/**
	 * Delete a player by their identifier.
	 *
	 * @param playerId identifier of the player to delete
	 */
	public void deletePlayer(Integer playerId) {
		playerRepository.deleteById(playerId);
	}
}
