package com.pdsa.games.common.algorithmExecute;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlgorithmExecuteRepository extends JpaRepository<AlgorithmExecuteModel, AlgorithmExecuteId> {

	@Query(value = "SELECT * FROM Algorithm_Execute WHERE Session_Id = :sessionId", nativeQuery = true)
	/**
	 * Retrieve execution records for a specific session.
	 *
	 * @param sessionId session identifier
	 * @return list of execution records for the session
	 */
	List<AlgorithmExecuteModel> findAllBySessionId(@Param("sessionId") Integer sessionId);

	@Query(value = "SELECT ae.* "
			+ "FROM Algorithm_Execute ae "
			+ "JOIN Algorithm a ON ae.Algorithm_Id = a.Algorithm_Id "
			+ "WHERE a.Game_Id = :gameId", nativeQuery = true)
	/**
	 * Retrieve execution records for a specific game.
	 *
	 * @param gameId game identifier
	 * @return list of execution records for the game
	 */
	List<AlgorithmExecuteModel> findAllByGameId(@Param("gameId") Integer gameId);

	@Query(value = "SELECT * FROM Algorithm_Execute WHERE Output_Result = :outputResult", nativeQuery = true)
	/**
	 * Retrieve execution records that match the specified output result.
	 *
	 * @param outputResult output result string to filter by
	 * @return list of matching execution records
	 */
	List<AlgorithmExecuteModel> findAllByOutputResult(@Param("outputResult") String outputResult);

}
