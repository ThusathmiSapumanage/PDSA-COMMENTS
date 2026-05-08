package com.pdsa.games.snakeladder.repository;

import com.pdsa.games.snakeladder.model.SnakeLadderGame;
import com.pdsa.games.snakeladder.model.SnakeLadderItem;
import com.pdsa.games.snakeladder.model.SnakeLadderItem.PathType;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * Handles all database operations for the Snake and Ladder game.
 *
 * Tables used:
 * - Snake_And_Ladder_Game (one row per session: N value, minimum throws)
 * - Snake_And_Ladder_Item (all snakes and ladders for a session)
 * - Algorithm_Execute (execution time + result for each algorithm)
 * - Response (player's answer for the session)
 */
@Repository
public class SnakeLadderRepository {

    private static final String GAME_NAME = "Snake and Ladder";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructs the repository with the configured JdbcTemplate.
     *
     * @param jdbcTemplate Spring JDBC template used for database access
     */
    public SnakeLadderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // -------------------------------------------------------------------------
    // Snake_And_Ladder_Game
    // -------------------------------------------------------------------------

    /**
     * Save the game summary (N value + correct minimum throws) for a session.
     * Deletes any existing game for this session first to avoid duplicate key
     * errors.
     */
    public void saveGame(SnakeLadderGame game) {
        // Clean up any duplicate old session data first
        cleanupOldSessionData(game.getSessionId());

        jdbcTemplate.update(
                "INSERT INTO Snake_And_Ladder_Game (Session_Id, N_Value, Minimum_Throws) VALUES (?, ?, ?)",
                game.getSessionId(),
                game.getNValue(),
                game.getMinimumThrows());
    }

    /**
     * Clean up all old data for a session to avoid duplicates.
     *
     * @param sessionId session id to delete data for
     */
    private void cleanupOldSessionData(int sessionId) {
        jdbcTemplate.update("DELETE FROM Snake_And_Ladder_Game WHERE Session_Id = ?", sessionId);
        jdbcTemplate.update("DELETE FROM Snake_And_Ladder_Item WHERE Session_Id = ?", sessionId);
        jdbcTemplate.update("DELETE FROM Algorithm_Execute WHERE Session_Id = ?", sessionId);
        jdbcTemplate.update("DELETE FROM Response WHERE Session_Id = ?", sessionId);
    }

    /**
     * Load a saved game summary by session ID.
     */
    public SnakeLadderGame findGameBySessionId(int sessionId) {
        RowMapper<SnakeLadderGame> rowMapper = (rs, rowNum) -> new SnakeLadderGame(
                rs.getInt("Session_Id"),
                rs.getInt("N_Value"),
                rs.getInt("Minimum_Throws"));

        List<SnakeLadderGame> results = jdbcTemplate.query(
                "SELECT Session_Id, N_Value, Minimum_Throws FROM Snake_And_Ladder_Game WHERE Session_Id = ?",
                rowMapper,
                sessionId);

        return results.isEmpty() ? null : results.get(0);
    }

    // -------------------------------------------------------------------------
    // Snake_And_Ladder_Item
    // -------------------------------------------------------------------------

    /**
     * Save all snakes and ladders for a session (called once after board
     * generation).
     */
    public void saveItems(List<SnakeLadderItem> items) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO Snake_And_Ladder_Item (Session_Id, Start_Cell, End_Cell, Path_Type) VALUES (?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
                        SnakeLadderItem item = items.get(i);
                        ps.setInt(1, item.getSessionId());
                        ps.setInt(2, item.getStartCell());
                        ps.setInt(3, item.getEndCell());
                        ps.setString(4, item.getPathType().name());
                    }

                    @Override
                    public int getBatchSize() {
                        return items.size();
                    }
                });
    }

    /**
     * Load all snakes and ladders for a given session.
     */
    public List<SnakeLadderItem> findItemsBySessionId(int sessionId) {
        RowMapper<SnakeLadderItem> rowMapper = (rs, rowNum) -> new SnakeLadderItem(
                rs.getInt("Session_Id"),
                rs.getInt("Start_Cell"),
                rs.getInt("End_Cell"),
                PathType.valueOf(rs.getString("Path_Type")));

        return jdbcTemplate.query(
                "SELECT Session_Id, Start_Cell, End_Cell, Path_Type FROM Snake_And_Ladder_Item WHERE Session_Id = ?",
                rowMapper,
                sessionId);
    }

    // -------------------------------------------------------------------------
    // Algorithm_Execute
    // -------------------------------------------------------------------------

    /**
     * Save the execution time and outcome for one algorithm run.
     *
     * @param sessionId       the current game session
     * @param algorithmId     ID from the Algorithm table (e.g. BFS=1, DP=2)
     * @param executionTimeMs time taken in milliseconds
     * @param success         true if algorithm found an answer
     */
    public void saveAlgorithmExecution(int sessionId, int algorithmId,
            double executionTimeMs, boolean success) {
        String outputResult = success ? "SUCCESS" : "FAILURE";
        jdbcTemplate.update(
                "INSERT INTO Algorithm_Execute (Session_Id, Algorithm_Id, Execution_Time_MS, Output_Result) VALUES (?, ?, ?, ?)",
                sessionId,
                algorithmId,
                executionTimeMs,
                outputResult);
    }

    // -------------------------------------------------------------------------
    // Response
    // -------------------------------------------------------------------------

    /**
     * Save the player's answer attempt.
     *
     * @param sessionId current session
     * @param response  the answer the player chose (e.g. "5")
     * @param isCorrect whether it matched the correct minimum throws
     * @return the auto-generated Response_Id
     */
    public int saveResponse(int sessionId, String response, boolean isCorrect) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO Response (Session_Id, Response, Is_Correct) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, sessionId);
            ps.setString(2, response);
            ps.setBoolean(3, isCorrect);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to save response — no generated key returned.");
        }

        return key.intValue();
    }

    // -------------------------------------------------------------------------
    // Game_Session creation and player lookup
    // -------------------------------------------------------------------------

    /**
     * Returns whether the given player exists in the Player table.
     *
     * @param playerId player id
     * @return true when the player exists
     */
    public boolean playerExists(int playerId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Player WHERE Player_Id = ?",
                Integer.class,
                playerId);
        return count != null && count > 0;
    }

    /**
     * Creates a new Game_Session record for the given player.
     *
     * @param playerId player id
     * @return generated session id
     */
    public int createGameSession(int playerId) {
        int gameId = ensureGameId();

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO Game_Session (Game_Id, Player_Id) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, gameId);
            ps.setInt(2, playerId);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create Game_Session.");
        }

        return key.intValue();
    }

    /**
     * Ensures the Snake and Ladder game exists in the Game table and returns its id.
     *
     * @return game id for Snake and Ladder
     */
    private int ensureGameId() {
        Integer gameId = jdbcTemplate.query(
                "SELECT Game_Id FROM Game WHERE Game_Name = ?",
                rs -> rs.next() ? rs.getInt("Game_Id") : null,
                GAME_NAME);

        if (gameId != null) {
            return gameId;
        }

        jdbcTemplate.update("INSERT INTO Game (Game_Name) VALUES (?)", GAME_NAME);

        Integer createdGameId = jdbcTemplate.queryForObject(
                "SELECT Game_Id FROM Game WHERE Game_Name = ?",
                Integer.class,
                GAME_NAME);

        if (createdGameId == null) {
            throw new IllegalStateException("Failed to create Game '" + GAME_NAME + "'.");
        }

        return createdGameId;
    }

    // -------------------------------------------------------------------------
    // Algorithm lookup helper
    // -------------------------------------------------------------------------

    /**
     * Look up an algorithm's ID by its name.
     * Useful for dynamically finding IDs instead of hardcoding them.
     *
     * @param algorithmName e.g. "BFS Snake Ladder" or "DP Snake Ladder"
     * @return the Algorithm_Id, or -1 if not found
     */
    public int findAlgorithmId(String algorithmName) {
        try {
            Integer result = jdbcTemplate.queryForObject(
                    "SELECT Algorithm_Id FROM Algorithm WHERE Algorithm_Name = ?",
                    Integer.class,
                    algorithmName);
            return result != null ? result : -1;
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return -1; // algorithm not seeded in DB yet
        }
    }
}
