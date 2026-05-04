package com.pdsa.games.mincost;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * MIN COST REPOSITORY:
 * This layer handles all Database interactions.
 * We follow the Knight's Tour pattern by separating SQL from the Business logic.
 */
@Repository
public class MinCostRepository {

    private final JdbcTemplate jdbcTemplate;

    public MinCostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds the 'Minimum Cost' game ID from the Game master table.
     */
    public Integer findGameId() {
        return jdbcTemplate.query(
                "SELECT Game_Id FROM Game WHERE Game_Name = ?",
                rs -> rs.next() ? rs.getInt("Game_Id") : null,
                "Minimum Cost"
        );
    }

    /**
     * Registers 'Minimum Cost' in the master table if it doesn't exist.
     */
    public void registerGame() {
        jdbcTemplate.update("INSERT IGNORE INTO Game (Game_Name) VALUES (?)", "Minimum Cost");
    }

    /**
     * Creates a new player entry.
     */
    public Long createPlayer(String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO Player (Player_Name, Player_Email, Player_Password) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, name);
            ps.setString(2, name.toLowerCase() + "@system.local"); // Dummy email for schema requirements
            ps.setString(3, "password123"); // Dummy password
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /**
     * Starts a new game session.
     */
    public Long createSession(int gameId, Long playerId) {
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
        return keyHolder.getKey().longValue();
    }

    /**
     * Saves the specific MinCost game details.
     */
    public void saveMinCostGame(Long sessionId, int n, int cost) {
        jdbcTemplate.update(
                "INSERT INTO Minimum_Cost_Game (Session_Id, N_Value, Minimum_Total_Cost) VALUES (?, ?, ?)",
                sessionId, n, cost
        );
    }

    /**
     * Ensures an algorithm exists and returns its ID.
     */
    public Integer ensureAlgorithm(int gameId, String algoName) {
        Integer id = jdbcTemplate.query(
                "SELECT Algorithm_Id FROM Algorithm WHERE Algorithm_Name = ?",
                rs -> rs.next() ? rs.getInt("Algorithm_Id") : null,
                algoName
        );
        if (id != null) return id;

        jdbcTemplate.update("INSERT INTO Algorithm (Game_Id, Algorithm_Name) VALUES (?, ?)", gameId, algoName);
        return jdbcTemplate.queryForObject(
                "SELECT Algorithm_Id FROM Algorithm WHERE Algorithm_Name = ?",
                Integer.class,
                algoName
        );
    }

    /**
     * Logs the execution time for an algorithm.
     */
    public void saveExecution(Long sessionId, int algorithmId, double timeMs) {
        jdbcTemplate.update(
                "INSERT INTO Algorithm_Execute (Session_Id, Algorithm_Id, Execution_Time_MS, Output_Result) VALUES (?, ?, ?, 'SUCCESS')",
                sessionId, algorithmId, timeMs
        );
    }

    /**
     * Fetches the last 10 game runs for the history charts.
     */
    public List<MinCostModel.HistoryItem> getHistory() {
        String sql = "SELECT gs.Created_At, mcg.N_Value, mcg.Minimum_Total_Cost, " +
                "ae_g.Execution_Time_MS as Greedy_Time, ae_h.Execution_Time_MS as Hungarian_Time " +
                "FROM Game_Session gs " +
                "JOIN Minimum_Cost_Game mcg ON gs.Session_Id = mcg.Session_Id " +
                "LEFT JOIN Algorithm_Execute ae_g ON gs.Session_Id = ae_g.Session_Id AND ae_g.Algorithm_Id = " +
                "(SELECT Algorithm_Id FROM Algorithm WHERE Algorithm_Name = 'Minimum Cost - Greedy') " +
                "LEFT JOIN Algorithm_Execute ae_h ON gs.Session_Id = ae_h.Session_Id AND ae_h.Algorithm_Id = " +
                "(SELECT Algorithm_Id FROM Algorithm WHERE Algorithm_Name = 'Minimum Cost - Hungarian') " +
                "ORDER BY gs.Created_At DESC LIMIT 10";

        RowMapper<MinCostModel.HistoryItem> rowMapper = (rs, rowNum) -> new MinCostModel.HistoryItem(
                rs.getString("Created_At"),
                rs.getInt("N_Value"),
                rs.getInt("Minimum_Total_Cost"),
                rs.getDouble("Greedy_Time"),
                rs.getDouble("Hungarian_Time")
        );

        return jdbcTemplate.query(sql, rowMapper);
    }
}
