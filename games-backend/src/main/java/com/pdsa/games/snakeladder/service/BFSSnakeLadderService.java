package com.pdsa.games.snakeladder.service;

import com.pdsa.games.snakeladder.model.AlgorithmResult;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Solves the Snake and Ladder problem using Breadth-First Search (BFS).
 *
 * WHY BFS?
 * - Think of each cell (1 to N²) as a node in a graph.
 * - A dice roll connects one cell to up to 6 others (current+1, current+2, ..., current+6).
 * - Snakes/ladders teleport you to another cell immediately.
 * - BFS explores all cells reachable in 1 throw, then 2 throws, etc.
 * - The FIRST time BFS reaches the last cell is guaranteed to be the minimum throws.
 *
 * Time Complexity:  O(N²)  — each cell is visited at most once
 * Space Complexity: O(N²)  — the visited array and queue
 */
@Service
public class BFSSnakeLadderService {

    /**
     * Find the minimum number of dice throws using BFS.
     *
     * @param totalCells total cells on the board (= N * N)
     * @param portalMap  maps start → end for every snake and ladder
     *                   (built once by SnakeLadderService from the item list)
     * @return an AlgorithmResult with the answer and execution time
     */
    public AlgorithmResult solve(int totalCells, Map<Integer, Integer> portalMap) {

        long startTime = System.nanoTime();

        // visited[i] = true means we already found the shortest path to cell i+1
        boolean[] visited = new boolean[totalCells];

        // Each entry in the queue stores: [cell number, number of throws to reach it]
        Queue<int[]> queue = new LinkedList<>();

        // We start at cell 1 (index 0), 0 throws used
        queue.add(new int[]{1, 0});
        visited[0] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int cell   = current[0];
            int numThrows = current[1];

            // Try all 6 dice outcomes from the current cell
            for (int dice = 1; dice <= 6; dice++) {
                int next = cell + dice;

                // Can't go beyond the last cell
                if (next > totalCells) break;

                // If there's a snake or ladder at 'next', follow it immediately
                if (portalMap.containsKey(next)) {
                    next = portalMap.get(next);
                }

                // Did we reach the last cell?
                if (next == totalCells) {
                    long endTime = System.nanoTime();
                    double ms = (endTime - startTime) / 1_000_000.0;
                    return new AlgorithmResult("Snake and Ladder - BFS", numThrows + 1, ms, true);
                }

                // Add unvisited cell to the queue
                if (!visited[next - 1]) {
                    visited[next - 1] = true;
                    queue.add(new int[]{next, numThrows + 1});
                }
            }
        }

        // No path found (shouldn't happen on a valid board)
        long endTime = System.nanoTime();
        double ms = (endTime - startTime) / 1_000_000.0;
        return new AlgorithmResult("Snake and Ladder - BFS", -1, ms, false);
    }
}