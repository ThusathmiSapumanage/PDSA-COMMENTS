package com.pdsa.games.common.gameSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSessionModel, Integer> {
    @Query(value = "SELECT * FROM Game_Session WHERE Game_Id = :gameId", nativeQuery = true)
    List<GameSessionModel> findAllByGameId(@Param("gameId") Integer gameId);

    @Query(value = "SELECT * FROM Game_Session WHERE Player_Id = :playerId", nativeQuery = true)
    List<GameSessionModel> findAllByPlayerId(@Param("playerId") Integer playerId);

    @Query(value = "SELECT * FROM Game_Session WHERE Game_Id = :gameId AND Player_Id = :playerId", nativeQuery = true)
    List<GameSessionModel> findAllByGameIdAndPlayerId(@Param("gameId") Integer gameId, @Param("playerId") Integer playerId);
}
