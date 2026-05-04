package com.pdsa.games.knightstour;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Service
public class KnightsTourService {

    private static final int[] DX = {2, 1, -1, -2, -2, -1, 1, 2};
    private static final int[] DY = {1, 2, 2, 1, -1, -2, -2, -1};

    private static final String WARNSDORFF_NAME = "Knight Tour - Warnsdorff";
    private static final String BACKTRACKING_NAME = "Knight Tour - Backtracking";

    private static final int MAX_ATTEMPTS_TO_GENERATE = 40;
    private static final long BACKTRACKING_TIMEOUT_MS_8 = 2000L;
    private static final long BACKTRACKING_TIMEOUT_MS_16 = 2500L;

    private final KnightsTourRepository repository;
    private final Random random = new Random();

    public KnightsTourService(KnightsTourRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public KnightsTourModel.StartResponse startRound(KnightsTourModel.StartRequest request, String authenticatedEmail) {
        validateStartRequest(request);

        Long playerId = resolveAuthenticatedPlayerId(authenticatedEmail);

        Integer warnsdorffId = mustFindAlgorithmId(WARNSDORFF_NAME);
        Integer backtrackingId = mustFindAlgorithmId(BACKTRACKING_NAME);

        GeneratedRound round = generateSuccessfulRound(request.getBoardSize());

        Long sessionId = repository.createGameSession(playerId);

        KnightsTourModel.AlgorithmResult selected = choosePrimarySolution(round.warnsdorffResult, round.backtrackingResult);

        repository.saveKnightTourGame(
                sessionId,
                request.getBoardSize(),
                round.startX + 1,
                round.startY + 1,
                selected.getMoveCount()
        );

        saveAlgorithmRun(sessionId, warnsdorffId, round.warnsdorffResult);
        saveAlgorithmRun(sessionId, backtrackingId, round.backtrackingResult);

        List<KnightsTourModel.AlgorithmMetric> metrics = new ArrayList<>();
        metrics.add(new KnightsTourModel.AlgorithmMetric(
                round.warnsdorffResult.getAlgorithmName(),
                round.warnsdorffResult.getExecutionTimeMs(),
                round.warnsdorffResult.getMoveCount(),
                round.warnsdorffResult.getStatus()
        ));
        metrics.add(new KnightsTourModel.AlgorithmMetric(
                round.backtrackingResult.getAlgorithmName(),
                round.backtrackingResult.getExecutionTimeMs(),
                round.backtrackingResult.getMoveCount(),
                round.backtrackingResult.getStatus()
        ));

        return new KnightsTourModel.StartResponse(
                sessionId,
                playerId,
                request.getBoardSize(),
                round.startX + 1,
                round.startY + 1,
                selected.getMoveCount(),
                selected.getAlgorithmName(),
                selected.getExecutionTimeMs(),
                buildComparisonSummary(round.warnsdorffResult, round.backtrackingResult),
                metrics,
                "READY",
                "Knight's Tour round generated successfully."
        );
    }

    @Transactional
    public KnightsTourModel.AnswerResponse submitAnswer(KnightsTourModel.AnswerRequest request, String authenticatedEmail) {
        validateAnswerRequest(request);

        Long playerId = resolveAuthenticatedPlayerId(authenticatedEmail);

        ensureSessionOwnedByPlayer(request.getSessionId(), playerId);

        KnightsTourModel.GameConfig gameConfig = repository.findGameConfig(request.getSessionId());
        if (gameConfig == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Knight Tour game configuration not found for this session.");
        }

        Integer warnsdorffId = mustFindAlgorithmId(WARNSDORFF_NAME);
        Integer backtrackingId = mustFindAlgorithmId(BACKTRACKING_NAME);

        List<KnightsTourModel.Move> warnsdorffCorrectMoves = repository.findCorrectMoves(request.getSessionId(), warnsdorffId);
        List<KnightsTourModel.Move> backtrackingCorrectMoves = repository.findCorrectMoves(request.getSessionId(), backtrackingId);

        if (warnsdorffCorrectMoves.isEmpty() && backtrackingCorrectMoves.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No saved algorithm solutions were found for this session.");
        }

        List<KnightsTourModel.Move> submittedMoves = normalizeMovesByStep(request.getMoves());

        StructuralValidationResult structural = validateStructure(
                submittedMoves,
                gameConfig.getBoardSize(),
                gameConfig.getStartX(),
                gameConfig.getStartY(),
                gameConfig.getMoveCount()
        );

        boolean warnsdorffMatched = !warnsdorffCorrectMoves.isEmpty() && areMovesExactlyEqual(warnsdorffCorrectMoves, submittedMoves);
        boolean backtrackingMatched = !backtrackingCorrectMoves.isEmpty() && areMovesExactlyEqual(backtrackingCorrectMoves, submittedMoves);

        String validationSource = null;
        String outcome;
        String reasonCode;
        String message;
        boolean validTour;
        boolean matchedSavedSolution;
        boolean isCorrect;

        if (!structural.isValid()) {
            outcome = "LOSE";
            reasonCode = structural.getReasonCode();
            message = buildStructuralFailureMessage(reasonCode);
            validTour = false;
            matchedSavedSolution = false;
            isCorrect = false;
        } else if (warnsdorffMatched && backtrackingMatched) {
            outcome = "WIN";
            validationSource = "BOTH";
            reasonCode = "EXACT_MATCH_BOTH";
            message = "Excellent. Your valid Knight's Tour exactly matches both saved algorithm solutions.";
            validTour = true;
            matchedSavedSolution = true;
            isCorrect = true;
        } else if (warnsdorffMatched) {
            outcome = "WIN";
            validationSource = "WARNSDORFF";
            reasonCode = "EXACT_MATCH_WARNSDORFF";
            message = "Excellent. Your valid Knight's Tour exactly matches the saved Warnsdorff solution.";
            validTour = true;
            matchedSavedSolution = true;
            isCorrect = true;
        } else if (backtrackingMatched) {
            outcome = "WIN";
            validationSource = "BACKTRACKING";
            reasonCode = "EXACT_MATCH_BACKTRACKING";
            message = "Excellent. Your valid Knight's Tour exactly matches the saved Backtracking solution.";
            validTour = true;
            matchedSavedSolution = true;
            isCorrect = true;
        } else {
            outcome = "DRAW";
            reasonCode = "VALID_TOUR_DIFFERENT_FROM_SAVED_SOLUTIONS";
            message = "Your path is a valid complete Knight's Tour, but it is different from the saved algorithm solutions.";
            validTour = true;
            matchedSavedSolution = false;
            isCorrect = false;
        }

        String responseSummary = buildResponseSummary(
                submittedMoves.size(),
                validTour,
                isCorrect,
                matchedSavedSolution,
                reasonCode,
                validationSource
        );

        Long responseId = repository.saveResponse(
                request.getSessionId(),
                responseSummary,
                isCorrect
        );

        repository.saveResponseMoves(responseId, submittedMoves);

        KnightsTourModel.AnswerResponse response = new KnightsTourModel.AnswerResponse();
        response.setResponseId(responseId);
        response.setSessionId(request.getSessionId());
        response.setPlayerId(playerId);
        response.setCorrect(isCorrect);
        response.setValidTour(validTour);
        response.setMatchedSavedSolution(matchedSavedSolution);
        response.setOutcome(outcome);
        response.setExpectedMoveCount(gameConfig.getMoveCount());
        response.setSubmittedMoveCount(submittedMoves.size());
        response.setStructuralValid(structural.isValid());
        response.setCompleteTour(structural.isComplete());
        response.setDeadEnd(structural.isDeadEnd());
        response.setWarnsdorffValid(warnsdorffMatched);
        response.setBacktrackingValid(backtrackingMatched);
        response.setValidationSource(validationSource);
        response.setReasonCode(reasonCode);
        response.setMessage(message);

        if (!"WIN".equals(outcome)) {
            response.setWarnsdorffCorrectMoves(warnsdorffCorrectMoves);
            response.setWarnsdorffMoveCount(warnsdorffCorrectMoves.size());
            response.setBacktrackingCorrectMoves(backtrackingCorrectMoves);
            response.setBacktrackingMoveCount(backtrackingCorrectMoves.size());
        }

        return response;
    }

    @Transactional(readOnly = true)
    public KnightsTourModel.HintResponse getHint(KnightsTourModel.HintRequest request, String authenticatedEmail) {
        validateHintRequest(request);

        Long playerId = resolveAuthenticatedPlayerId(authenticatedEmail);

        ensureSessionOwnedByPlayer(request.getSessionId(), playerId);

        KnightsTourModel.GameConfig gameConfig = repository.findGameConfig(request.getSessionId());
        if (gameConfig == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Knight Tour game configuration not found for this session.");
        }

        List<KnightsTourModel.Move> submittedMoves = normalizeMovesByStep(request.getMoves());

        StructuralValidationResult structural = validateStructureForHint(
                submittedMoves,
                gameConfig.getBoardSize(),
                gameConfig.getStartX(),
                gameConfig.getStartY()
        );

        if (!structural.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, buildStructuralFailureMessage(structural.getReasonCode()));
        }

        KnightsTourModel.Move lastMove = submittedMoves.get(submittedMoves.size() - 1);
        Set<String> visited = buildVisitedSet(submittedMoves);

        List<KnightsTourModel.Move> hintMoves = findLegalUnvisitedNextMoves(
                lastMove,
                submittedMoves.size() + 1,
                visited,
                gameConfig.getBoardSize()
        );

        return new KnightsTourModel.HintResponse(
                request.getSessionId(),
                playerId,
                lastMove.getStepNo(),
                lastMove.getX(),
                lastMove.getY(),
                hintMoves,
                hintMoves.isEmpty()
                        ? "No legal unvisited moves are available from your current square."
                        : "Possible legal next moves from your current square are returned."
        );
    }

