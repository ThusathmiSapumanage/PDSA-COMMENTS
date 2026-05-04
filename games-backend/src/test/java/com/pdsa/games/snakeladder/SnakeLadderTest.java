package com.pdsa.games.snakeladder;

import com.pdsa.games.snakeladder.model.AlgorithmResult;
import com.pdsa.games.snakeladder.model.SnakeLadderItem;
import com.pdsa.games.snakeladder.model.SnakeLadderItem.PathType;
import com.pdsa.games.snakeladder.service.BFSSnakeLadderService;
import com.pdsa.games.snakeladder.service.DPSnakeLadderService;
import com.pdsa.games.snakeladder.service.SnakeLadderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Snake and Ladder game backend.
 *
 * These tests focus only on logic that does not require a real database.
 */
class SnakeLadderTest {

    private BFSSnakeLadderService bfsService;
    private DPSnakeLadderService dpService;
    private SnakeLadderService snakeLadderService;

    private static final int TOTAL_CELLS_6x6 = 36;
    private static final Map<Integer, Integer> NO_PORTALS = Collections.emptyMap();

    @BeforeEach
    void setUp() {
        bfsService = new BFSSnakeLadderService();
        dpService = new DPSnakeLadderService();

        // Repository is not needed for the methods tested here
        snakeLadderService = new SnakeLadderService(null, bfsService, dpService);
    }

    private List<SnakeLadderItem> dummyItems(int n) {
        return snakeLadderService.generateBoard(n, 1);
    }

    // =========================================================================
    // 1. BFS Algorithm Tests
    // =========================================================================

    @Test
    void bfs_noSnakesOrLadders_returnsExactAnswer() {
        AlgorithmResult result = bfsService.solve(TOTAL_CELLS_6x6, NO_PORTALS);

        assertTrue(result.isSuccess(), "BFS should find a path");
        assertEquals("Snake and Ladder - BFS", result.getAlgorithmName());
        assertEquals(6, result.getMinimumThrows(),
                "On a 36-cell board with no portals, minimum throws should be exactly 6");
    }

    @Test
    void bfs_ladderNearStart_reducesThrows() {
        Map<Integer, Integer> portalMap = new HashMap<>();
        portalMap.put(2, 32);

        AlgorithmResult withLadder = bfsService.solve(TOTAL_CELLS_6x6, portalMap);
        AlgorithmResult withoutLadder = bfsService.solve(TOTAL_CELLS_6x6, NO_PORTALS);

        assertTrue(withLadder.getMinimumThrows() <= withoutLadder.getMinimumThrows(),
                "A big ladder should not increase minimum throws");
    }

    @Test
    void bfs_snakeNearEnd_increasesOrSameThrows() {
        Map<Integer, Integer> portalMap = new HashMap<>();
        portalMap.put(35, 5);

        AlgorithmResult withSnake = bfsService.solve(TOTAL_CELLS_6x6, portalMap);
        AlgorithmResult withoutSnake = bfsService.solve(TOTAL_CELLS_6x6, NO_PORTALS);

        assertTrue(withSnake.getMinimumThrows() >= withoutSnake.getMinimumThrows(),
                "A snake near the end should not decrease minimum throws");
    }

    @Test
    void bfs_recordsExecutionTime() {
        AlgorithmResult result = bfsService.solve(TOTAL_CELLS_6x6, NO_PORTALS);
        assertTrue(result.getExecutionTimeMs() >= 0, "Execution time must be non-negative");
    }

    @Test
    void bfs_algorithmNameIsBFS() {
        AlgorithmResult result = bfsService.solve(TOTAL_CELLS_6x6, NO_PORTALS);
        assertEquals("Snake and Ladder - BFS", result.getAlgorithmName());
    }

    // =========================================================================
    // 2. DP Algorithm Tests
    // =========================================================================

    @Test
    void dp_noSnakesOrLadders_returnsExactAnswer() {
        AlgorithmResult result = dpService.solve(TOTAL_CELLS_6x6, NO_PORTALS);

        assertTrue(result.isSuccess(), "DP should find a path");
        assertEquals("Snake and Ladder - Dynamic Programming", result.getAlgorithmName());
        assertEquals(6, result.getMinimumThrows(),
                "On a 36-cell board with no portals, minimum throws should be exactly 6");
    }

