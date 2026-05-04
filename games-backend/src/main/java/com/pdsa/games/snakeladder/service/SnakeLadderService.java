package com.pdsa.games.snakeladder.service;

import com.pdsa.games.snakeladder.model.AlgorithmResult;
import com.pdsa.games.snakeladder.model.SnakeLadderGame;
import com.pdsa.games.snakeladder.model.SnakeLadderItem;
import com.pdsa.games.snakeladder.model.SnakeLadderItem.PathType;
import com.pdsa.games.snakeladder.repository.SnakeLadderRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Central service for the Snake and Ladder game.
 *
 * Responsibilities:
 * 1. Generate a random board (snakes + ladders) for a given N
 * 2. Run both BFS and DP algorithms and return their results
 * 3. Build three answer choices for the player (one correct, two wrong)
 * 4. Check the player's answer and save it to the database
 * 5. Save all game data (board, algorithm timings, session summary) to DB
 */
@Service
public class SnakeLadderService {

    private final SnakeLadderRepository repository;
    private final BFSSnakeLadderService bfsService;
    private final DPSnakeLadderService dpService;
    private final Random random = new Random();

    public SnakeLadderService(SnakeLadderRepository repository,
            BFSSnakeLadderService bfsService,
            DPSnakeLadderService dpService) {
        this.repository = repository;
        this.bfsService = bfsService;
        this.dpService = dpService;
    }

    // -------------------------------------------------------------------------
    // Board generation
    // -------------------------------------------------------------------------

    /**
     * Generate a random board for board size N.
     * Produces (N-2) ladders and (N-2) snakes.
     *
     * Rules enforced:
     * - Cell 1 (start) and cell N² (end) are never used.
     * - No two snakes/ladders share a start cell.
     * - Ladders go upward (start < end).
     * - Snakes go downward (start > end).
     *
     * @param n         board dimension (6–12)
     * @param sessionId session ID to attach items to
     * @return list of SnakeLadderItem objects (not yet saved to DB)
     */
    public List<SnakeLadderItem> generateBoard(int n, int sessionId) {
        validateN(n);

        int totalCells = n * n;
        int count = n - 2; // number of snakes = number of ladders

        Set<Integer> usedStartCells = new HashSet<>();
        Set<Integer> usedCells = new HashSet<>();
        List<SnakeLadderItem> items = new ArrayList<>();

        // Reserve cell 1 and cell N² — never place a snake/ladder head there
        usedCells.add(1);
        usedCells.add(totalCells);

        // --- Generate ladders ---
        for (int i = 0; i < count; i++) {
            int start, end;
            do {
                // Ladder start: somewhere in the lower half (not last row)
                start = randomCell(2, totalCells - n - 1);
            } while (usedStartCells.contains(start) || usedCells.contains(start));

            do {
                // Ladder end: must be above start
                end = randomCell(start + 1, totalCells - 1);
            } while (usedCells.contains(end));

            usedStartCells.add(start);
            usedCells.add(start);
            usedCells.add(end);
            items.add(new SnakeLadderItem(sessionId, start, end, PathType.LADDER));
        }

        // --- Generate snakes ---
        for (int i = 0; i < count; i++) {
            int start, end;
            do {
                // Snake start (mouth): somewhere in the upper portion
                start = randomCell(n + 2, totalCells - 1);
            } while (usedStartCells.contains(start) || usedCells.contains(start));

            do {
                // Snake end (tail): must be below start
                end = randomCell(2, start - 1);
            } while (usedCells.contains(end));

            usedStartCells.add(start);
            usedCells.add(start);
            usedCells.add(end);
            items.add(new SnakeLadderItem(sessionId, start, end, PathType.SNAKE));
        }

        return items;
    }

    // -------------------------------------------------------------------------
    // Run algorithms
    // -------------------------------------------------------------------------

    /**
     * Build a portal map from a list of items (for algorithm input),
     * then run both BFS and DP and return their results.
     *
     * @param items      snakes and ladders on the board
     * @param totalCells N * N
     * @return list of two AlgorithmResult objects [BFS result, DP result]
     */
    public List<AlgorithmResult> runAlgorithms(List<SnakeLadderItem> items, int totalCells) {
        Map<Integer, Integer> portalMap = buildPortalMap(items);

        AlgorithmResult bfsResult = bfsService.solve(totalCells, portalMap);
        AlgorithmResult dpResult = dpService.solve(totalCells, portalMap);

        List<AlgorithmResult> results = new ArrayList<>();
        results.add(bfsResult);
        results.add(dpResult);
        return results;
    }

    // -------------------------------------------------------------------------
    // Answer choices
    // -------------------------------------------------------------------------

