package com.pdsa.games.queenspuzzle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Bitmask-based queens solver used by Sixteen Queens service paths.
 *
 * Provides synchronous, threaded, progress-aware, and timed solve methods for
 * board configurations up to the supported bitmask limit.
 */
class QueensSolver {

    static final long DEFAULT_TIME_BUDGET_MS = 15_000L;
    static final int DEFAULT_MAX_STORED = 5_000;
    private static final int SEED_DEPTH = 2;
    private static final int MAX_SUPPORTED_BOARD = 31;

    static final class TimedResult {
        final List<int[]> sample;
        final int totalFound;
        final long elapsedMs;
        final boolean hitTimeLimit;

        TimedResult(List<int[]> sample, int totalFound, long elapsedMs, boolean hitTimeLimit) {
            this.sample = sample;
            this.totalFound = totalFound;
            this.elapsedMs = elapsedMs;
            this.hitTimeLimit = hitTimeLimit;
        }
    }

    // ---------------- Count-only ----------------

    /**
     * Counts solutions sequentially using bitmask backtracking.
     *
     * @param boardSize board dimension
     * @param queenCount number of queens
     * @return number of valid placements
     */
    int solveSequentialCount(int boardSize, int queenCount) {
        validate(boardSize, queenCount);
        int boardMask = boardMask(boardSize);
        int[] rowToCol = initRowToCol(boardSize);
        long[] counter = {0L};
        countBacktrack(0, 0, boardSize, queenCount, 0, 0, 0, boardMask, rowToCol, counter);
        return clampToInt(counter[0]);
    }

