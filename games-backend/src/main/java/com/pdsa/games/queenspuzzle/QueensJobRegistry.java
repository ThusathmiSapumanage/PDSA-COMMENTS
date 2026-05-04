package com.pdsa.games.queenspuzzle;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.stereotype.Component;

/**
 * In-memory progress tracker for async Sixteen Queens full-solve jobs.
 * Keyed by {@code sessionId}. Only used for the 8-queens no-cap path — other modes stay synchronous.
 */
@Component
public class QueensJobRegistry {

	/** Jobs linger this long after completion so late pollers can still see the final state. */
	private static final long GRACE_PERIOD_MS = 60_000L;

	public enum Status {
		RUNNING,
		COMPLETED,
		CANCELLED,
		FAILED
	}

	public static final class Snapshot {
		public final long sessionId;
		public final int boardSize;
		public final int queenCount;
		public final long sequentialFound;
		public final long threadedFound;
		public final long sequentialElapsedMs;
		public final long threadedElapsedMs;
		public final boolean sequentialDone;
		public final boolean threadedDone;
		public final Status status;
		public final Long totalSolutions;
		public final String errorMessage;

		Snapshot(long sessionId, int boardSize, int queenCount,
				 long sequentialFound, long threadedFound,
				 long sequentialElapsedMs, long threadedElapsedMs,
				 boolean sequentialDone, boolean threadedDone,
				 Status status, Long totalSolutions, String errorMessage) {
			this.sessionId = sessionId;
			this.boardSize = boardSize;
			this.queenCount = queenCount;
			this.sequentialFound = sequentialFound;
			this.threadedFound = threadedFound;
			this.sequentialElapsedMs = sequentialElapsedMs;
			this.threadedElapsedMs = threadedElapsedMs;
			this.sequentialDone = sequentialDone;
			this.threadedDone = threadedDone;
			this.status = status;
			this.totalSolutions = totalSolutions;
			this.errorMessage = errorMessage;
		}
	}

	/** Live state for one job. Fields mutated directly by solver threads + completion callbacks. */
	static final class JobState {
		final long sessionId;
		final int boardSize;
		final int queenCount;
		final long startNanos;

		final LongAdder sequentialFound = new LongAdder();
		final LongAdder threadedFound = new LongAdder();
		final AtomicLong sequentialElapsedMs = new AtomicLong(0);
		final AtomicLong threadedElapsedMs = new AtomicLong(0);
		final AtomicBoolean sequentialDone = new AtomicBoolean(false);
		final AtomicBoolean threadedDone = new AtomicBoolean(false);

		final AtomicBoolean cancelled = new AtomicBoolean(false);
		volatile Status status = Status.RUNNING;
		volatile Long totalSolutions = null;
		volatile String errorMessage = null;
		volatile long completedAtMs = 0L;

		JobState(long sessionId, int boardSize, int queenCount) {
			this.sessionId = sessionId;
			this.boardSize = boardSize;
			this.queenCount = queenCount;
			this.startNanos = System.nanoTime();
		}
	}

	private final ConcurrentMap<Long, JobState> jobs = new ConcurrentHashMap<>();

	public JobState register(long sessionId, int boardSize, int queenCount) {
		JobState state = new JobState(sessionId, boardSize, queenCount);
		jobs.put(sessionId, state);
		return state;
	}

	public JobState get(long sessionId) {
		sweepExpired();
		return jobs.get(sessionId);
	}

	public Snapshot snapshot(long sessionId) {
		JobState s = get(sessionId);
		if (s == null) return null;
		return new Snapshot(
				s.sessionId, s.boardSize, s.queenCount,
				s.sequentialFound.sum(), s.threadedFound.sum(),
				s.sequentialElapsedMs.get(), s.threadedElapsedMs.get(),
				s.sequentialDone.get(), s.threadedDone.get(),
				s.status, s.totalSolutions, s.errorMessage
		);
	}

	public boolean cancel(long sessionId) {
		JobState s = jobs.get(sessionId);
		if (s == null) return false;
		s.cancelled.set(true);
		if (s.status == Status.RUNNING) {
			s.status = Status.CANCELLED;
			s.completedAtMs = System.currentTimeMillis();
		}
		return true;
	}

	public void markCompleted(long sessionId, long totalSolutions) {
		JobState s = jobs.get(sessionId);
		if (s == null) return;
		s.totalSolutions = totalSolutions;
		s.status = Status.COMPLETED;
		s.completedAtMs = System.currentTimeMillis();
	}

	public void markFailed(long sessionId, String message) {
		JobState s = jobs.get(sessionId);
		if (s == null) return;
		s.status = Status.FAILED;
		s.errorMessage = message;
		s.completedAtMs = System.currentTimeMillis();
	}

	private void sweepExpired() {
		long now = System.currentTimeMillis();
		jobs.values().removeIf(state ->
				state.status != Status.RUNNING
						&& state.completedAtMs > 0
						&& (now - state.completedAtMs) > GRACE_PERIOD_MS);
	}
}
