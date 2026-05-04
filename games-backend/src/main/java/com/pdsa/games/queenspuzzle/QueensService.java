package com.pdsa.games.queenspuzzle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QueensService {

	private static final Logger log = LoggerFactory.getLogger(QueensService.class);
	private static final int BOARD_SIZE_8 = 8;
	private static final int BOARD_SIZE_16 = 16;
	private static final int DEFAULT_BOARD_SIZE = 16;
	private static final int DEFAULT_QUEEN_COUNT = 8;

	private static final long MIN_TIME_BUDGET_MS = 500L;
	private static final long MAX_TIME_BUDGET_MS = 60_000L;

	private static final String GAME_NAME = "Sixteen Queens Puzzle";
	private static final String SEQUENTIAL_NAME = "Sixteen Queens - Sequential";
	private static final String THREADED_NAME = "Sixteen Queens - Threaded";

	private final QueensRepository repository;
	private final QueensJobRegistry jobRegistry;
	private final QueensSolver solver = new QueensSolver();
	private final ConcurrentMap<Long, Integer> sessionQueenCount = new ConcurrentHashMap<>();
	private final ExecutorService asyncExecutor = Executors.newCachedThreadPool(
			runnable -> {
				Thread t = new Thread(runnable, "queens-async-solve-" + System.nanoTime());
				t.setDaemon(true);
				t.setUncaughtExceptionHandler(
						(thread, ex) -> log.error("Uncaught exception in async solve thread {}", thread.getName(), ex));
				return t;
			});

	public QueensService(QueensRepository repository, QueensJobRegistry jobRegistry) {
		this.repository = repository;
		this.jobRegistry = jobRegistry;
	}

	@PreDestroy
	public void shutdownExecutor() {
		asyncExecutor.shutdownNow();
	}

	@Transactional
	public QueensModel.StartResponse startRound(QueensModel.StartRequest request, String authenticatedEmail) {
		Long playerId = resolveAuthenticatedPlayerId(authenticatedEmail);

		QueensModel.StartRequest payload = request == null
				? new QueensModel.StartRequest(null, null, null, null)
				: request;

		Integer boardSizeValue = payload.getBoardSize();
		Integer queenCountValue = payload.getQueenCount();
		int boardSize = boardSizeValue != null ? boardSizeValue : DEFAULT_BOARD_SIZE;
		int queenCount = queenCountValue != null ? queenCountValue : DEFAULT_QUEEN_COUNT;
		Long timeBudgetMs = normalizeBudget(payload.getTimeBudgetMs());
		boolean skipCache = Boolean.TRUE.equals(payload.getSkipCache());

		validateBoardConfig(boardSize, queenCount);

		Integer gameId = repository.ensureGameId(GAME_NAME);
		Integer sequentialAlgoId = repository.ensureAlgorithmId(gameId, SEQUENTIAL_NAME);
		Integer threadedAlgoId = repository.ensureAlgorithmId(gameId, THREADED_NAME);

		Long sessionId = repository.createGameSession(gameId, playerId);
		sessionQueenCount.put(sessionId, queenCount);

		boolean isAsyncEligible = timeBudgetMs == null
				&& boardSize == 16 && queenCount == 8
				&& !skipCache;

		if (isAsyncEligible) {
			QueensRepository.CachedCount cached = repository.findCachedCount(boardSize, queenCount);
			if (cached != null) {
				return respondFromCache(sessionId, boardSize, queenCount, cached,
						sequentialAlgoId, threadedAlgoId);
			}
			return startAsyncFullSolve(sessionId, boardSize, queenCount,
					sequentialAlgoId, threadedAlgoId);
		}

		return timeBudgetMs == null
				? runFullSolve(sessionId, boardSize, queenCount, sequentialAlgoId, threadedAlgoId, skipCache)
				: runTimedSolve(sessionId, boardSize, queenCount, timeBudgetMs,
						sequentialAlgoId, threadedAlgoId);
	}

	private QueensModel.StartResponse respondFromCache(Long sessionId, int boardSize, int queenCount,
			QueensRepository.CachedCount cached,
			Integer sequentialAlgoId, Integer threadedAlgoId) {
		int totalSolutions = cached.totalSolutions();
		double sequentialMs = cached.sequentialMs();
		double threadedMs = cached.threadedMs();

		repository.saveSixteenQueensGame(sessionId, totalSolutions);
		repository.saveAlgorithmExecution(sessionId, sequentialAlgoId, sequentialMs, "SUCCESS");
		repository.saveAlgorithmExecution(sessionId, threadedAlgoId, threadedMs, "SUCCESS");

		QueensModel.StartResponse response = new QueensModel.StartResponse();
		response.setSessionId(sessionId);
		response.setBoardSize(boardSize);
		response.setQueenCount(queenCount);
		response.setTotalSolutions(totalSolutions);
		response.setSequentialTimeMs(sequentialMs);
		response.setThreadedTimeMs(threadedMs);
		response.setSequentialFound(totalSolutions);
		response.setThreadedFound(totalSolutions);
		response.setSequentialHitLimit(false);
		response.setThreadedHitLimit(false);
		response.setTimeBudgetMs(null);
		response.setStatus("READY");
		response.setMessage("Cached full-solve result: " + totalSolutions + " solutions. Sequential "
				+ formatMs(sequentialMs) + " ms, Threaded " + formatMs(threadedMs) + " ms.");
		return response;
	}

	private QueensModel.StartResponse startAsyncFullSolve(Long sessionId, int boardSize, int queenCount,
			Integer sequentialAlgoId, Integer threadedAlgoId) {
		// Placeholder row so that submit/status calls have a Sixteen_Queens_Game row to
		// query.
		// Use Integer.MAX_VALUE as a "pending" sentinel so the all-found reset never
		// fires during the solve.
		repository.saveSixteenQueensGame(sessionId, Integer.MAX_VALUE);

		QueensJobRegistry.JobState job = jobRegistry.register(sessionId, boardSize, queenCount);

		log.info("Starting async 8q no-cap job for session {}", sessionId);
		asyncExecutor.execute(() -> runAsyncSequential(job, sequentialAlgoId));
		asyncExecutor.execute(() -> runAsyncThreaded(job, threadedAlgoId));

		QueensModel.StartResponse response = new QueensModel.StartResponse();
		response.setSessionId(sessionId);
		response.setBoardSize(boardSize);
		response.setQueenCount(queenCount);
		response.setTotalSolutions(0);
		response.setSequentialTimeMs(0.0);
		response.setThreadedTimeMs(0.0);
		response.setSequentialFound(0);
		response.setThreadedFound(0);
		response.setSequentialHitLimit(false);
		response.setThreadedHitLimit(false);
		response.setTimeBudgetMs(null);
		response.setStatus("RUNNING");
		response.setMessage("Async full-solve started. Poll /sixteen-queens/" + sessionId
				+ "/progress for live counts.");
		return response;
	}

	private void runAsyncSequential(QueensJobRegistry.JobState job, Integer sequentialAlgoId) {
		log.info("Async sequential task entered for session {}", job.sessionId);
		long startNs = System.nanoTime();
		try {
			long count = solver.solveSequentialCountWithProgress(
					job.boardSize, job.queenCount,
					job.sequentialFound, job.cancelled);
			long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
			job.sequentialElapsedMs.set(elapsedMs);
			job.sequentialDone.set(true);
			log.info("Async sequential done: session {}, count {}, elapsedMs {}",
					job.sessionId, count, elapsedMs);
			repository.saveAlgorithmExecution(job.sessionId, sequentialAlgoId,
					(double) elapsedMs, job.cancelled.get() ? "FAILURE" : "SUCCESS");
			tryFinishJob(job, count);
		} catch (Throwable ex) {
			log.error("Async sequential failed for session {}", job.sessionId, ex);
			job.sequentialDone.set(true);
			jobRegistry.markFailed(job.sessionId, ex.getMessage());
		}
	}

	private void runAsyncThreaded(QueensJobRegistry.JobState job, Integer threadedAlgoId) {
		log.info("Async threaded task entered for session {}", job.sessionId);
		long startNs = System.nanoTime();
		try {
			long count = solver.solveThreadedCountWithProgress(
					job.boardSize, job.queenCount,
					job.threadedFound, job.cancelled);
			long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
			job.threadedElapsedMs.set(elapsedMs);
			job.threadedDone.set(true);
			log.info("Async threaded done: session {}, count {}, elapsedMs {}",
					job.sessionId, count, elapsedMs);
			repository.saveAlgorithmExecution(job.sessionId, threadedAlgoId,
					(double) elapsedMs, job.cancelled.get() ? "FAILURE" : "SUCCESS");
			tryFinishJob(job, count);
		} catch (Throwable ex) {
			log.error("Async threaded failed for session {}", job.sessionId, ex);
			job.threadedDone.set(true);
			jobRegistry.markFailed(job.sessionId, ex.getMessage());
		}
	}

	/**
	 * When both algorithms have finished, persist the shared final total and cache
	 * it.
	 * Uses the threaded count (canonical) if both completed without cancel; else
	 * skip caching.
	 */
	private void tryFinishJob(QueensJobRegistry.JobState job, long myCount) {
		if (!(job.sequentialDone.get() && job.threadedDone.get()))
			return;
		if (job.status != QueensJobRegistry.Status.RUNNING)
			return; // already finalised (cancelled/failed)
		if (job.cancelled.get())
			return;

		long seqTotal = job.sequentialFound.sum();
		long thrTotal = job.threadedFound.sum();
		if (seqTotal != thrTotal) {
			jobRegistry.markFailed(job.sessionId,
					"Sequential and threaded solvers disagree: " + seqTotal + " vs " + thrTotal);
			return;
		}

		int totalInt = seqTotal > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seqTotal;
		try {
			repository.insertCachedCount(job.boardSize, job.queenCount, totalInt,
					(double) job.sequentialElapsedMs.get(),
					(double) job.threadedElapsedMs.get());
			repository.updateTotalSolutions(job.sessionId, totalInt);
			jobRegistry.markCompleted(job.sessionId, seqTotal);
		} catch (RuntimeException ex) {
			jobRegistry.markFailed(job.sessionId, "Persistence error: " + ex.getMessage());
		}
	}

	public QueensModel.ProgressResponse getProgress(Long sessionId, String authenticatedEmail) {
		if (sessionId == null || sessionId <= 0) {
			throw new IllegalArgumentException("Valid sessionId is required.");
		}
		Long playerId = resolveAuthenticatedPlayerId(authenticatedEmail);
		if (!repository.sessionBelongsToPlayer(sessionId, playerId)) {
			throw new IllegalArgumentException("This session does not belong to the authenticated player.");
		}

		QueensJobRegistry.Snapshot snap = jobRegistry.snapshot(sessionId);
		if (snap == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					"No async job is tracked for this session (synchronous completion or past the retention window).");
		}

		QueensModel.ProgressResponse response = new QueensModel.ProgressResponse();
		response.setSessionId(snap.sessionId);
		response.setBoardSize(snap.boardSize);
		response.setQueenCount(snap.queenCount);
		response.setSequentialFound(snap.sequentialFound);
		response.setThreadedFound(snap.threadedFound);
		response.setSequentialTimeMs(snap.sequentialElapsedMs);
		response.setThreadedTimeMs(snap.threadedElapsedMs);
		response.setSequentialDone(snap.sequentialDone);
		response.setThreadedDone(snap.threadedDone);
		response.setStatus(snap.status.name());
		response.setTotalSolutions(snap.totalSolutions);
		response.setMessage(snap.errorMessage);
		return response;
	}

	public boolean cancelJob(Long sessionId, String authenticatedEmail) {
		if (sessionId == null || sessionId <= 0) {
			throw new IllegalArgumentException("Valid sessionId is required.");
		}
		Long playerId = resolveAuthenticatedPlayerId(authenticatedEmail);
		if (!repository.sessionBelongsToPlayer(sessionId, playerId)) {
			throw new IllegalArgumentException("This session does not belong to the authenticated player.");
		}
		return jobRegistry.cancel(sessionId);
	}

	private QueensModel.StartResponse runFullSolve(Long sessionId, int boardSize, int queenCount,
			Integer sequentialAlgoId, Integer threadedAlgoId,
			boolean skipCache) {
		QueensRepository.CachedCount cached = skipCache
				? null
				: repository.findCachedCount(boardSize, queenCount);

		int totalSolutions;
		double sequentialMs;
		double threadedMs;
		boolean fromCache = cached != null;

		if (fromCache) {
			totalSolutions = cached.totalSolutions();
			sequentialMs = cached.sequentialMs();
			threadedMs = cached.threadedMs();
		} else {
			long seqStart = System.nanoTime();
			int seqCount = solver.solveSequentialCount(boardSize, queenCount);
			sequentialMs = (System.nanoTime() - seqStart) / 1_000_000.0;

			long thStart = System.nanoTime();
			int thCount = solver.solveThreadedCount(boardSize, queenCount);
			threadedMs = (System.nanoTime() - thStart) / 1_000_000.0;

			if (seqCount != thCount) {
				throw new IllegalStateException(
						"Sequential and threaded solvers disagree: " + seqCount + " vs " + thCount);
			}
			totalSolutions = seqCount;
			repository.insertCachedCount(boardSize, queenCount, totalSolutions, sequentialMs, threadedMs);
		}

		repository.saveSixteenQueensGame(sessionId, totalSolutions);
		repository.saveAlgorithmExecution(sessionId, sequentialAlgoId, sequentialMs, "SUCCESS");
		repository.saveAlgorithmExecution(sessionId, threadedAlgoId, threadedMs, "SUCCESS");

		QueensModel.StartResponse response = new QueensModel.StartResponse();
		response.setSessionId(sessionId);
		response.setBoardSize(boardSize);
		response.setQueenCount(queenCount);
		response.setTotalSolutions(totalSolutions);
		response.setSequentialTimeMs(sequentialMs);
		response.setThreadedTimeMs(threadedMs);
		response.setSequentialFound(totalSolutions);
		response.setThreadedFound(totalSolutions);
		response.setSequentialHitLimit(false);
		response.setThreadedHitLimit(false);
		response.setTimeBudgetMs(null);
		response.setStatus("READY");
		response.setMessage(fromCache
				? "Cached full-solve result: " + totalSolutions + " solutions. Sequential "
						+ formatMs(sequentialMs) + " ms, Threaded " + formatMs(threadedMs) + " ms."
				: "Full solve complete: " + totalSolutions + " solutions. Sequential "
						+ formatMs(sequentialMs) + " ms, Threaded " + formatMs(threadedMs) + " ms.");
		return response;
	}

	private QueensModel.StartResponse runTimedSolve(Long sessionId, int boardSize, int queenCount,
			long timeBudgetMs,
			Integer sequentialAlgoId, Integer threadedAlgoId) {
		QueensSolver.TimedResult sequentialResult = solver.solveSequentialTimed(
				boardSize, queenCount, timeBudgetMs, QueensSolver.DEFAULT_MAX_STORED);
		QueensSolver.TimedResult threadedResult = solver.solveThreadedTimed(
				boardSize, queenCount, timeBudgetMs, QueensSolver.DEFAULT_MAX_STORED);

		QueensRepository.CachedCount cached = repository.findCachedCount(boardSize, queenCount);
		int totalSolutions = cached != null
				? cached.totalSolutions()
				: Math.max(sequentialResult.totalFound, threadedResult.totalFound);

		repository.saveSixteenQueensGame(sessionId, totalSolutions);
		repository.saveAlgorithmExecution(sessionId, sequentialAlgoId, sequentialResult.elapsedMs,
				sequentialResult.hitTimeLimit ? "TIMEOUT" : "SUCCESS");
		repository.saveAlgorithmExecution(sessionId, threadedAlgoId, threadedResult.elapsedMs,
				threadedResult.hitTimeLimit ? "TIMEOUT" : "SUCCESS");

		QueensModel.StartResponse response = new QueensModel.StartResponse();
		response.setSessionId(sessionId);
		response.setBoardSize(boardSize);
		response.setQueenCount(queenCount);
		response.setTotalSolutions(totalSolutions);
		response.setSequentialTimeMs((double) sequentialResult.elapsedMs);
		response.setThreadedTimeMs((double) threadedResult.elapsedMs);
		response.setSequentialFound(sequentialResult.totalFound);
		response.setThreadedFound(threadedResult.totalFound);
		response.setSequentialHitLimit(sequentialResult.hitTimeLimit);
		response.setThreadedHitLimit(threadedResult.hitTimeLimit);
		response.setTimeBudgetMs(timeBudgetMs);
		response.setStatus("READY");
		response.setMessage(buildTimedMessage(sequentialResult, threadedResult, timeBudgetMs));
		return response;
	}

	@Transactional
	public QueensModel.SubmitResponse submitAnswer(QueensModel.SubmitRequest request, String authenticatedEmail) {
		if (request == null) {
			throw new IllegalArgumentException("Request payload is required.");
		}

		if (request.getSessionId() == null || request.getSessionId() <= 0) {
			throw new IllegalArgumentException("Valid sessionId is required.");
		}

		Long playerId = resolveAuthenticatedPlayerId(authenticatedEmail);

		if (!repository.sessionBelongsToPlayer(request.getSessionId(), playerId)) {
			throw new IllegalArgumentException("This session does not belong to the authenticated player.");
		}

		Integer totalSolutions = repository.getTotalSolutions(request.getSessionId());
		Integer discovered = repository.getDiscoveredCount(request.getSessionId());
		if (totalSolutions == null || discovered == null) {
			throw new IllegalStateException("Sixteen Queens game configuration not found for this session.");
		}

		List<QueensModel.Position> positions = normalizePositions(request.getQueens());

		Integer storedQueenCount = sessionQueenCount.get(request.getSessionId());
		int expectedQueenCount = storedQueenCount != null ? storedQueenCount : DEFAULT_QUEEN_COUNT;

		// Get session's actual board size from job registry, fallback to DEFAULT_BOARD_SIZE if not found
		QueensJobRegistry.Snapshot snap = jobRegistry.snapshot(request.getSessionId());
		int sessionBoardSize = (snap != null) ? snap.boardSize : DEFAULT_BOARD_SIZE;

		try {
			validatePositions(positions, sessionBoardSize, expectedQueenCount);
		} catch (IllegalArgumentException ex) {
			String responseSummary = buildResponseSummary(playerId, "INCORRECT");
			repository.saveResponse(request.getSessionId(), responseSummary, false);

			return new QueensModel.SubmitResponse(
					request.getSessionId(),
					playerId,
					false,
					false,
					discovered,
					totalSolutions,
					"Incorrect solution: " + ex.getMessage());
		}

		Integer existingSolutionId = repository.findMatchingSolutionId(request.getSessionId(), positions);
		int updatedDiscovered;
		String message;
		String responseSummary;

		if (existingSolutionId != null) {
			if (repository.isSolutionDiscovered(request.getSessionId(), existingSolutionId)) {
				return new QueensModel.SubmitResponse(
						request.getSessionId(),
						playerId,
						true,
						true,
						discovered,
						totalSolutions,
						"This solution was already discovered. Try a different one.");
			}
			repository.markSolutionDiscovered(request.getSessionId(), existingSolutionId);
			updatedDiscovered = repository.getDiscoveredCount(request.getSessionId());
			responseSummary = buildResponseSummary(playerId, "SOLUTION_" + existingSolutionId);
			message = "Correct solution recorded.";
		} else {
			int newSolutionId = repository.insertDiscoveredSolution(request.getSessionId(), positions);
			updatedDiscovered = repository.getDiscoveredCount(request.getSessionId());
			responseSummary = buildResponseSummary(playerId, "SOLUTION_" + newSolutionId);
			message = "Correct solution recorded.";
		}

		repository.saveResponse(request.getSessionId(), responseSummary, true);

		if (updatedDiscovered >= totalSolutions) {
			repository.resetDiscovered(request.getSessionId());
			updatedDiscovered = 0;
			message = "All solutions discovered. Progress has been reset for future players.";
		}

		return new QueensModel.SubmitResponse(
				request.getSessionId(),
				playerId,
				true,
				false,
				updatedDiscovered,
				totalSolutions,
				message);
	}

	@Transactional(readOnly = true)
	public QueensModel.StatusResponse getStatus(Long sessionId, String authenticatedEmail) {
		if (sessionId == null || sessionId <= 0) {
			throw new IllegalArgumentException("Valid sessionId is required.");
		}

		Long playerId = resolveAuthenticatedPlayerId(authenticatedEmail);

		if (!repository.sessionBelongsToPlayer(sessionId, playerId)) {
			throw new IllegalArgumentException("This session does not belong to the authenticated player.");
		}

		Integer totalSolutions = repository.getTotalSolutions(sessionId);
		Integer discovered = repository.getDiscoveredCount(sessionId);
		if (totalSolutions == null || discovered == null) {
			throw new IllegalStateException("Sixteen Queens game configuration not found for this session.");
		}

		QueensModel.StatusResponse response = new QueensModel.StatusResponse();
		response.setSessionId(sessionId);
		response.setTotalSolutions(totalSolutions);
		response.setSolutionsDiscovered(discovered);
		response.setMessage("Sixteen Queens progress loaded.");
		return response;
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

	private Long normalizeBudget(Long requestedBudget) {
		if (requestedBudget == null) {
			return null;
		}
		long value = requestedBudget;
		if (value < MIN_TIME_BUDGET_MS) {
			return MIN_TIME_BUDGET_MS;
		}
		if (value > MAX_TIME_BUDGET_MS) {
			return MAX_TIME_BUDGET_MS;
		}
		return value;
	}

	private void validateBoardConfig(int boardSize, int queenCount) {
		if (boardSize != BOARD_SIZE_8 && boardSize != BOARD_SIZE_16) {
			throw new IllegalArgumentException("Board size must be either " + BOARD_SIZE_8 + " or " + BOARD_SIZE_16 + ".");
		}
		if (queenCount < 1 || queenCount > boardSize) {
			throw new IllegalArgumentException("Queen count must be between 1 and the board size.");
		}
	}

	private List<QueensModel.Position> normalizePositions(List<QueensModel.Position> positions) {
		if (positions == null) {
			throw new IllegalArgumentException("Queens positions are required.");
		}
		return positions;
	}

	private void validatePositions(List<QueensModel.Position> positions, int boardSize, int queenCount) {
		if (positions.size() != queenCount) {
			throw new IllegalArgumentException("Exactly " + queenCount + " queens are required.");
		}

		Set<String> occupied = new HashSet<>();
		Set<Integer> rows = new HashSet<>();
		Set<Integer> cols = new HashSet<>();
		Set<Integer> diag1 = new HashSet<>();
		Set<Integer> diag2 = new HashSet<>();

		for (QueensModel.Position position : positions) {
			if (position == null) {
				throw new IllegalArgumentException("Queen position entries must not be null.");
			}
			int x = position.getX();
			int y = position.getY();
			if (x < 1 || x > boardSize || y < 1 || y > boardSize) {
				throw new IllegalArgumentException("Queen positions must be within the board range.");
			}
			String key = x + ":" + y;
			if (!occupied.add(key)) {
				throw new IllegalArgumentException("Duplicate queen positions are not allowed.");
			}
			if (!rows.add(y) || !cols.add(x)) {
				throw new IllegalArgumentException("Queens must not share a row or column.");
			}
			int d1 = y - x;
			int d2 = y + x;
			if (!diag1.add(d1) || !diag2.add(d2)) {
				throw new IllegalArgumentException("Queens must not share a diagonal.");
			}
		}
	}

	private String buildTimedMessage(QueensSolver.TimedResult sequential,
			QueensSolver.TimedResult threaded,
			long timeBudgetMs) {
		String suffix = (sequential.hitTimeLimit || threaded.hitTimeLimit)
				? " (time-bounded — search hit the budget; partial counts shown)"
				: " (full search completed within budget)";
		return "Sequential found " + sequential.totalFound
				+ "; Threaded found " + threaded.totalFound
				+ " in " + timeBudgetMs + " ms each" + suffix;
	}

	private String formatMs(double ms) {
		return String.format("%.2f", ms);
	}

	private String buildResponseSummary(Long playerId, String responseKey) {
		String playerName = repository.findPlayerName(playerId);
		String safeName = playerName == null ? "Player" : playerName;
		String summary = safeName + ":" + responseKey;
		if (summary.length() > 50) {
			return summary.substring(0, 50);
		}
		return summary;
	}
}
