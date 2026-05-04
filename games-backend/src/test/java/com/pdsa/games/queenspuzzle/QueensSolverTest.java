package com.pdsa.games.queenspuzzle;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueensSolverTest {

    private final QueensSolver solver = new QueensSolver();

    // ================= SEQUENTIAL =================

    @Test
    void solveSequential_shouldFindTwoSolutionsFor4x4() {
        List<int[]> solutions = solver.solveSequential(4, 4);
        assertEquals(2, solutions.size());
    }

    @Test
    void solveSequentialCount_shouldMatchSequentialListFor4x4() {
        int count = solver.solveSequentialCount(4, 4);
        assertEquals(solver.solveSequential(4, 4).size(), count);
    }

    @Test
    void solveSequentialCount_shouldFindTenSolutionsFor5x5() {
        assertEquals(10, solver.solveSequentialCount(5, 5));
    }

    @Test
    void solveSequentialCount_shouldFindFourSolutionsFor6x6() {
        assertEquals(4, solver.solveSequentialCount(6, 6));
    }

    @Test
    void solveSequentialCount_shouldFindFortySolutionsFor7x7() {
        assertEquals(40, solver.solveSequentialCount(7, 7));
    }

    @Test
    void solveSequentialCount_shouldHandlePartialPlacement() {
        int count = solver.solveSequentialCount(4, 2);
        assertTrue(count > 0, "placing 2 queens on 4x4 should yield at least one solution");
    }

    // ================= THREADED =================

    @Test
    void solveThreadedCount_shouldMatchSequentialFor4x4() {
        assertEquals(solver.solveSequentialCount(4, 4), solver.solveThreadedCount(4, 4));
    }

    @Test
    void solveThreadedCount_shouldMatchSequentialFor5x5() {
        assertEquals(solver.solveSequentialCount(5, 5), solver.solveThreadedCount(5, 5));
    }

    @Test
    void solveThreadedCount_shouldMatchSequentialFor6x6() {
        assertEquals(solver.solveSequentialCount(6, 6), solver.solveThreadedCount(6, 6));
    }

    @Test
    void solveThreadedCount_shouldMatchSequentialFor8x8() {
        assertEquals(solver.solveSequentialCount(8, 8), solver.solveThreadedCount(8, 8));
    }

    // ================= STRUCTURE =================

    @Test
    void solveSequential_shouldReturnNonNullList() {
        List<int[]> solutions = solver.solveSequential(4, 4);
        assertNotNull(solutions);
        for (int[] s : solutions) {
            assertEquals(4, s.length);
        }
    }

    // ================= TIMED =================

    @Test
    void solveSequentialTimed_shouldFindAllSolutionsForSmallBoardWithinBudget() {
        QueensSolver.TimedResult result = solver.solveSequentialTimed(5, 5, 5_000L, 100);
        assertEquals(10, result.totalFound);
        assertEquals(10, result.sample.size());
        assertTrue(!result.hitTimeLimit, "5x5 should finish well under a 5s budget");
    }

    @Test
    void solveSequentialTimed_shouldRespectMaxStored() {
        QueensSolver.TimedResult result = solver.solveSequentialTimed(7, 7, 5_000L, 5);
        assertEquals(40, result.totalFound);
        assertTrue(result.sample.size() <= 5);
    }

    @Test
    void solveThreadedTimed_shouldMatchSequentialCountForSmallBoard() {
        QueensSolver.TimedResult seq = solver.solveSequentialTimed(7, 7, 5_000L, 100);
        QueensSolver.TimedResult thr = solver.solveThreadedTimed(7, 7, 5_000L, 100);
        assertEquals(seq.totalFound, thr.totalFound);
    }

    @Test
    void solveSequentialTimed_shouldBailWhenBudgetExceeded() {
        // 8 queens on 16x16 has billions of solutions — guaranteed to hit a 200 ms budget.
        QueensSolver.TimedResult result = solver.solveSequentialTimed(16, 8, 200L, 50);
        assertTrue(result.hitTimeLimit, "expected the deadline to be reached on 8q-on-16x16 in 200 ms");
        assertTrue(result.elapsedMs <= 1_500L,
                "should bail close to the budget; was " + result.elapsedMs + " ms");
    }
}
