package com.pdsa.games.mincost.service;

import com.pdsa.games.mincost.MinCostModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * GREEDY STRATEGY: 
 * This is the "fast but selfish" way to solve the problem.
 * We just look for the smallest number we can find right now,
 * and we pick it without thinking about the future steps.
 */
@Service
public class GreedyAssignmentService {

    /**
     * Solve the matrix using greedy method.
     *
     * @param matrix the N x N cost grid to assign
     * @return the greedy result containing assignments and logs
     */
    public AlgorithmSolveResult solve(int[][] matrix) {
        // Defensive check: if matrix is missing, we can't do anything!
        if (matrix == null || matrix.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be empty or null");
        }

        int n = matrix.length;
        // Keep track of which rows and cols we already used
        boolean[] usedRows = new boolean[n];
        boolean[] usedCols = new boolean[n];
        
        List<MinCostModel.Assignment> assignments = new ArrayList<>();
        List<String> logs = new ArrayList<>();
        int total = 0;

        // We need to make N assignments in total
        for (int step = 0; step < n; step++) {
            int bestValue = Integer.MAX_VALUE;
            int bestRow = -1;
            int bestCol = -1;

            // Look through every cell in the grid
            for (int row = 0; row < n; row++) {
                if (usedRows[row]) {
                    continue; // Skip if row already has a task
                }
                for (int col = 0; col < n; col++) {
                    if (usedCols[col]) {
                        continue; // Skip if worker already has a task
                    }
                    
                    // If we find a smaller cost, remember it!
                    if (matrix[row][col] < bestValue) {
                        bestValue = matrix[row][col];
                        bestRow = row;
                        bestCol = col;
                    }
                }
            }

            // Mark this row/col as "taken" so we don't pick them again
            usedRows[bestRow] = true;
            usedCols[bestCol] = true;
            
            // Save the result for this step
            assignments.add(new MinCostModel.Assignment(bestRow, bestCol, bestValue));
            total += bestValue;
            
            // Log what happened so the UI can show it
            logs.add("Step " + (step + 1) + ": picked T" + (bestRow + 1)
                    + " -> E" + (bestCol + 1) + " (cost " + bestValue + ")");
        }

        // Return everything back to the service
        return new AlgorithmSolveResult(total, assignments, logs, List.of());
    }
}
