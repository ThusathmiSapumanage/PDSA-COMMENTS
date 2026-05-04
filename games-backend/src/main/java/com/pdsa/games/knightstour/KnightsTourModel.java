package com.pdsa.games.knightstour;

import java.util.ArrayList;
import java.util.List;

public class KnightsTourModel {

    public static class Move {
        private int stepNo;
        private int x;
        private int y;

        public Move() {
        }

        public Move(int stepNo, int x, int y) {
            this.stepNo = stepNo;
            this.x = x;
            this.y = y;
        }

        public int getStepNo() {
            return stepNo;
        }

        public void setStepNo(int stepNo) {
            this.stepNo = stepNo;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
    }

    public static class GameConfig {
        private Long sessionId;
        private Integer boardSize;
        private Integer startX;
        private Integer startY;
        private Integer moveCount;

        public GameConfig() {
        }

        public GameConfig(Long sessionId, Integer boardSize, Integer startX, Integer startY, Integer moveCount) {
            this.sessionId = sessionId;
            this.boardSize = boardSize;
            this.startX = startX;
            this.startY = startY;
            this.moveCount = moveCount;
        }

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Integer getBoardSize() {
            return boardSize;
        }

        public void setBoardSize(Integer boardSize) {
            this.boardSize = boardSize;
        }

        public Integer getStartX() {
            return startX;
        }

        public void setStartX(Integer startX) {
            this.startX = startX;
        }

        public Integer getStartY() {
            return startY;
        }

        public void setStartY(Integer startY) {
            this.startY = startY;
        }

        public Integer getMoveCount() {
            return moveCount;
        }

        public void setMoveCount(Integer moveCount) {
            this.moveCount = moveCount;
        }
    }

    public static class AlgorithmResult {
        private String algorithmName;
        private String status;
        private Double executionTimeMs;
        private Integer moveCount;
        private List<Move> moves;
        private String message;

        public AlgorithmResult() {
            this.moves = new ArrayList<>();
        }

        public AlgorithmResult(String algorithmName, String status, Double executionTimeMs, Integer moveCount, List<Move> moves, String message) {
            this.algorithmName = algorithmName;
            this.status = status;
            this.executionTimeMs = executionTimeMs;
            this.moveCount = moveCount;
            this.moves = moves;
            this.message = message;
        }

        public String getAlgorithmName() {
            return algorithmName;
        }

