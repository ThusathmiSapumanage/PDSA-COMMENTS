package com.pdsa.games.common.algorithm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlgorithmRepository extends JpaRepository<AlgorithmModel, Integer> {

    @Query(value = "SELECT * FROM Algorithm WHERE Game_Id = :gameId", nativeQuery = true)
    /**
     * Retrieve all algorithms associated with a specific game identifier.
     *
     * @param gameId identifier of the game to filter algorithms by
     * @return list of algorithms for the requested game
     */
    List<AlgorithmModel> findAllByGameId(@Param("gameId") Integer gameId);

    /**
     * Find an algorithm by its unique name.
     *
     * @param algorithmName unique algorithm name to search for
     * @return optional algorithm matching the name
     */
    Optional<AlgorithmModel> findByAlgorithmName(String algorithmName);
}
