package com.pdsa.games.common.algorithmExecute;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlgorithmExecuteRepository extends JpaRepository<AlgorithmExecuteModel, AlgorithmExecuteId> {

	@Query(value = "SELECT * FROM Algorithm_Execute WHERE Session_Id = :sessionId", nativeQuery = true)
	List<AlgorithmExecuteModel> findAllBySessionId(@Param("sessionId") Integer sessionId);

	@Query(value = "SELECT ae.* "
			+ "FROM Algorithm_Execute ae "
			+ "JOIN Algorithm a ON ae.Algorithm_Id = a.Algorithm_Id "
			+ "WHERE a.Game_Id = :gameId", nativeQuery = true)
	List<AlgorithmExecuteModel> findAllByGameId(@Param("gameId") Integer gameId);

	@Query(value = "SELECT * FROM Algorithm_Execute WHERE Output_Result = :outputResult", nativeQuery = true)
	List<AlgorithmExecuteModel> findAllByOutputResult(@Param("outputResult") String outputResult);

}
