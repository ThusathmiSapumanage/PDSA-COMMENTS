package com.pdsa.games.mincost;

import com.pdsa.games.mincost.service.AlgorithmSolveResult;
import com.pdsa.games.mincost.service.GreedyAssignmentService;
import com.pdsa.games.mincost.service.HungarianAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MinCostServiceTest {

    private GreedyAssignmentService greedyService;
    private HungarianAssignmentService hungarianService;
    private MinCostRepository repository;
    private MinCostService minCostService;

    @BeforeEach
    void setUp() {
        greedyService = mock(GreedyAssignmentService.class);
        hungarianService = mock(HungarianAssignmentService.class);
        repository = mock(MinCostRepository.class);

        minCostService = new MinCostService(greedyService, hungarianService, repository);

        when(repository.findGameId()).thenReturn(1);
        when(repository.ensureAlgorithm(1, "Minimum Cost - Greedy")).thenReturn(101);
        when(repository.ensureAlgorithm(1, "Minimum Cost - Hungarian")).thenReturn(102);
        when(repository.createSession(1, 1L)).thenReturn(1001L);
    }

    @Test
    void startGame_shouldRejectNBelow50() {
        MinCostModel.StartRequest request = new MinCostModel.StartRequest();
        request.setN(49);
        request.setMinCost(20);
        request.setMaxCost(200);
        request.setPlayerId(1L);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> minCostService.startGame(request));

        assertEquals("Matrix size N must be between 50 and 100.", ex.getMessage());
    }

    @Test
    void startGame_shouldRejectNAbove100() {
        MinCostModel.StartRequest request = new MinCostModel.StartRequest();
        request.setN(101);
        request.setMinCost(20);
        request.setMaxCost(200);
        request.setPlayerId(1L);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> minCostService.startGame(request));

        assertEquals("Matrix size N must be between 50 and 100.", ex.getMessage());
    }

    @Test
    void startGame_shouldRejectCostBelow20() {
        MinCostModel.StartRequest request = new MinCostModel.StartRequest();
        request.setN(50);
        request.setMinCost(10);
        request.setMaxCost(200);
        request.setPlayerId(1L);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> minCostService.startGame(request));

        assertEquals(
                "Cost range must be between 20 and 200, and Min Cost must be less than Max Cost.",
                ex.getMessage()
        );
    }

    @Test
    void startGame_shouldRejectCostAbove200() {
        MinCostModel.StartRequest request = new MinCostModel.StartRequest();
        request.setN(50);
        request.setMinCost(20);
        request.setMaxCost(250);
        request.setPlayerId(1L);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> minCostService.startGame(request));

        assertEquals(
                "Cost range must be between 20 and 200, and Min Cost must be less than Max Cost.",
                ex.getMessage()
        );
    }

    @Test
    void startGame_shouldRejectWhenMinCostIsNotLessThanMaxCost() {
        MinCostModel.StartRequest request = new MinCostModel.StartRequest();
        request.setN(50);
        request.setMinCost(100);
        request.setMaxCost(100);
        request.setPlayerId(1L);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> minCostService.startGame(request));

        assertEquals(
                "Cost range must be between 20 and 200, and Min Cost must be less than Max Cost.",
                ex.getMessage()
        );
    }

    @Test
    void startGame_shouldReturnResponseAndSaveExecutionRecords() {
        MinCostModel.StartRequest request = new MinCostModel.StartRequest();
        request.setN(50);
        request.setMinCost(20);
        request.setMaxCost(200);
        request.setPlayerId(1L);

        AlgorithmSolveResult greedyResult = new AlgorithmSolveResult(
                500,
                List.of(new MinCostModel.Assignment(0, 0, 50)),
                List.of("greedy log"),
                List.of()
        );

        AlgorithmSolveResult hungarianResult = new AlgorithmSolveResult(
                450,
                List.of(new MinCostModel.Assignment(0, 0, 50)),
                List.of("hungarian log"),
                List.of()
        );

        when(greedyService.solve(ArgumentMatchers.any(int[][].class))).thenReturn(greedyResult);
        when(hungarianService.solve(ArgumentMatchers.any(int[][].class))).thenReturn(hungarianResult);

        MinCostModel.GameResponse response = minCostService.startGame(request);

        assertNotNull(response);
        assertNotNull(response.getMatrix());
        assertEquals(50, response.getMatrix().length);
        assertEquals(50, response.getMatrix()[0].length);

        assertEquals(500, response.getGreedyTotalCost());
        assertEquals(450, response.getHungarianTotalCost());

        assertEquals(1, response.getGreedyAssignments().size());
        assertEquals(1, response.getHungarianAssignments().size());

        assertEquals("greedy log", response.getGreedyLogs().get(0));
        assertEquals("hungarian log", response.getHungarianLogs().get(0));

        assertTrue(response.getGreedyTimeMs() >= 0);
        assertTrue(response.getHungarianTimeMs() >= 0);

        verify(repository).registerGame();
        verify(repository).findGameId();
        verify(repository).ensureAlgorithm(1, "Minimum Cost - Greedy");
        verify(repository).ensureAlgorithm(1, "Minimum Cost - Hungarian");
        verify(repository).createSession(1, 1L);
        verify(repository).saveMinCostGame(1001L, 50, 450);
        verify(repository).saveExecution(eq(1001L), eq(101), anyDouble());
        verify(repository).saveExecution(eq(1001L), eq(102), anyDouble());
    }

    @Test
    void startGame_shouldUseDefaultCostRangeWhenMinAndMaxAreNull() {
        MinCostModel.StartRequest request = new MinCostModel.StartRequest();
        request.setN(50);
        request.setPlayerId(1L);

        AlgorithmSolveResult greedyResult = new AlgorithmSolveResult(
                300,
                List.of(new MinCostModel.Assignment(0, 0, 30)),
                List.of("greedy"),
                List.of()
        );

        AlgorithmSolveResult hungarianResult = new AlgorithmSolveResult(
                250,
                List.of(new MinCostModel.Assignment(0, 0, 25)),
                List.of("hungarian"),
                List.of()
        );

        when(greedyService.solve(ArgumentMatchers.any(int[][].class))).thenReturn(greedyResult);
        when(hungarianService.solve(ArgumentMatchers.any(int[][].class))).thenReturn(hungarianResult);

        MinCostModel.GameResponse response = minCostService.startGame(request);

        assertNotNull(response);
        assertEquals(50, response.getMatrix().length);

        for (int[] row : response.getMatrix()) {
            for (int value : row) {
                assertTrue(value >= 20 && value <= 200);
            }
        }
    }
}