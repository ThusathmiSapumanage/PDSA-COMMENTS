package com.pdsa.games.queenspuzzle;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueensServiceTest {

    private static final String EMAIL = "rajitha@test.com";

    private QueensRepository repository;
    private QueensService service;

    @BeforeEach
    void setUp() {
        repository = mock(QueensRepository.class);
        service = new QueensService(repository, new QueensJobRegistry());
    }

    private void stubAuthenticatedPlayer() {
        when(repository.findPlayerIdByEmail(EMAIL)).thenReturn(1L);
        when(repository.playerExists(1L)).thenReturn(true);
    }

    private QueensModel.StartRequest startRequest(Integer boardSize, Integer queenCount) {
        return new QueensModel.StartRequest(boardSize, queenCount, 1_000L, null);
    }

    // ================= START ROUND =================

    @Test
    void startRound_timedMode_shouldCreateRoundSuccessfully() {
        stubAuthenticatedPlayer();
        when(repository.ensureGameId(anyString())).thenReturn(10);
        when(repository.ensureAlgorithmId(eq(10), anyString())).thenReturn(20, 21);
        when(repository.createGameSession(10, 1L)).thenReturn(100L);
        when(repository.findCachedCount(8, 8)).thenReturn(null);

        QueensModel.StartResponse response = service.startRound(startRequest(8, 8), EMAIL);

        assertNotNull(response);
        assertEquals(100L, response.getSessionId());
        assertEquals(8, response.getBoardSize());
        assertEquals(8, response.getQueenCount());
        // 8-queens has 92 solutions; both algos should find them within the budget.
        assertEquals(92, response.getTotalSolutions());
        assertEquals(92, response.getSequentialFound());
        assertEquals(92, response.getThreadedFound());
        assertFalse(response.getSequentialHitLimit());
        assertFalse(response.getThreadedHitLimit());

        verify(repository).saveSixteenQueensGame(100L, 92);
        verify(repository, times(2)).saveAlgorithmExecution(eq(100L), anyInt(), anyDouble(), anyString());
        verify(repository, never()).saveSolutions(anyLong(), any());
        verify(repository, never()).insertCachedCount(anyInt(), anyInt(), anyInt(), anyDouble(), anyDouble());
    }

    @Test
    void startRound_noCapMode_shouldComputeAndCacheResult() {
        stubAuthenticatedPlayer();
        when(repository.ensureGameId(anyString())).thenReturn(10);
        when(repository.ensureAlgorithmId(eq(10), anyString())).thenReturn(20, 21);
        when(repository.createGameSession(10, 1L)).thenReturn(200L);
        when(repository.findCachedCount(8, 8)).thenReturn(null);

        QueensModel.StartResponse response = service.startRound(
            new QueensModel.StartRequest(8, 8, null, null), EMAIL);

        assertEquals(92, response.getTotalSolutions());
        assertEquals(92, response.getSequentialFound());
        assertEquals(92, response.getThreadedFound());
        assertFalse(response.getSequentialHitLimit());
        assertFalse(response.getThreadedHitLimit());
        assertEquals(null, response.getTimeBudgetMs());

        verify(repository).saveSixteenQueensGame(200L, 92);
        verify(repository).insertCachedCount(eq(8), eq(8), eq(92), anyDouble(), anyDouble());
        verify(repository, never()).saveSolutions(anyLong(), any());
    }

    @Test
    void startRound_noCapMode_shouldSkipCacheRead_whenSkipCacheFlagSet() {
        stubAuthenticatedPlayer();
        when(repository.ensureGameId(anyString())).thenReturn(10);
        when(repository.ensureAlgorithmId(eq(10), anyString())).thenReturn(20, 21);
        when(repository.createGameSession(10, 1L)).thenReturn(250L);

        QueensModel.StartResponse response = service.startRound(
                new QueensModel.StartRequest(8, 8, null, true), EMAIL);

        assertEquals(92, response.getTotalSolutions());
        verify(repository, never()).findCachedCount(anyInt(), anyInt());
        verify(repository).insertCachedCount(eq(8), eq(8), eq(92), anyDouble(), anyDouble());
    }

    @Test
    void startRound_noCapMode_shouldReadFromCache_whenPresent() {
        stubAuthenticatedPlayer();
        when(repository.ensureGameId(anyString())).thenReturn(10);
        when(repository.ensureAlgorithmId(eq(10), anyString())).thenReturn(20, 21);
        when(repository.createGameSession(10, 1L)).thenReturn(300L);
        when(repository.findCachedCount(16, 16)).thenReturn(
                new QueensRepository.CachedCount(14_772_512, 112_600.0, 18_800.0));

        QueensModel.StartResponse response = service.startRound(
                new QueensModel.StartRequest(16, 16, null, null), EMAIL);

        assertEquals(14_772_512, response.getTotalSolutions());
        assertEquals(112_600.0, response.getSequentialTimeMs(), 0.001);
        assertEquals(18_800.0, response.getThreadedTimeMs(), 0.001);
        verify(repository, never()).insertCachedCount(anyInt(), anyInt(), anyInt(), anyDouble(), anyDouble());
        verify(repository).saveSixteenQueensGame(300L, 14_772_512);
    }

    @Test
    void startRound_shouldThrow_whenEmailIsBlank() {
        assertThrows(ResponseStatusException.class,
                () -> service.startRound(startRequest(8, 8), "   "));
    }

    @Test
    void startRound_shouldThrow_whenPlayerDoesNotExist() {
        when(repository.findPlayerIdByEmail(EMAIL)).thenReturn(null);

        assertThrows(ResponseStatusException.class,
                () -> service.startRound(startRequest(8, 8), EMAIL));
    }

    @Test
    void startRound_shouldThrow_whenBoardSizeBelowMinimum() {
        stubAuthenticatedPlayer();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.startRound(startRequest(3, 3), EMAIL));
        assertTrue(ex.getMessage().contains("Board size"));
    }

    @Test
    void startRound_shouldThrow_whenQueenCountExceedsBoardSize() {
        stubAuthenticatedPlayer();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.startRound(startRequest(8, 9), EMAIL));
        assertTrue(ex.getMessage().contains("Queen count"));
    }

    // ================= SUBMIT ANSWER =================

    @Test
    void submitAnswer_shouldRecordCorrectSolution_whenNoMatchInAnswerKey() {
        stubAuthenticatedPlayer();
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(1_000_000);
        when(repository.getDiscoveredCount(100L)).thenReturn(0, 1);
        when(repository.findMatchingSolutionId(eq(100L), any())).thenReturn(null);
        when(repository.insertDiscoveredSolution(eq(100L), any())).thenReturn(1);
        when(repository.findPlayerName(1L)).thenReturn("Rajitha");

        primeSession(100L, 8);

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, validSolution());
        QueensModel.SubmitResponse response = service.submitAnswer(request, EMAIL);

        assertTrue(response.getCorrect());
        assertFalse(response.getAlreadyDiscovered());
        assertEquals(1, response.getSolutionsDiscovered());
        verify(repository).insertDiscoveredSolution(eq(100L), any());
        verify(repository, never()).markSolutionDiscovered(anyLong(), anyInt());
        verify(repository).saveResponse(eq(100L), anyString(), eq(true));
    }

    @Test
    void submitAnswer_shouldMarkSolutionDiscovered_whenMatchInAnswerKey() {
        stubAuthenticatedPlayer();
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(1_000_000);
        when(repository.getDiscoveredCount(100L)).thenReturn(0, 1);
        when(repository.findMatchingSolutionId(eq(100L), any())).thenReturn(7);
        when(repository.isSolutionDiscovered(100L, 7)).thenReturn(false);
        when(repository.findPlayerName(1L)).thenReturn("Rajitha");

        primeSession(100L, 8);

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, validSolution());
        QueensModel.SubmitResponse response = service.submitAnswer(request, EMAIL);

        assertTrue(response.getCorrect());
        assertFalse(response.getAlreadyDiscovered());
        assertEquals(1, response.getSolutionsDiscovered());
        verify(repository).markSolutionDiscovered(100L, 7);
        verify(repository, never()).insertDiscoveredSolution(anyLong(), any());
    }

    @Test
    void submitAnswer_shouldFlagAlreadyDiscovered() {
        stubAuthenticatedPlayer();
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(1_000_000);
        when(repository.getDiscoveredCount(100L)).thenReturn(1);
        when(repository.findMatchingSolutionId(eq(100L), any())).thenReturn(7);
        when(repository.isSolutionDiscovered(100L, 7)).thenReturn(true);

        primeSession(100L, 8);

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, validSolution());
        QueensModel.SubmitResponse response = service.submitAnswer(request, EMAIL);

        assertTrue(response.getCorrect());
        assertTrue(response.getAlreadyDiscovered());
        verify(repository, never()).insertDiscoveredSolution(anyLong(), any());
        verify(repository, never()).markSolutionDiscovered(anyLong(), anyInt());
    }

    @Test
    void submitAnswer_shouldReturnIncorrect_whenStructureInvalid() {
        stubAuthenticatedPlayer();
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(1_000_000);
        when(repository.getDiscoveredCount(100L)).thenReturn(0);
        when(repository.findPlayerName(1L)).thenReturn("Rajitha");

        primeSession(100L, 8);

        List<QueensModel.Position> conflicting = new ArrayList<>(validSolution());
        conflicting.set(0, new QueensModel.Position(1, 2));

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, conflicting);
        QueensModel.SubmitResponse response = service.submitAnswer(request, EMAIL);

        assertFalse(response.getCorrect());
        assertFalse(response.getAlreadyDiscovered());
        assertTrue(response.getMessage().startsWith("Incorrect solution"));
        verify(repository).saveResponse(eq(100L), anyString(), eq(false));
        verify(repository, never()).insertDiscoveredSolution(anyLong(), any());
    }

    @Test
    void submitAnswer_shouldResetDiscovered_whenAllSolutionsFound() {
        stubAuthenticatedPlayer();
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(1);
        when(repository.getDiscoveredCount(100L)).thenReturn(0, 1);
        when(repository.findMatchingSolutionId(eq(100L), any())).thenReturn(null);
        when(repository.insertDiscoveredSolution(eq(100L), any())).thenReturn(1);
        when(repository.findPlayerName(1L)).thenReturn("Rajitha");

        primeSession(100L, 8);

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, validSolution());
        QueensModel.SubmitResponse response = service.submitAnswer(request, EMAIL);

        assertTrue(response.getCorrect());
        assertEquals(0, response.getSolutionsDiscovered());
        assertTrue(response.getMessage().contains("All solutions discovered"));
        verify(repository).resetDiscovered(100L);
    }

    @Test
    void submitAnswer_shouldThrow_whenSessionDoesNotBelongToPlayer() {
        stubAuthenticatedPlayer();
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(false);

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, validSolution());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.submitAnswer(request, EMAIL));
        assertTrue(ex.getMessage().contains("session does not belong"));
    }

    @Test
    void submitAnswer_shouldThrow_whenGameConfigMissing() {
        stubAuthenticatedPlayer();
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(null);

        QueensModel.SubmitRequest request = new QueensModel.SubmitRequest(100L, validSolution());
        assertThrows(IllegalStateException.class, () -> service.submitAnswer(request, EMAIL));
    }

    // ================= STATUS =================

    @Test
    void getStatus_shouldReturnProgress() {
        stubAuthenticatedPlayer();
        when(repository.sessionBelongsToPlayer(100L, 1L)).thenReturn(true);
        when(repository.getTotalSolutions(100L)).thenReturn(1_000_000);
        when(repository.getDiscoveredCount(100L)).thenReturn(42);

        QueensModel.StatusResponse response = service.getStatus(100L, EMAIL);

        assertEquals(100L, response.getSessionId());
        assertEquals(1_000_000, response.getTotalSolutions());
        assertEquals(42, response.getSolutionsDiscovered());
    }

    @Test
    void getStatus_shouldThrow_whenSessionIdInvalid() {
        assertThrows(IllegalArgumentException.class, () -> service.getStatus(0L, EMAIL));
    }

    // Prime the in-memory queenCount cache by running a tiny startRound — needed so submitAnswer
    // knows how many queens to expect on this session.
    private void primeSession(long sessionId, int queenCount) {
        stubAuthenticatedPlayer();
        when(repository.ensureGameId(anyString())).thenReturn(10);
        when(repository.ensureAlgorithmId(eq(10), anyString())).thenReturn(20, 21);
        when(repository.createGameSession(10, 1L)).thenReturn(sessionId);
        service.startRound(new QueensModel.StartRequest(16, queenCount, 500L, null), EMAIL);
    }

    // 8 queens on 16x16, no-threat placement.
    // Row 1..8, cols 1,3,5,7,9,11,13,15 — all rows/cols/diagonals unique.
    private List<QueensModel.Position> validSolution() {
        int[] cols = {1, 3, 5, 7, 9, 11, 13, 15};
        List<QueensModel.Position> positions = new ArrayList<>();
        for (int row = 0; row < cols.length; row++) {
            positions.add(new QueensModel.Position(cols[row], row + 1));
        }
        return positions;
    }
}
