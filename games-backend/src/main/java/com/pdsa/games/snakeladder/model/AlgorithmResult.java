package com.pdsa.games.snakeladder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holds the output from running one algorithm on a board.
 * This is used to pass data between service and controller — not a DB table by
 * itself.
 * Execution time + result are saved to Algorithm_Execute table via repository.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlgorithmResult {

    private String algorithmName; // e.g. "Snake and Ladder - BFS" or "Snake and Ladder - Dynamic Programming"
    private int minimumThrows; // answer produced by this algorithm
    private double executionTimeMs; // how long the algorithm took (milliseconds)
    private boolean success; // did it find an answer?
}