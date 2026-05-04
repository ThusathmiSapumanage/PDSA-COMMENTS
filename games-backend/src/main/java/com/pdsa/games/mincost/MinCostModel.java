package com.pdsa.games.mincost;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MIN COST MODEL:
 * This file holds all our data structures (DTOs).
 * We use Lombok @Data to automatically generate getters, setters, and toString.
 * This is the professional standard used by other teams like Knight's Tour.
 */
public class MinCostModel {

    // Helper for cost assignments
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Assignment {
        private int row;
        private int col;
        private int cost;
    }

    // Step data for the Hungarian visualizer
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HungarianStep {
        private String type;
        private int[][] matrix;
        private String description;
        private List<Integer> rowMinVals;
        private List<Integer> colMinVals;
        private List<Boolean> markedRows;
        private List<Boolean> markedCols;
        private Double delta;
    }

    // DATA FOR THE API REQUEST
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartRequest {
        private Integer n;
        private Integer minCost;
        private Integer maxCost;
        private Long playerId; // Consistent with other games
    }

    // DATA FOR THE MAIN API RESPONSE
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameResponse {
        private int[][] matrix;
        private List<Assignment> greedyAssignments;
        private List<Assignment> hungarianAssignments;
        private List<String> greedyLogs;
        private List<String> hungarianLogs;
        private int greedyTotalCost;
        private int hungarianTotalCost;
        private double greedyTimeMs;
        private double hungarianTimeMs;
        private List<HungarianStep> hungarianSteps;
    }

    // DATA FOR THE HISTORY API
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItem {
        private String date;
        private int n;
        private int cost;
        private double greedyTime;
        private double hungarianTime;
    }
}