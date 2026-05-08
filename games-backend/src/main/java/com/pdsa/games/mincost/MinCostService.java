package com.pdsa.games.mincost;

import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdsa.games.mincost.service.AlgorithmSolveResult;
import com.pdsa.games.mincost.service.GreedyAssignmentService;
import com.pdsa.games.mincost.service.HungarianAssignmentService;

/**
 * MIN COST SERVICE:
 * Coordinates validation, matrix generation, algorithm execution, and persistence.
 */
@Service
public class MinCostService {

    private static final Logger logger = LoggerFactory.getLogger(MinCostService.class);
    private final GreedyAssignmentService greedyService;
    private final HungarianAssignmentService hungarianService;
    private final MinCostRepository repository;
    private final Random random = new Random();

    public MinCostService(GreedyAssignmentService greedyService,
                          HungarianAssignmentService hungarianService,
                          MinCostRepository repository) {
        this.greedyService = greedyService;
        this.hungarianService = hungarianService;
        this.repository = repository;
    }

    /**
     * Starts a new Min Cost game run by validating input, generating a random
     * matrix, executing both assignment algorithms, recording the results, and
     * building the API response payload.
     *
     * @param request the request payload containing matrix size and cost bounds
     * @return the full game response including assignments, logs, costs, and timings
     */
    @Transactional
    public MinCostModel.GameResponse startGame(MinCostModel.StartRequest request) {
        int n = pickN(request.getN());
        int minCost = (request.getMinCost() != null) ? request.getMinCost() : 20;
        int maxCost = (request.getMaxCost() != null) ? request.getMaxCost() : 200;

        if (n < 50 || n > 100) {
            throw new IllegalArgumentException("Matrix size N must be between 50 and 100.");
        }

        if (minCost < 20 || maxCost > 200 || minCost >= maxCost) {
            throw new IllegalArgumentException(
                    "Cost range must be between 20 and 200, and Min Cost must be less than Max Cost."
            );
        }

        int[][] matrix = generateMatrix(n, minCost, maxCost);

        long greedyStart = System.nanoTime();
        AlgorithmSolveResult greedyResult = greedyService.solve(matrix);
        long greedyEnd = System.nanoTime();

        long hungarianStart = System.nanoTime();
        AlgorithmSolveResult hungarianResult = hungarianService.solve(matrix);
        long hungarianEnd = System.nanoTime();

        double greedyTimeMs = nanosToMs(greedyEnd - greedyStart);
        double hungarianTimeMs = nanosToMs(hungarianEnd - hungarianStart);

        saveGameRun(request, n, greedyResult, hungarianResult, greedyTimeMs, hungarianTimeMs);

        return new MinCostModel.GameResponse(
                matrix,
                greedyResult.assignments(),
                hungarianResult.assignments(),
                greedyResult.logs(),
                hungarianResult.logs(),
                greedyResult.totalCost(),
                hungarianResult.totalCost(),
                greedyTimeMs,
                hungarianTimeMs,
                hungarianResult.hungarianSteps()
        );
    }

    /**
     * Retrieves the last 10 min-cost game history items from the database.
     *
     * @return list of history items for chart display
     */
    public List<MinCostModel.HistoryItem> getHistory() {
        return repository.getHistory();
    }

    /**
     * Persists the game run metadata and execution times for both algorithms.
     * The method is defensive against persistence failures so the game result
     * can still be returned even if DB logging fails.
     */
    private void saveGameRun(MinCostModel.StartRequest request,
                             int n,
                             AlgorithmSolveResult greedy,
                             AlgorithmSolveResult hungarian,
                             double greedyTimeMs,
                             double hungarianTimeMs) {
        try {
            repository.registerGame();
            Integer gameId = repository.findGameId();

            Integer greedyId = repository.ensureAlgorithm(gameId, "Minimum Cost - Greedy");
            Integer hungarianId = repository.ensureAlgorithm(gameId, "Minimum Cost - Hungarian");

            Long playerId = (request.getPlayerId() != null) ? request.getPlayerId() : 1L;
            Long sessionId = repository.createSession(gameId, playerId);

            repository.saveMinCostGame(sessionId, n, hungarian.totalCost());

            repository.saveExecution(sessionId, greedyId, greedyTimeMs);
            repository.saveExecution(sessionId, hungarianId, hungarianTimeMs);
        } catch (Exception e) {
            logger.warn("DB Persistence Warning: Could not save game stats! {}", e.getMessage());
        }
    }

    /**
     * Builds a random N x N cost matrix inside the requested cost range.
     *
     * @param n the size of the square matrix
     * @param min the minimum allowed cell cost
     * @param max the maximum allowed cell cost
     * @return a randomly generated cost matrix for the game
     */
    private int[][] generateMatrix(int n, int min, int max) {
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = min + random.nextInt(max - min + 1);
            }
        }
        return matrix;
    }

    /**
     * Determines the matrix size to use, returning the requested size or a
     * randomly chosen value when none is provided.
     *
     * @param requestedN optional requested matrix dimension
     * @return the chosen matrix size
     */
    private int pickN(Integer requestedN) {
        if (requestedN != null) {
            return requestedN;
        }
        return 50 + random.nextInt(51);
    }

    /**
     * Converts nanosecond timing values into milliseconds with 3 decimal places.
     *
     * @param nanos a duration in nanoseconds
     * @return the duration in milliseconds
     */
    private double nanosToMs(long nanos) {
        return Math.round((nanos / 1_000_000.0) * 1000.0) / 1000.0;
    }
}