    /**
     * Build 3 answer choices: the correct answer + two plausible wrong ones.
     * Wrong answers are based on realistic mistake patterns from the board layout.
     * The list is shuffled so the correct answer isn't always first.
     *
     * @param correctAnswer the real minimum throws
     * @param items         the snakes and ladders on the board
     * @param n             board dimension
     * @return list of 3 distinct positive integers (shuffled)
     */
    public List<Integer> generateChoices(int correctAnswer, List<SnakeLadderItem> items, int n) {
        Set<Integer> choiceSet = new LinkedHashSet<>();
        choiceSet.add(correctAnswer);

        // Guaranteed off-by-one draw candidate
        int drawChoice = correctAnswer > 1 ? correctAnswer - 1 : correctAnswer + 1;
        if (drawChoice <= 0) {
            drawChoice = correctAnswer + 1;
        }
        choiceSet.add(drawChoice);

        // Build a realistic wrong answer using board composition
        long ladderCount = items.stream().filter(item -> item.getPathType() == PathType.LADDER).count();
        long snakeCount = items.stream().filter(item -> item.getPathType() == PathType.SNAKE).count();

        int wrongChoice;
        if (ladderCount >= snakeCount) {
            wrongChoice = correctAnswer + 2;
        } else {
            wrongChoice = correctAnswer - 2;
        }

        if (wrongChoice <= 0 || choiceSet.contains(wrongChoice)) {
            wrongChoice = correctAnswer + 2;
        }
        while (wrongChoice <= 0 || choiceSet.contains(wrongChoice)) {
            wrongChoice++;
        }
        choiceSet.add(wrongChoice);

        // Fallback: add nearby values until there are exactly 3 distinct positive
        // answers
        int offset = 2;
        while (choiceSet.size() < 3) {
            int candidate = correctAnswer + offset;
            if (candidate > 0) {
                choiceSet.add(candidate);
            }
            candidate = correctAnswer - offset;
            if (candidate > 0) {
                choiceSet.add(candidate);
            }
            offset++;
        }

        List<Integer> choices = new ArrayList<>(choiceSet);
        choices.removeIf(val -> val <= 0);
        while (choices.size() > 3) {
            choices.remove(choices.size() - 1);
        }

        Collections.shuffle(choices);
        return choices;
    }

    /**
     * Determine the outcome based on player answer vs correct answer.
     *
     * @param playerAnswer  the player's chosen answer
     * @param correctAnswer the correct minimum throws
     * @return "WIN", "DRAW", or "LOSE"
     */
    public String determineOutcome(int playerAnswer, int correctAnswer) {
        if (playerAnswer == correctAnswer) {
            return "WIN";
        } else if (Math.abs(playerAnswer - correctAnswer) == 1) {
            return "DRAW";
        } else {
            return "LOSE";
        }
    }

    // -------------------------------------------------------------------------
    // Check player answer & save to DB
    // -------------------------------------------------------------------------

    /**
     * Check whether the player's chosen answer is correct, then save it.
     *
     * @param sessionId     current session
     * @param playerAnswer  what the player chose
     * @param correctAnswer the real minimum throws
     * @return true if the player was correct
     */
    public boolean checkAndSaveAnswer(int sessionId, int playerAnswer, int correctAnswer) {
        boolean isCorrect = (playerAnswer == correctAnswer);
        try {
            repository.saveResponse(sessionId, String.valueOf(playerAnswer), isCorrect);
        } catch (Exception e) {
            // Log it but don't fail the player's game over a DB write issue
            System.err.println("Warning: failed to save response for session "
                    + sessionId + ": " + e.getMessage());
        }
        return isCorrect;
    }

    // -------------------------------------------------------------------------
    // Save full game session to DB
    // -------------------------------------------------------------------------

    /**
     * Persist the complete game: board items + algorithm timings + game summary.
     *
     * @param sessionId        current session ID (already created in Game_Session)
     * @param n                board size
     * @param items            snakes and ladders on the board
     * @param algorithmResults BFS and DP results
     * @param minimumThrows    agreed correct answer (both algorithms should agree)
     */

    public int saveGameSession(int playerId, int n, List<SnakeLadderItem> items,
            List<AlgorithmResult> algorithmResults,
            int minimumThrows) {

        // Step 0 — validate player exists (mirrors KnightsTourService pattern)
        if (!repository.playerExists(playerId)) {
            throw new IllegalArgumentException("Player does not exist.");
        }

        // Step 1 — create the Game_Session row and get back the real DB-generated ID
        int sessionId = repository.createGameSession(playerId);

        // Step 2 — save game summary (now safe, parent row exists)
        repository.saveGame(new SnakeLadderGame(sessionId, n, minimumThrows));

        // Step 3 — save board items, re-stamping with the real sessionId
        List<SnakeLadderItem> stampedItems = items.stream()
                .map(item -> new SnakeLadderItem(
                        sessionId,
                        item.getStartCell(),
                        item.getEndCell(),
                        item.getPathType()))
                .collect(java.util.stream.Collectors.toList());
        repository.saveItems(stampedItems);

        // Step 4 — save algorithm execution times
        for (AlgorithmResult result : algorithmResults) {
            int algorithmId = repository.findAlgorithmId(result.getAlgorithmName());
            if (algorithmId != -1) {
                repository.saveAlgorithmExecution(
                        sessionId, algorithmId,
                        result.getExecutionTimeMs(),
                        result.isSuccess());
            }
        }

        return sessionId; // return the real sessionId to the controller
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /**
     * Validate that N is within the allowed range (6–12).
     *
     * @throws IllegalArgumentException if N is out of range
     */
    public void validateN(int n) {
        if (n < 6 || n > 12) {
            throw new IllegalArgumentException(
                    "Board size N must be between 6 and 12. You entered: " + n);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Build a map of cell → destination for all snakes and ladders. */
    private Map<Integer, Integer> buildPortalMap(List<SnakeLadderItem> items) {
        Map<Integer, Integer> map = new HashMap<>();
        for (SnakeLadderItem item : items) {
            map.put(item.getStartCell(), item.getEndCell());
        }
        return map;
    }

    /** Return a random integer between min and max (inclusive). */
    private int randomCell(int min, int max) {
        if (min > max)
            return min; // safety guard
        return min + random.nextInt(max - min + 1);
    }
}