        public void setAlgorithmName(String algorithmName) {
            this.algorithmName = algorithmName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Double getExecutionTimeMs() {
            return executionTimeMs;
        }

        public void setExecutionTimeMs(Double executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
        }

        public Integer getMoveCount() {
            return moveCount;
        }

        public void setMoveCount(Integer moveCount) {
            this.moveCount = moveCount;
        }

        public List<Move> getMoves() {
            return moves;
        }

        public void setMoves(List<Move> moves) {
            this.moves = moves;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class StartRequest {
        private Integer boardSize;

        public StartRequest() {
        }

        public StartRequest(Integer boardSize) {
            this.boardSize = boardSize;
        }

        public Integer getBoardSize() {
            return boardSize;
        }

        public void setBoardSize(Integer boardSize) {
            this.boardSize = boardSize;
        }
    }

    public static class StartResponse {
        private Long sessionId;
        private Long playerId;
        private Integer boardSize;
        private Integer startX;
        private Integer startY;
        private Integer expectedMoveCount;
        private String algorithmName;
        private Double algorithmExecutionTimeMs;
        private String comparisonSummary;
        private List<AlgorithmMetric> algorithmMetrics;
        private String status;
        private String message;

        public StartResponse() {
            this.algorithmMetrics = new ArrayList<>();
        }

        public StartResponse(Long sessionId, Long playerId, Integer boardSize, Integer startX, Integer startY,
                             Integer expectedMoveCount, String algorithmName, Double algorithmExecutionTimeMs,
                             String comparisonSummary, List<AlgorithmMetric> algorithmMetrics,
                             String status, String message) {
            this.sessionId = sessionId;
            this.playerId = playerId;
            this.boardSize = boardSize;
            this.startX = startX;
            this.startY = startY;
            this.expectedMoveCount = expectedMoveCount;
            this.algorithmName = algorithmName;
            this.algorithmExecutionTimeMs = algorithmExecutionTimeMs;
            this.comparisonSummary = comparisonSummary;
            this.algorithmMetrics = algorithmMetrics;
            this.status = status;
            this.message = message;
        }

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Long getPlayerId() {
            return playerId;
        }

        public void setPlayerId(Long playerId) {
            this.playerId = playerId;
        }

        public Integer getBoardSize() {
            return boardSize;
        }

        public void setBoardSize(Integer boardSize) {
            this.boardSize = boardSize;
        }

        public Integer getStartX() {
            return startX;
        }

        public void setStartX(Integer startX) {
            this.startX = startX;
        }

        public Integer getStartY() {
            return startY;
        }

        public void setStartY(Integer startY) {
            this.startY = startY;
        }

        public Integer getExpectedMoveCount() {
            return expectedMoveCount;
        }

        public void setExpectedMoveCount(Integer expectedMoveCount) {
            this.expectedMoveCount = expectedMoveCount;
        }

        public String getAlgorithmName() {
            return algorithmName;
        }

        public void setAlgorithmName(String algorithmName) {
            this.algorithmName = algorithmName;
        }

        public Double getAlgorithmExecutionTimeMs() {
            return algorithmExecutionTimeMs;
        }

        public void setAlgorithmExecutionTimeMs(Double algorithmExecutionTimeMs) {
            this.algorithmExecutionTimeMs = algorithmExecutionTimeMs;
        }

        public String getComparisonSummary() {
            return comparisonSummary;
        }

        public void setComparisonSummary(String comparisonSummary) {
            this.comparisonSummary = comparisonSummary;
        }

        public List<AlgorithmMetric> getAlgorithmMetrics() {
            return algorithmMetrics;
        }

        public void setAlgorithmMetrics(List<AlgorithmMetric> algorithmMetrics) {
            this.algorithmMetrics = algorithmMetrics;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class AnswerRequest {
        private Long sessionId;
        private List<Move> moves;

        public AnswerRequest() {
        }

        public AnswerRequest(Long sessionId, List<Move> moves) {
            this.sessionId = sessionId;
            this.moves = moves;
        }

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public List<Move> getMoves() {
            return moves;
        }

        public void setMoves(List<Move> moves) {
            this.moves = moves;
        }
    }

    public static class HintRequest {
        private Long sessionId;
        private List<Move> moves;

        public HintRequest() {
        }

        public HintRequest(Long sessionId, List<Move> moves) {
            this.sessionId = sessionId;
            this.moves = moves;
        }

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public List<Move> getMoves() {
            return moves;
        }

        public void setMoves(List<Move> moves) {
            this.moves = moves;
        }
    }

    public static class HintResponse {
        private Long sessionId;
        private Long playerId;
        private Integer fromStepNo;
        private Integer fromX;
        private Integer fromY;
        private List<Move> hintMoves;
        private String message;

        public HintResponse() {
            this.hintMoves = new ArrayList<>();
        }

        public HintResponse(Long sessionId, Long playerId, Integer fromStepNo, Integer fromX, Integer fromY, List<Move> hintMoves, String message) {
            this.sessionId = sessionId;
            this.playerId = playerId;
            this.fromStepNo = fromStepNo;
            this.fromX = fromX;
            this.fromY = fromY;
            this.hintMoves = hintMoves;
            this.message = message;
        }

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Long getPlayerId() {
            return playerId;
        }

        public void setPlayerId(Long playerId) {
            this.playerId = playerId;
        }

        public Integer getFromStepNo() {
            return fromStepNo;
        }

        public void setFromStepNo(Integer fromStepNo) {
            this.fromStepNo = fromStepNo;
        }

        public Integer getFromX() {
            return fromX;
        }

        public void setFromX(Integer fromX) {
            this.fromX = fromX;
        }

        public Integer getFromY() {
            return fromY;
        }

        public void setFromY(Integer fromY) {
            this.fromY = fromY;
        }

        public List<Move> getHintMoves() {
            return hintMoves;
        }

        public void setHintMoves(List<Move> hintMoves) {
            this.hintMoves = hintMoves;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class AnswerResponse {
        private Long responseId;
        private Long sessionId;
        private Long playerId;
        private Boolean correct;
        private Boolean validTour;
        private Boolean matchedSavedSolution;
        private String outcome; // WIN / DRAW / LOSE
        private Integer expectedMoveCount;
        private Integer submittedMoveCount;
        private Boolean structuralValid;
        private Boolean completeTour;
        private Boolean deadEnd;
        private Boolean warnsdorffValid;
        private Boolean backtrackingValid;
        private String validationSource;
        private String reasonCode;
        private List<Move> warnsdorffCorrectMoves;
        private Integer warnsdorffMoveCount;
        private List<Move> backtrackingCorrectMoves;
        private Integer backtrackingMoveCount;
        private String message;

        public AnswerResponse() {
            this.warnsdorffCorrectMoves = new ArrayList<>();
            this.backtrackingCorrectMoves = new ArrayList<>();
        }

        public Long getResponseId() {
            return responseId;
        }

        public void setResponseId(Long responseId) {
            this.responseId = responseId;
        }

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Long getPlayerId() {
            return playerId;
        }

        public void setPlayerId(Long playerId) {
            this.playerId = playerId;
        }

        public Boolean getCorrect() {
            return correct;
        }

        public void setCorrect(Boolean correct) {
            this.correct = correct;
        }

        public Boolean getValidTour() {
            return validTour;
        }

        public void setValidTour(Boolean validTour) {
            this.validTour = validTour;
        }

        public Boolean getMatchedSavedSolution() {
            return matchedSavedSolution;
        }

        public void setMatchedSavedSolution(Boolean matchedSavedSolution) {
            this.matchedSavedSolution = matchedSavedSolution;
        }

        public String getOutcome() {
            return outcome;
        }

        public void setOutcome(String outcome) {
            this.outcome = outcome;
        }

        public Integer getExpectedMoveCount() {
            return expectedMoveCount;
        }

        public void setExpectedMoveCount(Integer expectedMoveCount) {
            this.expectedMoveCount = expectedMoveCount;
        }

        public Integer getSubmittedMoveCount() {
            return submittedMoveCount;
        }

        public void setSubmittedMoveCount(Integer submittedMoveCount) {
            this.submittedMoveCount = submittedMoveCount;
        }

        public Boolean getStructuralValid() {
            return structuralValid;
        }

        public void setStructuralValid(Boolean structuralValid) {
            this.structuralValid = structuralValid;
        }

        public Boolean getCompleteTour() {
            return completeTour;
        }

        public void setCompleteTour(Boolean completeTour) {
            this.completeTour = completeTour;
        }

        public Boolean getDeadEnd() {
            return deadEnd;
        }

        public void setDeadEnd(Boolean deadEnd) {
            this.deadEnd = deadEnd;
        }

        public Boolean getWarnsdorffValid() {
            return warnsdorffValid;
        }

        public void setWarnsdorffValid(Boolean warnsdorffValid) {
            this.warnsdorffValid = warnsdorffValid;
        }

        public Boolean getBacktrackingValid() {
            return backtrackingValid;
        }

        public void setBacktrackingValid(Boolean backtrackingValid) {
            this.backtrackingValid = backtrackingValid;
        }

        public String getValidationSource() {
            return validationSource;
        }

        public void setValidationSource(String validationSource) {
            this.validationSource = validationSource;
        }

        public String getReasonCode() {
            return reasonCode;
        }

        public void setReasonCode(String reasonCode) {
            this.reasonCode = reasonCode;
        }

        public List<Move> getWarnsdorffCorrectMoves() {
            return warnsdorffCorrectMoves;
        }

        public void setWarnsdorffCorrectMoves(List<Move> warnsdorffCorrectMoves) {
            this.warnsdorffCorrectMoves = warnsdorffCorrectMoves;
        }

        public Integer getWarnsdorffMoveCount() {
            return warnsdorffMoveCount;
        }

        public void setWarnsdorffMoveCount(Integer warnsdorffMoveCount) {
            this.warnsdorffMoveCount = warnsdorffMoveCount;
        }

        public List<Move> getBacktrackingCorrectMoves() {
            return backtrackingCorrectMoves;
        }

        public void setBacktrackingCorrectMoves(List<Move> backtrackingCorrectMoves) {
            this.backtrackingCorrectMoves = backtrackingCorrectMoves;
        }

        public Integer getBacktrackingMoveCount() {
            return backtrackingMoveCount;
        }

        public void setBacktrackingMoveCount(Integer backtrackingMoveCount) {
            this.backtrackingMoveCount = backtrackingMoveCount;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class CorrectAnswerResponse {
        private Long sessionId;
        private Long playerId;

        private List<Move> warnsdorffMoves;
        private Integer warnsdorffMoveCount;

        private List<Move> backtrackingMoves;
        private Integer backtrackingMoveCount;

        private String message;

        public CorrectAnswerResponse() {
            this.warnsdorffMoves = new ArrayList<>();
            this.backtrackingMoves = new ArrayList<>();
        }

        public CorrectAnswerResponse(Long sessionId, Long playerId,
                                     List<Move> warnsdorffMoves, Integer warnsdorffMoveCount,
                                     List<Move> backtrackingMoves, Integer backtrackingMoveCount,
                                     String message) {
            this.sessionId = sessionId;
            this.playerId = playerId;
            this.warnsdorffMoves = warnsdorffMoves;
            this.warnsdorffMoveCount = warnsdorffMoveCount;
            this.backtrackingMoves = backtrackingMoves;
            this.backtrackingMoveCount = backtrackingMoveCount;
            this.message = message;
        }

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Long getPlayerId() {
            return playerId;
        }

        public void setPlayerId(Long playerId) {
            this.playerId = playerId;
        }

        public List<Move> getWarnsdorffMoves() {
            return warnsdorffMoves;
        }

        public void setWarnsdorffMoves(List<Move> warnsdorffMoves) {
            this.warnsdorffMoves = warnsdorffMoves;
        }

        public Integer getWarnsdorffMoveCount() {
            return warnsdorffMoveCount;
        }

        public void setWarnsdorffMoveCount(Integer warnsdorffMoveCount) {
            this.warnsdorffMoveCount = warnsdorffMoveCount;
        }

        public List<Move> getBacktrackingMoves() {
            return backtrackingMoves;
        }

        public void setBacktrackingMoves(List<Move> backtrackingMoves) {
            this.backtrackingMoves = backtrackingMoves;
        }

        public Integer getBacktrackingMoveCount() {
            return backtrackingMoveCount;
        }

        public void setBacktrackingMoveCount(Integer backtrackingMoveCount) {
            this.backtrackingMoveCount = backtrackingMoveCount;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class AlgorithmMetric {
        private String algorithmName;
        private Double executionTimeMs;
        private Integer moveCount;
        private String status;

        public AlgorithmMetric() {
        }

        public AlgorithmMetric(String algorithmName, Double executionTimeMs, Integer moveCount, String status) {
            this.algorithmName = algorithmName;
            this.executionTimeMs = executionTimeMs;
            this.moveCount = moveCount;
            this.status = status;
        }

        public String getAlgorithmName() {
            return algorithmName;
        }

        public void setAlgorithmName(String algorithmName) {
            this.algorithmName = algorithmName;
        }

        public Double getExecutionTimeMs() {
            return executionTimeMs;
        }

        public void setExecutionTimeMs(Double executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
        }

        public Integer getMoveCount() {
            return moveCount;
        }

        public void setMoveCount(Integer moveCount) {
            this.moveCount = moveCount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}