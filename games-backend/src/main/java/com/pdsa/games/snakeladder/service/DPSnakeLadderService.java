package com.pdsa.games.snakeladder.service;

import com.pdsa.games.snakeladder.model.AlgorithmResult;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

/**
 * Solves the Snake and Ladder problem using Dynamic Programming (DP).
 *
 * WHY DYNAMIC PROGRAMMING?
 * - Define dp[i] = minimum number of throws needed to reach cell i.
 * - Base case: dp[1] = 0 (we start here, no throws needed).
 * - For every cell i that we can reach, simulate all 6 dice outcomes.
 * - If landing on cell j (after snake/ladder), update dp[j] if dp[i]+1 is
 * better.
 * - We process cells in order 1 → N², so each cell's best answer is already
 * known
 * when we use it to update further cells.
 *
 * Time Complexity: O(N²) — one pass over all cells, 6 transitions each
 * Space Complexity: O(N²) — the dp array
 *
 * NOTE: Both BFS and DP give the same correct answer.
 * BFS is naturally suited because the graph is unweighted (each throw costs 1).
 * DP is an alternative formulation that demonstrates a different algorithmic
 * mindset.
 */
@Service
public class DPSnakeLadderService {

    /**
     * Find the minimum number of dice throws using Dynamic Programming.
     *
     * @param totalCells total cells on the board (= N * N)
     * @param portalMap  maps start → end for every snake and ladder
     * @return an AlgorithmResult with the answer and execution time
     */
    public AlgorithmResult solve(int totalCells, Map<Integer, Integer> portalMap) {

        long startTime = System.nanoTime();

        // dp[i] = minimum throws to reach cell (i+1)
        // Initialise everything to "infinity" (unreachable)
        int[] dp = new int[totalCells];
        Arrays.fill(dp, Integer.MAX_VALUE);

        // We start at cell 1 (index 0) — takes 0 throws
        dp[0] = 0;

        // Process cells 1 to N²-1 (we don't need to move FROM the last cell)
        for (int cell = 1; cell < totalCells; cell++) {

            // Only process cells we can actually reach
            if (dp[cell - 1] == Integer.MAX_VALUE)
                continue;

            // Try all 6 dice outcomes from this cell
            for (int dice = 1; dice <= 6; dice++) {
                int next = cell + dice; // cell is 1-indexed, so cell+dice is the target cell number

                if (next > totalCells)
                    break;

                // Follow snake or ladder if present
                if (portalMap.containsKey(next)) {
                    next = portalMap.get(next);
                }

                // Update dp if we found a shorter path to 'next'
                int newThrows = dp[cell - 1] + 1;
                if (newThrows < dp[next - 1]) {
                    dp[next - 1] = newThrows;
                }
            }
        }

        long endTime = System.nanoTime();
        double ms = (endTime - startTime) / 1_000_000.0;

        int answer = dp[totalCells - 1];
        boolean found = (answer != Integer.MAX_VALUE);

        return new AlgorithmResult("Snake and Ladder - Dynamic Programming", found ? answer : -1, ms, found);
    }
}