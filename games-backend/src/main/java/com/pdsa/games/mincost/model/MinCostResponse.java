package com.pdsa.games.mincost.model;

import java.util.List;

/**
 * Full API payload for POST /api/mincost/start.
 */
public record MinCostResponse(
        int[][] matrix,
        List<Assignment> greedyAssignments,
        List<Assignment> hungarianAssignments,
        List<String> greedyLogs,
        List<String> hungarianLogs,
        int greedyTotalCost,
        int hungarianTotalCost,
        double greedyTimeMs,
        double hungarianTimeMs,
        List<HungarianStepDto> hungarianSteps
) {
}
