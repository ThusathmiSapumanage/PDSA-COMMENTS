package com.pdsa.games.queenspuzzle;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class QueensRepository {

	private final JdbcTemplate jdbcTemplate;

	public QueensRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Long findPlayerIdByEmail(String playerEmail) {
		try {
			return jdbcTemplate.queryForObject(
					"SELECT Player_Id FROM Player WHERE Player_Email = ?",
					Long.class,
					playerEmail
			);
		} catch (EmptyResultDataAccessException ex) {
			return null;
		}
	}

	public boolean playerExists(Long playerId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM Player WHERE Player_Id = ?",
				Integer.class,
				playerId
		);
		return count != null && count > 0;
	}

	public boolean sessionBelongsToPlayer(Long sessionId, Long playerId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM Game_Session WHERE Session_Id = ? AND Player_Id = ?",
				Integer.class,
				sessionId,
				playerId
		);
		return count != null && count > 0;
	}

	public Integer ensureGameId(String gameName) {
		Integer id = jdbcTemplate.query(
				"SELECT Game_Id FROM Game WHERE Game_Name = ?",
				rs -> rs.next() ? rs.getInt("Game_Id") : null,
				gameName
		);
		if (id != null) {
			return id;
		}
		jdbcTemplate.update("INSERT INTO Game (Game_Name) VALUES (?)", gameName);
		return jdbcTemplate.queryForObject(
				"SELECT Game_Id FROM Game WHERE Game_Name = ?",
				Integer.class,
				gameName
		);
	}

	public Integer ensureAlgorithmId(Integer gameId, String algorithmName) {
		Integer id = jdbcTemplate.query(
				"SELECT Algorithm_Id FROM Algorithm WHERE Algorithm_Name = ?",
				rs -> rs.next() ? rs.getInt("Algorithm_Id") : null,
				algorithmName
		);
		if (id != null) {
			return id;
		}
		jdbcTemplate.update("INSERT INTO Algorithm (Game_Id, Algorithm_Name) VALUES (?, ?)", gameId, algorithmName);
		return jdbcTemplate.queryForObject(
				"SELECT Algorithm_Id FROM Algorithm WHERE Algorithm_Name = ?",
				Integer.class,
				algorithmName
		);
	}

	public Long createGameSession(Integer gameId, Long playerId) {
		KeyHolder keyHolder = new GeneratedKeyHolder();

		jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(
					"INSERT INTO Game_Session (Game_Id, Player_Id) VALUES (?, ?)",
					Statement.RETURN_GENERATED_KEYS
			);
			ps.setInt(1, gameId);
			ps.setLong(2, playerId);
			return ps;
		}, keyHolder);

		Number key = keyHolder.getKey();
		if (key == null) {
			throw new IllegalStateException("Failed to create Game_Session.");
		}
		return key.longValue();
	}

	public void saveSixteenQueensGame(Long sessionId, int totalSolutions) {
		jdbcTemplate.update(
				"INSERT INTO Sixteen_Queens_Game (Session_Id, Total_Solutions, Solutions_Discovered) VALUES (?, ?, 0)",
				sessionId, totalSolutions
		);
	}

	public void saveAlgorithmExecution(Long sessionId, int algorithmId, double executionTimeMs, String outputResult) {
		jdbcTemplate.update(
				"INSERT INTO Algorithm_Execute (Session_Id, Algorithm_Id, Execution_Time_MS, Output_Result) VALUES (?, ?, ?, ?)",
				sessionId, algorithmId, executionTimeMs, outputResult
		);
	}

	public void saveSolutions(Long sessionId, List<int[]> solutions) {
		if (solutions.isEmpty()) {
			return;
		}

		List<Integer> solutionIds = new ArrayList<>(solutions.size());
		for (int i = 0; i < solutions.size(); i++) {
			solutionIds.add(i + 1);
		}

		jdbcTemplate.batchUpdate(
				"INSERT INTO Solution (Session_Id, Solution_Id, Is_Discovered) VALUES (?, ?, false)",
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
						ps.setLong(1, sessionId);
						ps.setInt(2, solutionIds.get(i));
					}

					@Override
					public int getBatchSize() {
						return solutionIds.size();
					}
				}
		);

		List<QueenLocationRow> locations = new ArrayList<>();
		for (int i = 0; i < solutions.size(); i++) {
			int solutionId = i + 1;
			int[] rowToCol = solutions.get(i);
			for (int row = 0; row < rowToCol.length; row++) {
				int col = rowToCol[row];
				if (col >= 0) {
					locations.add(new QueenLocationRow(solutionId, col + 1, row + 1));
				}
			}
		}

		jdbcTemplate.batchUpdate(
				"INSERT INTO Queen_Location (Session_Id, Solution_Id, X_Value, Y_Value) VALUES (?, ?, ?, ?)",
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
						QueenLocationRow row = locations.get(i);
						ps.setLong(1, sessionId);
						ps.setInt(2, row.solutionId());
						ps.setInt(3, row.x());
						ps.setInt(4, row.y());
					}

					@Override
					public int getBatchSize() {
						return locations.size();
					}
				}
		);
	}

	public int insertDiscoveredSolution(Long sessionId, List<QueensModel.Position> positions) {
		Integer maxId = jdbcTemplate.queryForObject(
				"SELECT COALESCE(MAX(Solution_Id), 0) FROM Solution WHERE Session_Id = ?",
				Integer.class,
				sessionId
		);
		int newSolutionId = (maxId == null ? 0 : maxId) + 1;

		jdbcTemplate.update(
				"INSERT INTO Solution (Session_Id, Solution_Id, Is_Discovered) VALUES (?, ?, true)",
				sessionId, newSolutionId
		);

		jdbcTemplate.batchUpdate(
				"INSERT INTO Queen_Location (Session_Id, Solution_Id, X_Value, Y_Value) VALUES (?, ?, ?, ?)",
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
						QueensModel.Position p = positions.get(i);
						ps.setLong(1, sessionId);
						ps.setInt(2, newSolutionId);
						ps.setInt(3, p.getX());
						ps.setInt(4, p.getY());
					}

					@Override
					public int getBatchSize() {
						return positions.size();
					}
				}
		);

		jdbcTemplate.update(
				"UPDATE Sixteen_Queens_Game SET Solutions_Discovered = Solutions_Discovered + 1 WHERE Session_Id = ?",
				sessionId
		);

		return newSolutionId;
	}

	public Integer findMatchingSolutionId(Long sessionId, List<QueensModel.Position> positions) {
		if (positions.isEmpty()) {
			return null;
		}

		StringBuilder sql = new StringBuilder(
				"SELECT Solution_Id FROM Queen_Location WHERE Session_Id = ? AND (X_Value, Y_Value) IN ("
		);
		List<Object> params = new ArrayList<>();
		params.add(sessionId);

		for (int i = 0; i < positions.size(); i++) {
			if (i > 0) {
				sql.append(", ");
			}
			sql.append("(?, ?)");
			QueensModel.Position position = positions.get(i);
			params.add(position.getX());
			params.add(position.getY());
		}

		sql.append(") GROUP BY Solution_Id HAVING COUNT(*) = ?");
		params.add(positions.size());

		return jdbcTemplate.query(sql.toString(),
				rs -> rs.next() ? rs.getInt("Solution_Id") : null,
				params.toArray());
	}

	public boolean isSolutionDiscovered(Long sessionId, int solutionId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM Solution WHERE Session_Id = ? AND Solution_Id = ? AND Is_Discovered = true",
				Integer.class,
				sessionId,
				solutionId
		);
		return count != null && count > 0;
	}

	public void markSolutionDiscovered(Long sessionId, int solutionId) {
		jdbcTemplate.update(
				"UPDATE Solution SET Is_Discovered = true WHERE Session_Id = ? AND Solution_Id = ?",
				sessionId, solutionId
		);
		jdbcTemplate.update(
				"UPDATE Sixteen_Queens_Game SET Solutions_Discovered = Solutions_Discovered + 1 WHERE Session_Id = ?",
				sessionId
		);
	}

	public void resetDiscovered(Long sessionId) {
		jdbcTemplate.update(
				"UPDATE Solution SET Is_Discovered = false WHERE Session_Id = ?",
				sessionId
		);
		jdbcTemplate.update(
				"UPDATE Sixteen_Queens_Game SET Solutions_Discovered = 0 WHERE Session_Id = ?",
				sessionId
		);
	}

	public void updateTotalSolutions(Long sessionId, int totalSolutions) {
		jdbcTemplate.update(
				"UPDATE Sixteen_Queens_Game SET Total_Solutions = ? WHERE Session_Id = ?",
				totalSolutions, sessionId
		);
	}

	public Integer getTotalSolutions(Long sessionId) {
		return jdbcTemplate.queryForObject(
				"SELECT Total_Solutions FROM Sixteen_Queens_Game WHERE Session_Id = ?",
				Integer.class,
				sessionId
		);
	}

	public Integer getDiscoveredCount(Long sessionId) {
		return jdbcTemplate.queryForObject(
				"SELECT Solutions_Discovered FROM Sixteen_Queens_Game WHERE Session_Id = ?",
				Integer.class,
				sessionId
		);
	}

	public Long saveResponse(Long sessionId, String responseSummary, boolean isCorrect) {
		KeyHolder keyHolder = new GeneratedKeyHolder();

		jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(
					"INSERT INTO Response (Session_Id, Response, Is_Correct) VALUES (?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS
			);
			ps.setLong(1, sessionId);
			ps.setString(2, responseSummary);
			ps.setBoolean(3, isCorrect);
			return ps;
		}, keyHolder);

		Number key = keyHolder.getKey();
		if (key == null) {
			throw new IllegalStateException("Failed to save Response.");
		}

		return key.longValue();
	}

	public String findPlayerName(Long playerId) {
		return jdbcTemplate.queryForObject(
				"SELECT Player_Name FROM Player WHERE Player_Id = ?",
				String.class,
				playerId
		);
	}

	public CachedCount findCachedCount(int boardSize, int queenCount) {
		try {
			return jdbcTemplate.queryForObject(
					"SELECT Total_Solutions, Sequential_Time_Ms, Threaded_Time_Ms " +
							"FROM Queens_Solution_Count WHERE Board_Size = ? AND Queen_Count = ?",
					(rs, rowNum) -> new CachedCount(
							rs.getInt("Total_Solutions"),
							rs.getDouble("Sequential_Time_Ms"),
							rs.getDouble("Threaded_Time_Ms")
					),
					boardSize,
					queenCount
			);
		} catch (EmptyResultDataAccessException ex) {
			return null;
		}
	}

	public void insertCachedCount(int boardSize, int queenCount, int totalSolutions,
								   double sequentialMs, double threadedMs) {
		jdbcTemplate.update(
				"INSERT INTO Queens_Solution_Count " +
						"(Board_Size, Queen_Count, Total_Solutions, Sequential_Time_Ms, Threaded_Time_Ms) " +
						"VALUES (?, ?, ?, ?, ?) " +
						"ON DUPLICATE KEY UPDATE " +
						"Total_Solutions = VALUES(Total_Solutions), " +
						"Sequential_Time_Ms = VALUES(Sequential_Time_Ms), " +
						"Threaded_Time_Ms = VALUES(Threaded_Time_Ms)",
				boardSize, queenCount, totalSolutions, sequentialMs, threadedMs
		);
	}

	public record CachedCount(int totalSolutions, double sequentialMs, double threadedMs) {
	}

	private record QueenLocationRow(int solutionId, int x, int y) {
	}
}
