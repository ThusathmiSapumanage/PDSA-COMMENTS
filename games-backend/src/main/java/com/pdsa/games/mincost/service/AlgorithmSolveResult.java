package com.pdsa.games.mincost.service;

import com.pdsa.games.mincost.MinCostModel;
import java.util.List;

/**
 * Result of running one assignment algorithm on a cost matrix.
 * Uses the professional MinCostModel classes.
 */
public record AlgorithmSolveResult(
        int totalCost,
        List<MinCostModel.Assignment> assignments,
        List<String> logs,
        List<MinCostModel.HungarianStep> hungarianSteps
) {
}
