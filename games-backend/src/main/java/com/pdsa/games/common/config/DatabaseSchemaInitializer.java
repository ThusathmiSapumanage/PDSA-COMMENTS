package com.pdsa.games.common.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSchemaInitializer.class);

        // Keep this aligned with every CREATE TABLE statement in schema.sql.
        private static final List<String> REQUIRED_TABLES = List.of(
            "Player",
            "Game",
            "Algorithm",
            "Game_Session",
            "Response",
            "Minimum_Cost_Game",
            "Snake_And_Ladder_Game",
            "Traffic_Sim_Game",
            "Knight_Tour_Game",
            "Sixteen_Queens_Game",
            "Queens_Solution_Count",
            "Snake_And_Ladder_Item",
            "Roads",
            "Knight_Move",
            "Solution",
            "Queen_Location",
            "Algorithm_Execute",
            "Knight_Tour_Response_Move"
        );

    private static final List<GameSeed> MASTER_DATA = List.of(
            new GameSeed("Knight Tour", List.of("Knight Tour - Warnsdorff", "Knight Tour - Backtracking")),
            new GameSeed("Snake and Ladder", List.of("Snake and Ladder - BFS", "Snake and Ladder - Dynamic Programming")),
            new GameSeed("Minimum Cost", List.of("Minimum Cost - Greedy", "Minimum Cost - Hungarian")),
            new GameSeed("Sixteen Queens Puzzle", List.of("Sixteen Queens - Sequential", "Sixteen Queens - Threaded")),
            new GameSeed("Traffic Simulation", List.of("Traffic - Dinic", "Traffic - Ford-Fulkerson"))
    );

    // Demo player account for testing
    private static final String DEMO_PLAYER_EMAIL = "demo@example.com";
    private static final String DEMO_PLAYER_NAME = "Demo User";
    private static final String DEMO_PLAYER_PASSWORD = "123456";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.schema.auto-init:true}")
    private boolean autoInitEnabled;

    public DatabaseSchemaInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!autoInitEnabled) {
            logger.info("Schema auto-initialization is disabled (app.schema.auto-init=false).");
            return;
        }

        try {
            if (schemaLooksReady()) {
                logger.info("Database schema check passed. Required tables already exist.");
            } else {
                logger.warn("Database schema appears missing/incomplete. Running schema.sql...");
                runSchemaScript();
                logger.info("schema.sql executed successfully.");
            }

            seedMasterData();
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Database schema initialization failed. Check DB connectivity/permissions and schema.sql.",
                    ex
            );
        }
    }

    private boolean schemaLooksReady() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            for (String table : REQUIRED_TABLES) {
                if (!tableExists(connection, table)) {
                    logger.warn("Required table not found: {}", table);
                    return false;
                }
            }
            return true;
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();

        if (existsWithPattern(metaData, catalog, tableName)) {
            return true;
        }

        if (existsWithPattern(metaData, catalog, tableName.toUpperCase())) {
            return true;
        }

        return existsWithPattern(metaData, catalog, tableName.toLowerCase());
    }

    private boolean existsWithPattern(DatabaseMetaData metaData, String catalog, String tableNamePattern)
            throws SQLException {
        try (ResultSet rs = metaData.getTables(catalog, null, tableNamePattern, new String[] {"TABLE"})) {
            return rs.next();
        }
    }

    private void runSchemaScript() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema.sql"));
        populator.setContinueOnError(false);
        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    private void seedMasterData() {
        for (GameSeed gameSeed : MASTER_DATA) {
            Integer gameId = ensureGame(gameSeed.gameName());
            for (String algorithmName : gameSeed.algorithmNames()) {
                ensureAlgorithm(gameId, algorithmName);
            }
        }

        seedDemoPlayer();

        logger.info("Master data seed check completed for {} games.", MASTER_DATA.size());
    }

    private void seedDemoPlayer() {
        Integer existingPlayerId = jdbcTemplate.query(
                "SELECT Player_Id FROM Player WHERE Player_Email = ?",
                rs -> rs.next() ? rs.getInt("Player_Id") : null,
                DEMO_PLAYER_EMAIL
        );

        if (existingPlayerId != null) {
            logger.info("Demo player already exists with email: {}", DEMO_PLAYER_EMAIL);
            return;
        }

        String encodedPassword = passwordEncoder.encode(DEMO_PLAYER_PASSWORD);
        jdbcTemplate.update(
                "INSERT INTO Player (Player_Name, Player_Email, Player_Password) VALUES (?, ?, ?)",
                DEMO_PLAYER_NAME,
                DEMO_PLAYER_EMAIL,
                encodedPassword
        );

        logger.info("Demo player created: {} with password: {}", DEMO_PLAYER_EMAIL, DEMO_PLAYER_PASSWORD);
    }

    private Integer ensureGame(String gameName) {
        Integer gameId = jdbcTemplate.query(
                "SELECT Game_Id FROM Game WHERE Game_Name = ?",
                rs -> rs.next() ? rs.getInt("Game_Id") : null,
                gameName
        );

        if (gameId != null) {
            return gameId;
        }

        jdbcTemplate.update("INSERT INTO Game (Game_Name) VALUES (?)", gameName);

        Integer createdGameId = jdbcTemplate.query(
                "SELECT Game_Id FROM Game WHERE Game_Name = ?",
                rs -> rs.next() ? rs.getInt("Game_Id") : null,
                gameName
        );

        if (createdGameId == null) {
            throw new IllegalStateException("Failed to create Game '" + gameName + "'.");
        }

        logger.info("Ensured game row exists: {}", gameName);
        return createdGameId;
    }

    private void ensureAlgorithm(Integer gameId, String algorithmName) {
        Integer existingGameId = jdbcTemplate.query(
                "SELECT Game_Id FROM Algorithm WHERE Algorithm_Name = ?",
                rs -> rs.next() ? rs.getInt("Game_Id") : null,
                algorithmName
        );

        if (existingGameId != null) {
            if (!existingGameId.equals(gameId)) {
                throw new IllegalStateException(
                        "Algorithm '" + algorithmName + "' already exists for a different game (Game_Id="
                                + existingGameId + ")."
                );
            }
            return;
        }

        jdbcTemplate.update(
                "INSERT INTO Algorithm (Game_Id, Algorithm_Name) VALUES (?, ?)",
                gameId,
                algorithmName
        );

        logger.info("Ensured algorithm row exists: {}", algorithmName);
    }

    private record GameSeed(String gameName, List<String> algorithmNames) {
    }
}