    @Transactional(readOnly = true)
    public KnightsTourModel.CorrectAnswerResponse getCorrectAnswer(Long sessionId, String authenticatedEmail) {
        Long playerId = resolveAuthenticatedPlayerId(authenticatedEmail);

        if (sessionId == null || sessionId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid sessionId is required.");
        }

        if (!repository.sessionExists(sessionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session does not exist.");
        }

        ensureSessionOwnedByPlayer(sessionId, playerId);

        Integer warnsdorffId = mustFindAlgorithmId(WARNSDORFF_NAME);
        Integer backtrackingId = mustFindAlgorithmId(BACKTRACKING_NAME);

        List<KnightsTourModel.Move> warnsdorffMoves = repository.findCorrectMoves(sessionId, warnsdorffId);
        List<KnightsTourModel.Move> backtrackingMoves = repository.findCorrectMoves(sessionId, backtrackingId);

        List<KnightsTourModel.Move> selected = !warnsdorffMoves.isEmpty() ? warnsdorffMoves : backtrackingMoves;

        if (selected.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Correct Knight Tour path not found for this session.");
        }

        return new KnightsTourModel.CorrectAnswerResponse(
                sessionId,
                playerId,
                warnsdorffMoves,
                warnsdorffMoves.size(),
                backtrackingMoves,
                backtrackingMoves.size(),
                "Both algorithm solutions loaded."
        );
    }

    private void saveAlgorithmRun(Long sessionId, int algorithmId, KnightsTourModel.AlgorithmResult result) {

        repository.saveAlgorithmExecution(
                sessionId,
                algorithmId,
                result.getExecutionTimeMs() == null ? 0.0 : result.getExecutionTimeMs(),
                normalizeStatusForDb(result.getStatus())
        );

        if ("SUCCESS".equalsIgnoreCase(result.getStatus())
                && result.getMoves() != null
                && !result.getMoves().isEmpty()) {

            repository.saveKnightMoves(sessionId, algorithmId, result.getMoves());
        }
    }

    private KnightsTourModel.AlgorithmResult choosePrimarySolution(
            KnightsTourModel.AlgorithmResult warnsdorff,
            KnightsTourModel.AlgorithmResult backtracking
    ) {
        if (isSuccessful(warnsdorff)) {
            return warnsdorff;
        }
        if (isSuccessful(backtracking)) {
            return backtracking;
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate a solvable Knight's Tour round.");
    }

    private boolean isSuccessful(KnightsTourModel.AlgorithmResult result) {
        return result != null
                && "SUCCESS".equalsIgnoreCase(result.getStatus())
                && result.getMoves() != null
                && !result.getMoves().isEmpty();
    }

    private GeneratedRound generateSuccessfulRound(int boardSize) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS_TO_GENERATE; attempt++) {
            int startX = random.nextInt(boardSize);
            int startY = random.nextInt(boardSize);

            KnightsTourModel.AlgorithmResult warnsdorff = executeWarnsdorff(boardSize, startX, startY);
            KnightsTourModel.AlgorithmResult backtracking = executeBacktracking(boardSize, startX, startY);

            if (isSuccessful(warnsdorff) || isSuccessful(backtracking)) {
                return new GeneratedRound(startX, startY, warnsdorff, backtracking);
            }
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not generate a Knight's Tour round after multiple attempts.");
    }

    private KnightsTourModel.AlgorithmResult executeWarnsdorff(int boardSize, int startX, int startY) {
        long started = System.nanoTime();

        try {
            List<KnightsTourModel.Move> moves = solveWarnsdorff(boardSize, startX, startY);

            if (moves.size() == boardSize * boardSize) {
                return new KnightsTourModel.AlgorithmResult(
                        WARNSDORFF_NAME,
                        "SUCCESS",
                        elapsedMs(started),
                        moves.size(),
                        moves,
                        "Warnsdorff generated a full tour."
                );
            }

            return new KnightsTourModel.AlgorithmResult(
                    WARNSDORFF_NAME,
                    "FAILURE",
                    elapsedMs(started),
                    moves.size(),
                    moves,
                    "Warnsdorff did not generate a full tour."
            );
        } catch (Exception ex) {
            return new KnightsTourModel.AlgorithmResult(
                    WARNSDORFF_NAME,
                    "ERROR",
                    elapsedMs(started),
                    0,
                    new ArrayList<>(),
                    ex.getMessage()
            );
        }
    }

    private KnightsTourModel.AlgorithmResult executeBacktracking(int boardSize, int startX, int startY) {
        long started = System.nanoTime();

        try {
            long timeoutMs = boardSize == 8 ? BACKTRACKING_TIMEOUT_MS_8 : BACKTRACKING_TIMEOUT_MS_16;
            long deadlineNanos = System.nanoTime() + (timeoutMs * 1_000_000L);

            List<KnightsTourModel.Move> moves = solveBacktracking(boardSize, startX, startY, deadlineNanos);

            if (moves.size() == boardSize * boardSize) {
                return new KnightsTourModel.AlgorithmResult(
                        BACKTRACKING_NAME,
                        "SUCCESS",
                        elapsedMs(started),
                        moves.size(),
                        moves,
                        "Backtracking generated a full tour."
                );
            }

            if (System.nanoTime() > deadlineNanos) {
                return new KnightsTourModel.AlgorithmResult(
                        BACKTRACKING_NAME,
                        "TIMEOUT",
                        elapsedMs(started),
                        moves.size(),
                        new ArrayList<>(),
                        "Backtracking timed out before completing the tour."
                );
            }

            return new KnightsTourModel.AlgorithmResult(
                    BACKTRACKING_NAME,
                    "FAILURE",
                    elapsedMs(started),
                    moves.size(),
                    new ArrayList<>(),
                    "Backtracking did not generate a full tour."
            );
        } catch (TimeoutException ex) {
            return new KnightsTourModel.AlgorithmResult(
                    BACKTRACKING_NAME,
                    "TIMEOUT",
                    elapsedMs(started),
                    0,
                    new ArrayList<>(),
                    "Backtracking timed out before completing the tour."
            );
        } catch (Exception ex) {
            return new KnightsTourModel.AlgorithmResult(
                    BACKTRACKING_NAME,
                    "ERROR",
                    elapsedMs(started),
                    0,
                    new ArrayList<>(),
                    ex.getMessage()
            );
        }
    }

    private List<KnightsTourModel.Move> solveWarnsdorff(int boardSize, int startX, int startY) {
        boolean[][] visited = new boolean[boardSize][boardSize];
        List<KnightsTourModel.Move> moves = new ArrayList<>();

        int currentX = startX;
        int currentY = startY;

        visited[currentY][currentX] = true;
        moves.add(new KnightsTourModel.Move(1, currentX + 1, currentY + 1));

        for (int step = 2; step <= boardSize * boardSize; step++) {
            int[] next = chooseBestNextMove(currentX, currentY, visited, boardSize);
            if (next == null) {
                break;
            }

            currentX = next[0];
            currentY = next[1];
            visited[currentY][currentX] = true;
            moves.add(new KnightsTourModel.Move(step, currentX + 1, currentY + 1));
        }

        return moves;
    }

    private int[] chooseBestNextMove(int currentX, int currentY, boolean[][] visited, int boardSize) {
        List<int[]> candidates = new ArrayList<>();

        for (int i = 0; i < DX.length; i++) {
            int nx = currentX + DX[i];
            int ny = currentY + DY[i];

            if (isValidZeroBased(nx, ny, boardSize) && !visited[ny][nx]) {
                int onwardMoves = countOnwardMoves(nx, ny, visited, boardSize);
                candidates.add(new int[]{nx, ny, onwardMoves});
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(Comparator.comparingInt(a -> a[2]));

        int minOnward = candidates.get(0)[2];
        List<int[]> tied = new ArrayList<>();
        for (int[] candidate : candidates) {
            if (candidate[2] == minOnward) {
                tied.add(candidate);
            }
        }

        int[] chosen = tied.get(random.nextInt(tied.size()));
        return new int[]{chosen[0], chosen[1]};
    }

    private int countOnwardMoves(int x, int y, boolean[][] visited, int boardSize) {
        int count = 0;
        for (int i = 0; i < DX.length; i++) {
            int nx = x + DX[i];
            int ny = y + DY[i];
            if (isValidZeroBased(nx, ny, boardSize) && !visited[ny][nx]) {
                count++;
            }
        }
        return count;
    }

    private List<KnightsTourModel.Move> solveBacktracking(int boardSize, int startX, int startY, long deadlineNanos) {
        boolean[][] visited = new boolean[boardSize][boardSize];
        List<KnightsTourModel.Move> path = new ArrayList<>();

        visited[startY][startX] = true;
        path.add(new KnightsTourModel.Move(1, startX + 1, startY + 1));

        boolean solved = backtrack(boardSize, startX, startY, 2, visited, path, deadlineNanos);
        if (solved) {
            return path;
        }
        return new ArrayList<>();
    }

    private boolean backtrack(
            int boardSize,
            int currentX,
            int currentY,
            int stepNo,
            boolean[][] visited,
            List<KnightsTourModel.Move> path,
            long deadlineNanos
    ) {
        if (System.nanoTime() > deadlineNanos) {
            throw new TimeoutException();
        }

        if (stepNo > boardSize * boardSize) {
            return true;
        }

        List<int[]> candidates = orderedBacktrackingCandidates(currentX, currentY, visited, boardSize);

        for (int[] candidate : candidates) {
            int nx = candidate[0];
            int ny = candidate[1];

            visited[ny][nx] = true;
            path.add(new KnightsTourModel.Move(stepNo, nx + 1, ny + 1));

            if (backtrack(boardSize, nx, ny, stepNo + 1, visited, path, deadlineNanos)) {
                return true;
            }

            path.remove(path.size() - 1);
            visited[ny][nx] = false;
        }

        return false;
    }

    private List<int[]> orderedBacktrackingCandidates(int currentX, int currentY, boolean[][] visited, int boardSize) {
        List<int[]> candidates = new ArrayList<>();

        for (int i = 0; i < DX.length; i++) {
            int nx = currentX + DX[i];
            int ny = currentY + DY[i];
            if (isValidZeroBased(nx, ny, boardSize) && !visited[ny][nx]) {
                int onward = countOnwardMoves(nx, ny, visited, boardSize);
                candidates.add(new int[]{nx, ny, onward});
            }
        }

        candidates.sort(Comparator.comparingInt(a -> a[2]));
        return candidates;
    }

    private StructuralValidationResult validateStructure(
            List<KnightsTourModel.Move> moves,
            int boardSize,
            int startX,
            int startY,
            int expectedMoveCount
    ) {
        if (moves == null || moves.isEmpty()) {
            return new StructuralValidationResult(false, false, false, "INCOMPLETE_TOUR");
        }

        KnightsTourModel.Move first = moves.get(0);
        if (first.getStepNo() != 1) {
            return new StructuralValidationResult(false, false, false, "INVALID_STEP_SEQUENCE");
        }

        if (first.getX() != startX || first.getY() != startY) {
            return new StructuralValidationResult(false, false, false, "WRONG_START_POSITION");
        }

        Set<String> visited = new HashSet<>();

        for (int i = 0; i < moves.size(); i++) {
            KnightsTourModel.Move move = moves.get(i);

            if (move.getStepNo() != i + 1) {
                return new StructuralValidationResult(false, false, false, "INVALID_STEP_SEQUENCE");
            }

            if (!isWithinBoard(move.getX(), move.getY(), boardSize)) {
                return new StructuralValidationResult(false, false, false, "OUT_OF_BOUNDS");
            }

            String key = move.getX() + "," + move.getY();
            if (!visited.add(key)) {
                return new StructuralValidationResult(false, false, false, "DUPLICATE_CELL");
            }

            if (i > 0) {
                KnightsTourModel.Move prev = moves.get(i - 1);
                if (!isKnightMove(prev.getX(), prev.getY(), move.getX(), move.getY())) {
                    return new StructuralValidationResult(false, false, false, "ILLEGAL_MOVE_SEQUENCE");
                }
            }
        }

        boolean complete = moves.size() == expectedMoveCount;

        if (!complete) {
            KnightsTourModel.Move last = moves.get(moves.size() - 1);
            boolean deadEnd = hasNoLegalUnvisitedMove(last, visited, boardSize);
            if (deadEnd) {
                return new StructuralValidationResult(false, false, true, "DEAD_END_BEFORE_COMPLETION");
            }
            return new StructuralValidationResult(false, false, false, "INCOMPLETE_TOUR");
        }

        return new StructuralValidationResult(true, true, false, "VALID_TOUR");
    }

    private StructuralValidationResult validateStructureForHint(
            List<KnightsTourModel.Move> moves,
            int boardSize,
            int startX,
            int startY
    ) {
        if (moves == null || moves.isEmpty()) {
            return new StructuralValidationResult(false, false, false, "INCOMPLETE_TOUR");
        }

        KnightsTourModel.Move first = moves.get(0);
        if (first.getStepNo() != 1) {
            return new StructuralValidationResult(false, false, false, "INVALID_STEP_SEQUENCE");
        }

        if (first.getX() != startX || first.getY() != startY) {
            return new StructuralValidationResult(false, false, false, "WRONG_START_POSITION");
        }

        Set<String> visited = new HashSet<>();

        for (int i = 0; i < moves.size(); i++) {
            KnightsTourModel.Move move = moves.get(i);

            if (move.getStepNo() != i + 1) {
                return new StructuralValidationResult(false, false, false, "INVALID_STEP_SEQUENCE");
            }

            if (!isWithinBoard(move.getX(), move.getY(), boardSize)) {
                return new StructuralValidationResult(false, false, false, "OUT_OF_BOUNDS");
            }

            String key = move.getX() + "," + move.getY();
            if (!visited.add(key)) {
                return new StructuralValidationResult(false, false, false, "DUPLICATE_CELL");
            }

            if (i > 0) {
                KnightsTourModel.Move prev = moves.get(i - 1);
                if (!isKnightMove(prev.getX(), prev.getY(), move.getX(), move.getY())) {
                    return new StructuralValidationResult(false, false, false, "ILLEGAL_MOVE_SEQUENCE");
                }
            }
        }

        return new StructuralValidationResult(true, false, false, "VALID_TOUR");
    }

    private List<KnightsTourModel.Move> findLegalUnvisitedNextMoves(
            KnightsTourModel.Move lastMove,
            int nextStepNo,
            Set<String> visited,
            int boardSize
    ) {
        List<KnightsTourModel.Move> hintMoves = new ArrayList<>();

        for (int i = 0; i < DX.length; i++) {
            int nx = lastMove.getX() + DX[i];
            int ny = lastMove.getY() + DY[i];

            if (isWithinBoard(nx, ny, boardSize)) {
                String key = nx + "," + ny;
                if (!visited.contains(key)) {
                    hintMoves.add(new KnightsTourModel.Move(nextStepNo, nx, ny));
                }
            }
        }

        return hintMoves;
    }

    private Set<String> buildVisitedSet(List<KnightsTourModel.Move> moves) {
        Set<String> visited = new HashSet<>();
        for (KnightsTourModel.Move move : moves) {
            visited.add(move.getX() + "," + move.getY());
        }
        return visited;
    }

    private boolean hasNoLegalUnvisitedMove(KnightsTourModel.Move move, Set<String> visited, int boardSize) {
        for (int i = 0; i < DX.length; i++) {
            int nx = move.getX() + DX[i];
            int ny = move.getY() + DY[i];

            if (isWithinBoard(nx, ny, boardSize) && !visited.contains(nx + "," + ny)) {
                return false;
            }
        }
        return true;
    }

    private boolean isKnightMove(int fromX, int fromY, int toX, int toY) {
        int dx = Math.abs(toX - fromX);
        int dy = Math.abs(toY - fromY);
        return (dx == 2 && dy == 1) || (dx == 1 && dy == 2);
    }

    private boolean isWithinBoard(int x, int y, int boardSize) {
        return x >= 1 && x <= boardSize && y >= 1 && y <= boardSize;
    }

    private boolean isValidZeroBased(int x, int y, int boardSize) {
        return x >= 0 && x < boardSize && y >= 0 && y < boardSize;
    }

    private List<KnightsTourModel.Move> normalizeMovesByStep(List<KnightsTourModel.Move> moves) {
        if (moves == null) {
            return new ArrayList<>();
        }

        List<KnightsTourModel.Move> normalized = new ArrayList<>(moves);
        normalized.sort(Comparator.comparingInt(KnightsTourModel.Move::getStepNo));
        return normalized;
    }

    private boolean areMovesExactlyEqual(List<KnightsTourModel.Move> a, List<KnightsTourModel.Move> b) {
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }

        for (int i = 0; i < a.size(); i++) {
            KnightsTourModel.Move left = a.get(i);
            KnightsTourModel.Move right = b.get(i);

            if (left.getStepNo() != right.getStepNo()
                    || left.getX() != right.getX()
                    || left.getY() != right.getY()) {
                return false;
            }
        }

        return true;
    }

    private String buildStructuralFailureMessage(String reasonCode) {
        switch (reasonCode) {
            case "WRONG_START_POSITION":
                return "The first move does not match the required starting square.";
            case "OUT_OF_BOUNDS":
                return "At least one move goes outside the board.";
            case "DUPLICATE_CELL":
                return "The same square was visited more than once.";
            case "INVALID_STEP_SEQUENCE":
                return "The move numbering is invalid.";
            case "ILLEGAL_MOVE_SEQUENCE":
                return "At least one move is not a legal knight move.";
            case "DEAD_END_BEFORE_COMPLETION":
                return "You reached a dead end before visiting every square.";
            case "INCOMPLETE_TOUR":
                return "The submitted path does not cover the whole board.";
            default:
                return "The submitted path failed structural validation.";
        }
    }

    private String buildResponseSummary(
            int submittedMoveCount,
            boolean validTour,
            boolean isCorrect,
            boolean matchedSavedSolution,
            String reasonCode,
            String validationSource
    ) {
        // Keep VERY SHORT (<50 chars)
        return "m=" + submittedMoveCount +
                ";v=" + (validTour ? 1 : 0) +
                ";c=" + (isCorrect ? 1 : 0) +
                ";r=" + reasonCode;
    }

    private String buildComparisonSummary(
            KnightsTourModel.AlgorithmResult warnsdorff,
            KnightsTourModel.AlgorithmResult backtracking
    ) {
        String warnsdorffTime = warnsdorff.getExecutionTimeMs() == null ? "-" : String.format("%.3f", warnsdorff.getExecutionTimeMs());
        String backtrackingTime = backtracking.getExecutionTimeMs() == null ? "-" : String.format("%.3f", backtracking.getExecutionTimeMs());

        return WARNSDORFF_NAME + ": " + warnsdorff.getStatus() + " (" + warnsdorffTime + " ms), " +
                BACKTRACKING_NAME + ": " + backtracking.getStatus() + " (" + backtrackingTime + " ms)";
    }

    private double elapsedMs(long startedNano) {
        return (System.nanoTime() - startedNano) / 1_000_000.0;
    }

    private String normalizeStatusForDb(String status) {
        if (status == null) {
            return "ERROR";
        }

        switch (status.toUpperCase()) {
            case "SUCCESS":
                return "SUCCESS";
            case "FAILURE":
                return "FAILURE";
            case "TIMEOUT":
                return "TIMEOUT";
            default:
                return "ERROR";
        }
    }

    private void validateStartRequest(KnightsTourModel.StartRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        if (request.getBoardSize() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Board size is required.");
        }
        if (request.getBoardSize() != 8 && request.getBoardSize() != 16) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Board size must be either 8 or 16.");
        }
    }

    private void validateAnswerRequest(KnightsTourModel.AnswerRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        if (request.getSessionId() == null || request.getSessionId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid sessionId is required.");
        }
        if (request.getMoves() == null || request.getMoves().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one move is required.");
        }
    }

    private void validateHintRequest(KnightsTourModel.HintRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        if (request.getSessionId() == null || request.getSessionId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid sessionId is required.");
        }
        if (request.getMoves() == null || request.getMoves().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one move is required to request a hint.");
        }
    }

    private Integer mustFindAlgorithmId(String algorithmName) {
        Integer id = repository.findAlgorithmId(algorithmName);
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Algorithm not found: " + algorithmName);
        }
        return id;
    }