    @Test
    void dp_ladderNearStart_reducesThrows() {
        Map<Integer, Integer> portalMap = new HashMap<>();
        portalMap.put(2, 32);

        AlgorithmResult withLadder = dpService.solve(TOTAL_CELLS_6x6, portalMap);
        AlgorithmResult withoutLadder = dpService.solve(TOTAL_CELLS_6x6, NO_PORTALS);

        assertTrue(withLadder.getMinimumThrows() <= withoutLadder.getMinimumThrows(),
                "A big ladder should not increase minimum throws");
    }

    @Test
    void dp_snakeNearEnd_increasesOrSameThrows() {
        Map<Integer, Integer> portalMap = new HashMap<>();
        portalMap.put(35, 5);

        AlgorithmResult withSnake = dpService.solve(TOTAL_CELLS_6x6, portalMap);
        AlgorithmResult withoutSnake = dpService.solve(TOTAL_CELLS_6x6, NO_PORTALS);

        assertTrue(withSnake.getMinimumThrows() >= withoutSnake.getMinimumThrows(),
                "A snake near the end should not decrease minimum throws");
    }

    @Test
    void dp_recordsExecutionTime() {
        AlgorithmResult result = dpService.solve(TOTAL_CELLS_6x6, NO_PORTALS);
        assertTrue(result.getExecutionTimeMs() >= 0);
    }

    @Test
    void dp_algorithmNameIsDynamicProgramming() {
        AlgorithmResult result = dpService.solve(TOTAL_CELLS_6x6, NO_PORTALS);
        assertEquals("Snake and Ladder - Dynamic Programming", result.getAlgorithmName());
    }

    // =========================================================================
    // 3. SnakeLadderService Tests
    // =========================================================================

    @Test
    void validateN_validValues_noException() {
        for (int n = 6; n <= 12; n++) {
            final int testN = n;
            assertDoesNotThrow(() -> snakeLadderService.validateN(testN));
        }
    }

