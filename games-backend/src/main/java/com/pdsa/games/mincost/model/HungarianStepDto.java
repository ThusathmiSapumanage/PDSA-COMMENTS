package com.pdsa.games.mincost.model;

/**
 * One visualization step of the Hungarian algorithm (for the Next.js UI).
 */
public record HungarianStepDto(
        String type,
        int[][] matrix,
        String description,
        int[] rowMinVals,
        int[] colMinVals,
        boolean[] markedRows,
        boolean[] markedCols,
        Integer delta
) {
}
