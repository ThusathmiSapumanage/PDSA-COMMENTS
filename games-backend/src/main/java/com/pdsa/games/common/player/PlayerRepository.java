package com.pdsa.games.common.player;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerModel, Integer> {
	/**
	 * Find a player by email address.
	 *
	 * @param playerEmail email address to search for
	 * @return optional player model matching the email
	 */
	@Query(value = "SELECT * FROM Player p WHERE p.Player_Email = :playerEmail", nativeQuery = true)
	Optional<PlayerModel> findByPlayerEmail(@Param("playerEmail") String playerEmail);
}