    @Test
    void validateN_tooSmall_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> snakeLadderService.validateN(5));
    }

    @Test
    void validateN_tooLarge_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> snakeLadderService.validateN(13));
    }

    @Test
    void generateBoard_correctNumberOfSnakesAndLadders() {
        int n = 8;
        List<SnakeLadderItem> items = snakeLadderService.generateBoard(n, 1);

        long snakeCount = items.stream().filter(i -> i.getPathType() == PathType.SNAKE).count();
        long ladderCount = items.stream().filter(i -> i.getPathType() == PathType.LADDER).count();

        assertEquals(n - 2, snakeCount, "Should have N-2 snakes");
        assertEquals(n - 2, ladderCount, "Should have N-2 ladders");
    }

    @Test
    void generateBoard_laddersGoUpward() {
        List<SnakeLadderItem> items = snakeLadderService.generateBoard(6, 1);

        for (SnakeLadderItem item : items) {
            if (item.getPathType() == PathType.LADDER) {
                assertTrue(item.getStartCell() < item.getEndCell(),
                        "Ladder start must be less than end: " + item);
            }
        }
    }

    @Test
    void generateBoard_snakesGoDownward() {
        List<SnakeLadderItem> items = snakeLadderService.generateBoard(6, 1);

        for (SnakeLadderItem item : items) {
            if (item.getPathType() == PathType.SNAKE) {
                assertTrue(item.getStartCell() > item.getEndCell(),
                        "Snake start must be greater than end: " + item);
            }
        }
    }

    @Test
    void generateBoard_noDuplicateStartCells() {
        List<SnakeLadderItem> items = snakeLadderService.generateBoard(10, 1);
        Set<Integer> starts = new HashSet<>();

        for (SnakeLadderItem item : items) {
            assertTrue(starts.add(item.getStartCell()),
                    "Duplicate start cell found: " + item.getStartCell());
        }
    }

    @Test
    void generateBoard_doesNotUseFirstOrLastCell() {
        int n = 8;
        int totalCells = n * n;
        List<SnakeLadderItem> items = snakeLadderService.generateBoard(n, 1);

        for (SnakeLadderItem item : items) {
            assertNotEquals(1, item.getStartCell(), "Start cell 1 must not be used");
            assertNotEquals(1, item.getEndCell(), "End cell 1 must not be used");
            assertNotEquals(totalCells, item.getStartCell(), "Last cell must not be used");
            assertNotEquals(totalCells, item.getEndCell(), "Last cell must not be used");
        }
    }

    @Test
    void generateChoices_alwaysContainsCorrectAnswer() {
        for (int correct = 1; correct <= 20; correct++) {
            List<Integer> choices = snakeLadderService.generateChoices(correct, dummyItems(8), 8);
            assertTrue(choices.contains(correct),
                    "Choices must include the correct answer: " + correct);
        }
    }

    @Test
    void generateChoices_alwaysReturnsThreeOptions() {
        List<Integer> choices = snakeLadderService.generateChoices(7, dummyItems(8), 8);
        assertEquals(3, choices.size(), "There must always be exactly 3 choices");
    }

    @Test
    void generateChoices_allChoicesArePositive() {
        List<Integer> choices = snakeLadderService.generateChoices(1, dummyItems(8), 8);

        for (int choice : choices) {
            assertTrue(choice > 0, "All choices must be positive: " + choice);
        }
    }

    @Test
    void generateChoices_allChoicesAreDistinct() {
        List<Integer> choices = snakeLadderService.generateChoices(5, dummyItems(8), 8);
        assertEquals(3, new HashSet<>(choices).size(), "All choices must be distinct");
    }

    @Test
    void generateChoices_alwaysIncludesDrawOption() {
        int correctAnswer = 5;
        List<Integer> choices = snakeLadderService.generateChoices(correctAnswer, dummyItems(8), 8);

        assertTrue(
                choices.contains(correctAnswer - 1) || choices.contains(correctAnswer + 1),
                "Choices must include a draw option off by one from the correct answer.");
    }

    @Test
    void determineOutcome_exactMatch_returnsWin() {
        assertEquals("WIN", snakeLadderService.determineOutcome(5, 5));
    }

    @Test
    void determineOutcome_offByOneAbove_returnsDraw() {
        assertEquals("DRAW", snakeLadderService.determineOutcome(6, 5));
    }

    @Test
    void determineOutcome_offByOneBelow_returnsDraw() {
        assertEquals("DRAW", snakeLadderService.determineOutcome(4, 5));
    }

    @Test
    void determineOutcome_offByTwo_returnsLose() {
        assertEquals("LOSE", snakeLadderService.determineOutcome(3, 5));
    }

    @Test
    void determineOutcome_offByMoreThanTwo_returnsLose() {
        assertEquals("LOSE", snakeLadderService.determineOutcome(9, 5));
    }

    @Test
    void runAlgorithms_returnsTwoResults() {
        List<SnakeLadderItem> items = snakeLadderService.generateBoard(6, 1);
        List<AlgorithmResult> results = snakeLadderService.runAlgorithms(items, 36);

        assertEquals(2, results.size(), "Should return exactly 2 algorithm results");
        assertEquals("Snake and Ladder - BFS", results.get(0).getAlgorithmName());
        assertEquals("Snake and Ladder - Dynamic Programming", results.get(1).getAlgorithmName());
    }

    // =========================================================================
    // 4. BFS and DP Must Agree
    // =========================================================================

    @Test
    void bfsAndDP_agreeOnSameBoard_noPortals() {
        AlgorithmResult bfs = bfsService.solve(TOTAL_CELLS_6x6, NO_PORTALS);
        AlgorithmResult dp = dpService.solve(TOTAL_CELLS_6x6, NO_PORTALS);

        assertEquals(bfs.getMinimumThrows(), dp.getMinimumThrows(),
                "BFS and DP must produce the same answer on the same board");
    }

    @Test
    void bfsAndDP_agreeOnSameBoard_withPortals() {
        Map<Integer, Integer> portalMap = new HashMap<>();
        portalMap.put(4, 14);
        portalMap.put(9, 3);
        portalMap.put(20, 30);
        portalMap.put(33, 12);

        AlgorithmResult bfs = bfsService.solve(TOTAL_CELLS_6x6, portalMap);
        AlgorithmResult dp = dpService.solve(TOTAL_CELLS_6x6, portalMap);

        assertEquals(bfs.getMinimumThrows(), dp.getMinimumThrows(),
                "BFS and DP must agree: BFS=" + bfs.getMinimumThrows() + " DP=" + dp.getMinimumThrows());
    }

    @Test
    void bfsAndDP_agreeOnLargerBoard_12x12() {
        int total = 144;
        Map<Integer, Integer> portalMap = new HashMap<>();
        portalMap.put(5, 50);
        portalMap.put(22, 100);
        portalMap.put(110, 30);
        portalMap.put(130, 60);

        AlgorithmResult bfs = bfsService.solve(total, portalMap);
        AlgorithmResult dp = dpService.solve(total, portalMap);

        assertEquals(bfs.getMinimumThrows(), dp.getMinimumThrows(),
                "Algorithms must agree on a 12×12 board");
    }
}