    /**
     * Counts solutions using a thread pool and seeded subtrees.
     *
     * @param boardSize board dimension
     * @param queenCount number of queens
     * @return number of valid placements
     */
    int solveThreadedCount(int boardSize, int queenCount) {
        validate(boardSize, queenCount);
        int boardMask = boardMask(boardSize);
        List<Seed> seeds = buildSeeds(boardSize, queenCount, SEED_DEPTH);

        ExecutorService executor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors())
        );
        try {
            List<Future<Long>> futures = new ArrayList<>();
            for (Seed seed : seeds) {
                futures.add(executor.submit((Callable<Long>) () -> {
                    long[] local = {0L};
                    int[] r2c = seed.rowToCol.clone();
                    countBacktrack(seed.row, seed.placed, boardSize, queenCount,
                            seed.cols, seed.ld, seed.rd, boardMask, r2c, local);
                    return local[0];
                }));
            }
            long total = 0;
            for (Future<Long> f : futures) total += f.get();
            return clampToInt(total);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Threaded solver interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Threaded solver failed", ex.getCause());
        } finally {
            executor.shutdownNow();
        }
    }

    // ---------------- Count with live progress + cancel support ----------------

    /**
     * Bitmask sequential count that periodically flushes progress to a shared {@link LongAdder}
     * so external threads (e.g. a polling endpoint) can observe intermediate counts.
     * Honors the {@code cancelled} flag — returns partial count on cancellation.
     */
    long solveSequentialCountWithProgress(int boardSize, int queenCount,
                                          LongAdder published, AtomicBoolean cancelled) {
        validate(boardSize, queenCount);
        int boardMask = boardMask(boardSize);
        long[] local = {0L};
        long[] lastPublished = {0L};
        progressBacktrack(0, 0, boardSize, queenCount, 0, 0, 0, boardMask,
                local, lastPublished, published, cancelled);
        published.add(local[0] - lastPublished[0]);
        lastPublished[0] = local[0];
        return local[0];
    }

    /**
     * Bitmask threaded count with live progress. Seeds the tree at a fixed depth, fans out to
     * the shared thread pool, and each worker periodically flushes to the shared adder.
     */
    long solveThreadedCountWithProgress(int boardSize, int queenCount,
                                        LongAdder published, AtomicBoolean cancelled) {
        validate(boardSize, queenCount);
        int boardMask = boardMask(boardSize);
        List<Seed> seeds = buildSeeds(boardSize, queenCount, SEED_DEPTH);

        ExecutorService executor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors())
        );
        try {
            List<Future<Long>> futures = new ArrayList<>();
            for (Seed seed : seeds) {
                futures.add(executor.submit((Callable<Long>) () -> {
                    long[] local = {0L};
                    long[] lastPublished = {0L};
                    progressBacktrack(seed.row, seed.placed, boardSize, queenCount,
                            seed.cols, seed.ld, seed.rd, boardMask,
                            local, lastPublished, published, cancelled);
                    published.add(local[0] - lastPublished[0]);
                    return local[0];
                }));
            }
            long total = 0;
            for (Future<Long> f : futures) total += f.get();
            return total;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Threaded solver interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Threaded solver failed", ex.getCause());
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Backtracking helper that publishes count progress and honors cancellation.
     */
    private void progressBacktrack(int row, int placed, int n, int qc,
                                   int cols, int ld, int rd, int boardMask,
                                   long[] local, long[] lastPublished,
                                   LongAdder published, AtomicBoolean cancelled) {
        if (cancelled.get()) return;
        if (placed == qc) {
            local[0]++;
            long delta = local[0] - lastPublished[0];
            if (delta >= 65_536L) {
                published.add(delta);
                lastPublished[0] = local[0];
            }
            return;
        }
        if (row == n) return;
        if (n - row < qc - placed) return;

        int available = boardMask & ~(cols | ld | rd);
        while (available != 0) {
            int bit = available & -available;
            available ^= bit;
            progressBacktrack(row + 1, placed + 1, n, qc,
                    cols | bit, (ld | bit) << 1, (rd | bit) >>> 1, boardMask,
                    local, lastPublished, published, cancelled);
            if (cancelled.get()) return;
        }
        if (n - (row + 1) >= qc - placed) {
            progressBacktrack(row + 1, placed, n, qc,
                    cols, ld << 1, rd >>> 1, boardMask, local, lastPublished, published, cancelled);
        }
    }

    // ---------------- Timed (sample + count) ----------------

    /**
     * Runs a timed sequential solve and stores a sample of found solutions.
     *
     * @param boardSize board dimension
     * @param queenCount number of queens
     * @param timeBudgetMs maximum search time in milliseconds
     * @param maxStored max sample solutions to store
     * @return timed result containing count, elapsed time, and sample solutions
     */
    TimedResult solveSequentialTimed(int boardSize, int queenCount, long timeBudgetMs, int maxStored) {
        validate(boardSize, queenCount);
        long startNs = System.nanoTime();
        long deadlineNs = startNs + Math.max(1L, timeBudgetMs) * 1_000_000L;
        int boardMask = boardMask(boardSize);
        int[] rowToCol = initRowToCol(boardSize);
        List<int[]> sample = new ArrayList<>();
        long[] counter = {0L};
        timedBacktrack(0, 0, boardSize, queenCount, 0, 0, 0, boardMask,
                rowToCol, sample, counter, maxStored, deadlineNs);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        boolean hit = System.nanoTime() >= deadlineNs;
        return new TimedResult(sample, clampToInt(counter[0]), elapsedMs, hit);
    }

    /**
     * Runs a timed threaded solve and stores a sample of found solutions.
     *
     * @param boardSize board dimension
     * @param queenCount number of queens
     * @param timeBudgetMs maximum search time in milliseconds
     * @param maxStored max sample solutions to store
     * @return timed result containing count, elapsed time, and sample solutions
     */
    TimedResult solveThreadedTimed(int boardSize, int queenCount, long timeBudgetMs, int maxStored) {
        validate(boardSize, queenCount);
        long startNs = System.nanoTime();
        long deadlineNs = startNs + Math.max(1L, timeBudgetMs) * 1_000_000L;
        int boardMask = boardMask(boardSize);
        List<Seed> seeds = buildSeeds(boardSize, queenCount, SEED_DEPTH);

        ExecutorService executor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors())
        );
        AtomicLong counter = new AtomicLong(0);
        List<int[]> sample = Collections.synchronizedList(new ArrayList<>());

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Seed seed : seeds) {
                futures.add(executor.submit(() -> {
                    int[] r2c = seed.rowToCol.clone();
                    timedBacktrackShared(seed.row, seed.placed, boardSize, queenCount,
                            seed.cols, seed.ld, seed.rd, boardMask,
                            r2c, sample, counter, maxStored, deadlineNs);
                }));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ExecutionException ex) {
                    throw new IllegalStateException("Threaded solver failed", ex.getCause());
                }
            }
        } finally {
            executor.shutdownNow();
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        boolean hit = System.nanoTime() >= deadlineNs;
        return new TimedResult(new ArrayList<>(sample), clampToInt(counter.get()), elapsedMs, hit);
    }

    // ---------------- Collect (full list — used by tests and small boards only) ----------------

    /**
     * Collects all solutions sequentially, primarily used for tests and small boards.
     *
     * @param boardSize board dimension
     * @param queenCount number of queens
     * @return list of all valid solutions
     */
    List<int[]> solveSequential(int boardSize, int queenCount) {
        validate(boardSize, queenCount);
        int boardMask = boardMask(boardSize);
        int[] rowToCol = initRowToCol(boardSize);
        List<int[]> solutions = new ArrayList<>();
        collectBacktrack(0, 0, boardSize, queenCount, 0, 0, 0, boardMask, rowToCol, solutions);
        return solutions;
    }

    // ---------------- Core bitmask backtrack ----------------

    /**
     * Recursively counts placements for the standard bitmask backtracking path.
     */
    private void countBacktrack(int row, int placed, int n, int qc,
                                int cols, int ld, int rd, int boardMask,
                                int[] rowToCol, long[] counter) {
        if (placed == qc) {
            counter[0]++;
            return;
        }
        if (row == n) return;
        if (n - row < qc - placed) return;

        int available = boardMask & ~(cols | ld | rd);
        while (available != 0) {
            int bit = available & -available;
            available ^= bit;
            int col = Integer.numberOfTrailingZeros(bit);
            rowToCol[row] = col;
            countBacktrack(row + 1, placed + 1, n, qc,
                    cols | bit, (ld | bit) << 1, (rd | bit) >>> 1, boardMask,
                    rowToCol, counter);
        }
        if (n - (row + 1) >= qc - placed) {
            rowToCol[row] = -1;
            countBacktrack(row + 1, placed, n, qc,
                    cols, ld << 1, rd >>> 1, boardMask, rowToCol, counter);
        }
    }

    /**
     * Recursively searches while collecting a sample of solutions and respecting a deadline.
     */
    private boolean timedBacktrack(int row, int placed, int n, int qc,
                                   int cols, int ld, int rd, int boardMask,
                                   int[] rowToCol, List<int[]> sample,
                                   long[] counter, int maxStored, long deadlineNs) {
        if (System.nanoTime() >= deadlineNs) return true;
        if (placed == qc) {
            counter[0]++;
            if (sample.size() < maxStored) sample.add(copyBoard(rowToCol));
            return false;
        }
        if (row == n) return false;
        if (n - row < qc - placed) return false;

        int available = boardMask & ~(cols | ld | rd);
        while (available != 0) {
            int bit = available & -available;
            available ^= bit;
            int col = Integer.numberOfTrailingZeros(bit);
            rowToCol[row] = col;
            boolean stop = timedBacktrack(row + 1, placed + 1, n, qc,
                    cols | bit, (ld | bit) << 1, (rd | bit) >>> 1, boardMask,
                    rowToCol, sample, counter, maxStored, deadlineNs);
            if (stop) return true;
        }
        if (n - (row + 1) >= qc - placed) {
            rowToCol[row] = -1;
            boolean stop = timedBacktrack(row + 1, placed, n, qc,
                    cols, ld << 1, rd >>> 1, boardMask, rowToCol,
                    sample, counter, maxStored, deadlineNs);
            if (stop) return true;
        }
        return false;
    }

    /**
     * Recursively searches in a threaded context while sharing sample storage and count.
     */
    private boolean timedBacktrackShared(int row, int placed, int n, int qc,
                                         int cols, int ld, int rd, int boardMask,
                                         int[] rowToCol, List<int[]> sample,
                                         AtomicLong counter, int maxStored, long deadlineNs) {
        if (System.nanoTime() >= deadlineNs) return true;
        if (placed == qc) {
            long next = counter.incrementAndGet();
            if (next <= maxStored) sample.add(copyBoard(rowToCol));
            return false;
        }
        if (row == n) return false;
        if (n - row < qc - placed) return false;

        int available = boardMask & ~(cols | ld | rd);
        while (available != 0) {
            int bit = available & -available;
            available ^= bit;
            int col = Integer.numberOfTrailingZeros(bit);
            rowToCol[row] = col;
            boolean stop = timedBacktrackShared(row + 1, placed + 1, n, qc,
                    cols | bit, (ld | bit) << 1, (rd | bit) >>> 1, boardMask,
                    rowToCol, sample, counter, maxStored, deadlineNs);
            if (stop) return true;
        }
        if (n - (row + 1) >= qc - placed) {
            rowToCol[row] = -1;
            boolean stop = timedBacktrackShared(row + 1, placed, n, qc,
                    cols, ld << 1, rd >>> 1, boardMask, rowToCol,
                    sample, counter, maxStored, deadlineNs);
            if (stop) return true;
        }
        return false;
    }

    /**
     * Recursively collects every valid placement into the solutions list.
     */
    private void collectBacktrack(int row, int placed, int n, int qc,
                                  int cols, int ld, int rd, int boardMask,
                                  int[] rowToCol, List<int[]> solutions) {
        if (placed == qc) {
            solutions.add(copyBoard(rowToCol));
            return;
        }
        if (row == n) return;
        if (n - row < qc - placed) return;

        int available = boardMask & ~(cols | ld | rd);
        while (available != 0) {
            int bit = available & -available;
            available ^= bit;
            int col = Integer.numberOfTrailingZeros(bit);
            rowToCol[row] = col;
            collectBacktrack(row + 1, placed + 1, n, qc,
                    cols | bit, (ld | bit) << 1, (rd | bit) >>> 1, boardMask,
                    rowToCol, solutions);
        }
        if (n - (row + 1) >= qc - placed) {
            rowToCol[row] = -1;
            collectBacktrack(row + 1, placed, n, qc,
                    cols, ld << 1, rd >>> 1, boardMask, rowToCol, solutions);
        }
    }

    // ---------------- Seed partitioning for parallelism ----------------

    /**
     * Builds partial seed states for parallel search partitioning.
     *
     * @param boardSize board dimension
     * @param queenCount number of queens
     * @param depth seed depth to partition
     * @return list of seed states
     */
    private List<Seed> buildSeeds(int boardSize, int queenCount, int depth) {
        int boardMask = boardMask(boardSize);
        int[] rowToCol = initRowToCol(boardSize);
        List<Seed> seeds = new ArrayList<>();
        buildSeedsRec(0, 0, boardSize, queenCount, depth, 0, 0, 0, boardMask, rowToCol, seeds);
        return seeds;
    }

    /**
     * Recursively generates seed states until the target depth is reached.
     */
    private void buildSeedsRec(int row, int placed, int n, int qc, int depth,
                               int cols, int ld, int rd, int boardMask,
                               int[] rowToCol, List<Seed> seeds) {
        if (depth == 0 || placed == qc || row == n) {
            seeds.add(new Seed(row, placed, cols, ld, rd, rowToCol.clone()));
            return;
        }
        if (n - row < qc - placed) return;

        int available = boardMask & ~(cols | ld | rd);
        while (available != 0) {
            int bit = available & -available;
            available ^= bit;
            int col = Integer.numberOfTrailingZeros(bit);
            rowToCol[row] = col;
            buildSeedsRec(row + 1, placed + 1, n, qc, depth - 1,
                    cols | bit, (ld | bit) << 1, (rd | bit) >>> 1, boardMask, rowToCol, seeds);
        }
        if (n - (row + 1) >= qc - placed) {
            rowToCol[row] = -1;
            buildSeedsRec(row + 1, placed, n, qc, depth - 1,
                    cols, ld << 1, rd >>> 1, boardMask, rowToCol, seeds);
        }
    }

    // ---------------- Helpers ----------------

    /**
     * Initializes an empty row-to-column mapping for a board.
     */
    private int[] initRowToCol(int boardSize) {
        int[] r = new int[boardSize];
        for (int i = 0; i < boardSize; i++) r[i] = -1;
        return r;
    }

    /**
     * Copies the current board placement array.
     */
    private int[] copyBoard(int[] r) {
        int[] c = new int[r.length];
        System.arraycopy(r, 0, c, 0, r.length);
        return c;
    }

    /**
     * Computes a bitmask representing the available columns for the board.
     */
    private int boardMask(int n) {
        return n >= 31 ? 0x7FFFFFFF : (1 << n) - 1;
    }

    /**
     * Validates board and queen configuration constraints.
     */
    private void validate(int boardSize, int queenCount) {
        if (boardSize < 1 || boardSize > MAX_SUPPORTED_BOARD) {
            throw new IllegalArgumentException(
                    "Board size must be between 1 and " + MAX_SUPPORTED_BOARD + " for the bitmask solver.");
        }
        if (queenCount < 0 || queenCount > boardSize) {
            throw new IllegalArgumentException("Queen count must be between 0 and the board size.");
        }
    }

    /**
     * Clamps a long count value to the integer range.
     */
    private int clampToInt(long value) {
        if (value < 0) return 0;
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private record Seed(int row, int placed, int cols, int ld, int rd, int[] rowToCol) {
    }
}
