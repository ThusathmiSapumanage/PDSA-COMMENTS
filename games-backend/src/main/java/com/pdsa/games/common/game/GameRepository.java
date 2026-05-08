package com.pdsa.games.common.game;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<GameModel, Integer> {
	/**
	 * Find a game by its unique name.
	 *
	 * @param gameName name of the game to search for
	 * @return optional game model matching the name
	 */
	Optional<GameModel> findByGameName(String gameName);
}