    private Long resolveAuthenticatedPlayerId(String authenticatedEmail) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user email is missing.");
        }

        Long playerId = repository.findPlayerIdByEmail(authenticatedEmail);
        if (playerId == null || !repository.playerExists(playerId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Authenticated player does not exist. Make sure the logged-in user's email exists in Player.Player_Email.");
        }

        return playerId;
    }

    private void ensureSessionOwnedByPlayer(Long sessionId, Long playerId) {
        if (!repository.sessionExists(sessionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session does not exist.");
        }

        if (!repository.sessionBelongsToPlayer(sessionId, playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This session does not belong to the authenticated player.");
        }
    }

    private static class GeneratedRound {
        private final int startX;
        private final int startY;
        private final KnightsTourModel.AlgorithmResult warnsdorffResult;
        private final KnightsTourModel.AlgorithmResult backtrackingResult;

        private GeneratedRound(int startX, int startY,
                               KnightsTourModel.AlgorithmResult warnsdorffResult,
                               KnightsTourModel.AlgorithmResult backtrackingResult) {
            this.startX = startX;
            this.startY = startY;
            this.warnsdorffResult = warnsdorffResult;
            this.backtrackingResult = backtrackingResult;
        }
    }

    private static class StructuralValidationResult {
        private final boolean valid;
        private final boolean complete;
        private final boolean deadEnd;
        private final String reasonCode;

        private StructuralValidationResult(boolean valid, boolean complete, boolean deadEnd, String reasonCode) {
            this.valid = valid;
            this.complete = complete;
            this.deadEnd = deadEnd;
            this.reasonCode = reasonCode;
        }

        public boolean isValid() {
            return valid;
        }

        public boolean isComplete() {
            return complete;
        }

        public boolean isDeadEnd() {
            return deadEnd;
        }

        public String getReasonCode() {
            return reasonCode;
        }
    }

    private static class TimeoutException extends RuntimeException {
    }
}