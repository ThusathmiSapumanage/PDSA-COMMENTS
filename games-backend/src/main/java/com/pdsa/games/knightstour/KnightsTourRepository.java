package com.pdsa.games.knightstour;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class KnightsTourRepository {

    private final JdbcTemplate jdbcTemplate;

    public KnightsTourRepository(JdbcTemplate jdbcTemplate) {
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

    public Long createGameSession(Long playerId) {
        Integer gameId = findGameIdByName("Knight Tour");

        if (gameId == null) {
            throw new IllegalStateException("Game 'Knight Tour' not found in Game table.");
        }

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

    public Integer findGameIdByName(String gameName) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT Game_Id FROM Game WHERE Game_Name = ?",
                    Integer.class,
                    gameName
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public Integer findAlgorithmId(String algorithmName) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT Algorithm_Id FROM Algorithm WHERE Algorithm_Name = ?",
                    Integer.class,
                    algorithmName
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
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

    public boolean sessionExists(Long sessionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Game_Session WHERE Session_Id = ?",
                Integer.class,
                sessionId
        );
        return count != null && count > 0;
    }

    public void saveKnightTourGame(Long sessionId, int boardSize, int startX, int startY, int moveCount) {
        jdbcTemplate.update(
                "INSERT INTO Knight_Tour_Game (Session_Id, Board_Size, X_Start, Y_Start, Move_Count) VALUES (?, ?, ?, ?, ?)",
                sessionId, boardSize, startX, startY, moveCount
        );
    }

    public KnightsTourModel.GameConfig findGameConfig(Long sessionId) {
        RowMapper<KnightsTourModel.GameConfig> rowMapper = (rs, rowNum) -> {
            KnightsTourModel.GameConfig config = new KnightsTourModel.GameConfig();
            config.setSessionId(rs.getLong("Session_Id"));
            config.setBoardSize(rs.getInt("Board_Size"));
            config.setStartX(rs.getInt("X_Start"));
            config.setStartY(rs.getInt("Y_Start"));
            config.setMoveCount(rs.getInt("Move_Count"));
            return config;
        };

        List<KnightsTourModel.GameConfig> results = jdbcTemplate.query(
                "SELECT Session_Id, Board_Size, X_Start, Y_Start, Move_Count " +
                        "FROM Knight_Tour_Game WHERE Session_Id = ?",
                rowMapper,
                sessionId
        );

        return results.isEmpty() ? null : results.get(0);
    }

    public void saveKnightMoves(Long sessionId, int algorithmId, List<KnightsTourModel.Move> moves) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO Knight_Move (Session_Id, Algorithm_Id, Step_No, X_Value, Y_Value) VALUES (?, ?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
                        KnightsTourModel.Move move = moves.get(i);
                        ps.setLong(1, sessionId);
                        ps.setInt(2, algorithmId);
                        ps.setInt(3, move.getStepNo());
                        ps.setInt(4, move.getX());
                        ps.setInt(5, move.getY());
                    }

                    @Override
                    public int getBatchSize() {
                        return moves.size();
                    }
                }
        );
    }

    public void saveAlgorithmExecution(Long sessionId, int algorithmId, double executionTimeMs, String outputResult) {
        jdbcTemplate.update(
                "INSERT INTO Algorithm_Execute (Session_Id, Algorithm_Id, Execution_Time_MS, Output_Result) VALUES (?, ?, ?, ?)",
                sessionId, algorithmId, executionTimeMs, outputResult
        );
    }

    public List<KnightsTourModel.Move> findCorrectMoves(Long sessionId, int algorithmId) {
        RowMapper<KnightsTourModel.Move> rowMapper = (rs, rowNum) ->
                new KnightsTourModel.Move(
                        rs.getInt("Step_No"),
                        rs.getInt("X_Value"),
                        rs.getInt("Y_Value")
                );

        return jdbcTemplate.query(
                "SELECT Step_No, X_Value, Y_Value " +
                        "FROM Knight_Move " +
                        "WHERE Session_Id = ? AND Algorithm_Id = ? " +
                        "ORDER BY Step_No",
                rowMapper,
                sessionId,
                algorithmId
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

    public void saveResponseMoves(Long responseId, List<KnightsTourModel.Move> moves) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO Knight_Tour_Response_Move (Response_Id, Step_No, X_Value, Y_Value) VALUES (?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
                        KnightsTourModel.Move move = moves.get(i);
                        ps.setLong(1, responseId);
                        ps.setInt(2, move.getStepNo());
                        ps.setInt(3, move.getX());
                        ps.setInt(4, move.getY());
                    }

                    @Override
                    public int getBatchSize() {
                        return moves.size();
                    }
                }
        );
    }
}