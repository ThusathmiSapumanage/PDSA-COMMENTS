package com.pdsa.games.queenspuzzle;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class QueensModel {

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Position {
		private int x;
		private int y;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class StartRequest {
		private Integer boardSize;
		private Integer queenCount;
		private Long timeBudgetMs;
		private Boolean skipCache;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class StartResponse {
		private Long sessionId;
		private Integer boardSize;
		private Integer queenCount;
		private Integer totalSolutions;
		private Double sequentialTimeMs;
		private Double threadedTimeMs;
		private Integer sequentialFound;
		private Integer threadedFound;
		private Boolean sequentialHitLimit;
		private Boolean threadedHitLimit;
		private Long timeBudgetMs;
		private String message;
		/** "READY" (synchronous result) or "RUNNING" (async — poll /progress for live updates). */
		private String status;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ProgressResponse {
		private Long sessionId;
		private Integer boardSize;
		private Integer queenCount;
		private Long sequentialFound;
		private Long threadedFound;
		private Long sequentialTimeMs;
		private Long threadedTimeMs;
		private Boolean sequentialDone;
		private Boolean threadedDone;
		private String status;
		private Long totalSolutions;
		private String message;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SubmitRequest {
		private Long sessionId;
		private List<Position> queens;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SubmitResponse {
		private Long sessionId;
		private Long playerId;
		private Boolean correct;
		private Boolean alreadyDiscovered;
		private Integer solutionsDiscovered;
		private Integer totalSolutions;
		private String message;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class StatusResponse {
		private Long sessionId;
		private Integer totalSolutions;
		private Integer solutionsDiscovered;
		private String message;
	}
}
