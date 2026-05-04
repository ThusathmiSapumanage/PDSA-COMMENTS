"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import GameLayoutShell from "../layouts/GameLayoutShell";
import AuthGate from "../auth/AuthGate";
import QueensBoard from "./QueensBoard";
import PerformanceStats from "./PerformanceStats";
import PerformanceRunChart from "../common/PerformanceRunChart";
import SolutionProgress from "./SolutionProgress";
import {
  Position,
  ProgressResponse,
  StartResponse,
  SubmitResponse,
} from "./types";
import {
  cancelQueensRun,
  getQueensProgress,
  startQueensRound,
  submitQueensAnswer,
} from "../../src/lib/QueensApi";

const BOARD_SIZE = 16;
const BENCHMARK_TIME_BUDGET_MS = 15_000;
const FULL_SOLVE_BENCHMARK_ROUNDS = 5;

type QueenCountOption = 8 | 16;
type TimeCapOption = "none" | "15s" | "60s";
type BenchmarkMode = "timed" | "full-solve";

const TIME_CAP_MS: Record<TimeCapOption, number | null> = {
  none: null,
  "15s": 15_000,
  "60s": 60_000,
};

type BenchmarkRow = {
  round: number;
  sequentialFound: number;
  threadedFound: number;
  sequentialTimeMs: number;
  threadedTimeMs: number;
};

type GamePhase =
  | "idle"
  | "starting"
  | "running"
  | "ready"
  | "submitting"
  | "result"
  | "complete";

const PROGRESS_POLL_INTERVAL_MS = 400;

export default function QueensPuzzleGame() {
  const [phase, setPhase] = useState<GamePhase>("idle");
  const [queenCount, setQueenCount] = useState<QueenCountOption>(8);
  const [timeCap, setTimeCap] = useState<TimeCapOption>("15s");
  const [activeQueenCount, setActiveQueenCount] = useState<QueenCountOption>(8);
  const [roundData, setRoundData] = useState<StartResponse | null>(null);
  const [queens, setQueens] = useState<Position[]>([]);
  const [lastResult, setLastResult] = useState<SubmitResponse | null>(null);
  const [discovered, setDiscovered] = useState(0);
  const [totalSolutions, setTotalSolutions] = useState(0);
  const [error, setError] = useState("");
  const [benchmarkData, setBenchmarkData] = useState<BenchmarkRow[]>([]);
  const [benchmarkMode, setBenchmarkMode] = useState<BenchmarkMode>("timed");
  const [loadingBenchmark, setLoadingBenchmark] = useState(false);
  const [progress, setProgress] = useState<ProgressResponse | null>(null);
  const [pollTick, setPollTick] = useState(0);
  const [cancelling, setCancelling] = useState(false);
  const pollingTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const pollSessionIdRef = useRef<number | null>(null);

  const formatMs = (value: number) => {
    if (!Number.isFinite(value)) return "0.000";
    return value.toFixed(3);
  };

  const stopPolling = useCallback(() => {
    if (pollingTimerRef.current) {
      clearInterval(pollingTimerRef.current);
      pollingTimerRef.current = null;
    }
    pollSessionIdRef.current = null;
  }, []);

  useEffect(() => {
    return () => stopPolling();
  }, [stopPolling]);

  const applyFinalProgress = useCallback((snap: ProgressResponse) => {
    const total = snap.totalSolutions ?? snap.sequentialFound;
    setTotalSolutions(total);
    setRoundData((prev) =>
      prev
        ? {
            ...prev,
            totalSolutions: total,
            sequentialFound: snap.sequentialFound,
            threadedFound: snap.threadedFound,
            sequentialTimeMs: snap.sequentialTimeMs,
            threadedTimeMs: snap.threadedTimeMs,
            status: "READY",
          }
        : prev,
    );
  }, []);

  const startPolling = useCallback(
    (sessionId: number) => {
      stopPolling();
      pollSessionIdRef.current = sessionId;
      pollingTimerRef.current = setInterval(async () => {
        if (pollSessionIdRef.current !== sessionId) return;
        try {
          const snap = await getQueensProgress(sessionId);
          console.log("[queens poll]", sessionId, snap);
          setPollTick((t) => t + 1);
          setProgress(snap);
          if (snap.status !== "RUNNING") {
            stopPolling();
            applyFinalProgress(snap);
            if (snap.status === "COMPLETED") {
              setPhase("ready");
            } else if (snap.status === "CANCELLED") {
              setError("Run cancelled — partial counts shown.");
              setPhase("ready");
            } else {
              setError(snap.message || "Solver failed.");
              setPhase("idle");
            }
          }
        } catch (err) {
          stopPolling();
          setError(err instanceof Error ? err.message : "Progress poll failed.");
          setPhase("idle");
        }
      }, PROGRESS_POLL_INTERVAL_MS);
    },
    [applyFinalProgress, stopPolling],
  );

  const handleCancel = useCallback(async () => {
    const sessionId = pollSessionIdRef.current ?? roundData?.sessionId;
    if (!sessionId) return;
    setCancelling(true);
    try {
      await cancelQueensRun(sessionId);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Cancel failed.");
    } finally {
      setCancelling(false);
    }
  }, [roundData]);

  const resolveTimeBudget = useCallback(
    (qc: QueenCountOption, cap: TimeCapOption): number | undefined => {
      // 16 queens always runs full (cached after first run).
      if (qc === 16) return undefined;
      const ms = TIME_CAP_MS[cap];
      return ms === null ? undefined : ms;
    },
    [],
  );

  const handleStartRound = useCallback(async () => {
    setError("");
    setLastResult(null);
    setQueens([]);
    setProgress(null);
    setPhase("starting");

    try {
      const timeBudgetMs = resolveTimeBudget(queenCount, timeCap);
      const response = await startQueensRound({
        boardSize: BOARD_SIZE,
        queenCount,
        ...(timeBudgetMs !== undefined ? { timeBudgetMs } : {}),
      });
      setRoundData(response);
      setActiveQueenCount(queenCount);
      setTotalSolutions(response.totalSolutions);
      setDiscovered(0);

      if (response.status === "RUNNING") {
        setProgress({
          sessionId: response.sessionId,
          boardSize: response.boardSize,
          queenCount: response.queenCount,
          sequentialFound: 0,
          threadedFound: 0,
          sequentialTimeMs: 0,
          threadedTimeMs: 0,
          sequentialDone: false,
          threadedDone: false,
          status: "RUNNING",
          totalSolutions: null,
          message: response.message,
        });
        setPhase("running");
        startPolling(response.sessionId);
      } else {
        setPhase("ready");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to start round.");
      setPhase("idle");
    }
  }, [queenCount, timeCap, resolveTimeBudget, startPolling]);

  const handleCellClick = useCallback(
    (x: number, y: number) => {
      if (phase !== "ready" && phase !== "result" && phase !== "running") return;

      setLastResult(null);

      setQueens((prev) => {
        const existing = prev.find((q) => q.x === x && q.y === y);
        if (existing) {
          return prev.filter((q) => !(q.x === x && q.y === y));
        }
        if (prev.length >= activeQueenCount) {
          return prev;
        }
        return [...prev, { x, y }];
      });

      if (phase === "result") {
        setPhase("ready");
      }
    },
    [phase, activeQueenCount],
  );

  const handleClearBoard = useCallback(() => {
    setQueens([]);
    setLastResult(null);
    if (phase === "result") setPhase("ready");
  }, [phase]);

  const handleSubmit = useCallback(async () => {
    if (!roundData) {
      setError("Start a round first.");
      return;
    }
    if (queens.length !== activeQueenCount) {
      setError(`Place exactly ${activeQueenCount} queens before submitting.`);
      return;
    }

    setError("");
    setPhase("submitting");

    try {
      const response = await submitQueensAnswer({
        sessionId: roundData.sessionId,
        queens,
      });
      setLastResult(response);
      setDiscovered(response.solutionsDiscovered);
      setTotalSolutions(response.totalSolutions);

      const isAllFound =
        response.correct &&
        !response.alreadyDiscovered &&
        response.solutionsDiscovered === 0 &&
        response.totalSolutions > 0;

      setPhase(isAllFound ? "complete" : "result");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to submit answer.");
      setPhase("ready");
    }
  }, [roundData, queens, activeQueenCount]);

  const handleNewRound = useCallback(() => {
    stopPolling();
    setRoundData(null);
    setQueens([]);
    setLastResult(null);
    setDiscovered(0);
    setTotalSolutions(0);
    setBenchmarkData([]);
    setProgress(null);
    setPhase("idle");
    setError("");
  }, [stopPolling]);

  const handleRun20Times = useCallback(async () => {
    setError("");
    setLastResult(null);
    setQueens([]);
    setPhase("starting");
    setLoadingBenchmark(true);
    setBenchmarkData([]);
    setBenchmarkMode("timed");

    try {
      const runs: BenchmarkRow[] = [];

      for (let i = 1; i <= 20; i++) {
        const response = await startQueensRound({
          boardSize: BOARD_SIZE,
          queenCount,
          timeBudgetMs: BENCHMARK_TIME_BUDGET_MS,
        });

        setRoundData(response);
        setActiveQueenCount(queenCount);
        setTotalSolutions(response.totalSolutions);
        setDiscovered(0);

        runs.push({
          round: i,
          sequentialFound: response.sequentialFound,
          threadedFound: response.threadedFound,
          sequentialTimeMs: response.sequentialTimeMs,
          threadedTimeMs: response.threadedTimeMs,
        });
        setBenchmarkData([...runs]);
      }

      setPhase("ready");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to run benchmark.");
      setPhase("idle");
    } finally {
      setLoadingBenchmark(false);
    }
  }, [queenCount]);

  const handleRun5FullSolve = useCallback(async () => {
    setError("");
    setLastResult(null);
    setQueens([]);
    setPhase("starting");
    setLoadingBenchmark(true);
    setBenchmarkData([]);
    setBenchmarkMode("full-solve");

    try {
      const runs: BenchmarkRow[] = [];

      for (let i = 1; i <= FULL_SOLVE_BENCHMARK_ROUNDS; i++) {
        const response = await startQueensRound({
          boardSize: BOARD_SIZE,
          queenCount: 16,
          skipCache: true,
        });

        setRoundData(response);
        setActiveQueenCount(16);
        setTotalSolutions(response.totalSolutions);
        setDiscovered(0);

        runs.push({
          round: i,
          sequentialFound: response.sequentialFound,
          threadedFound: response.threadedFound,
          sequentialTimeMs: response.sequentialTimeMs,
          threadedTimeMs: response.threadedTimeMs,
        });
        setBenchmarkData([...runs]);
      }

      setPhase("ready");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to run benchmark.");
      setPhase("idle");
    } finally {
      setLoadingBenchmark(false);
    }
  }, []);

  const isBoardDisabled = phase === "starting" || phase === "submitting";
  const canSubmit =
    queens.length === activeQueenCount &&
    (phase === "ready" || phase === "running");

  const queenCountButton = (value: QueenCountOption, label: string) => (
    <button
      onClick={() => setQueenCount(value)}
      className={`flex-1 py-3 rounded font-bold uppercase tracking-widest text-xs transition-colors border ${
        queenCount === value
          ? "bg-blue-600 text-white border-blue-500"
          : "bg-transparent text-white/60 border-white/10 hover:border-blue-400/60 hover:text-white"
      }`}
    >
      {label}
    </button>
  );

  const timeCapButton = (value: TimeCapOption, label: string, hint: string) => (
    <button
      onClick={() => setTimeCap(value)}
      className={`flex flex-col items-center gap-0.5 py-2.5 px-2 rounded font-mono uppercase tracking-[0.15em] text-[10px] transition-colors border ${
        timeCap === value
          ? "bg-cyan-500/15 text-cyan-200 border-cyan-400/60"
          : "bg-transparent text-white/50 border-white/10 hover:border-white/30 hover:text-white/80"
      }`}
    >
      <span className="font-bold">{label}</span>
      <span className="text-[8px] opacity-70">{hint}</span>
    </button>
  );

  const currentBudgetLabel =
    queenCount === 16
      ? "Full search (cached after first run)"
      : timeCap === "none"
        ? "Full search — no time cap (first run may be slow, then cached)"
        : timeCap === "15s"
          ? "Time-bounded — each algorithm runs for ~15 seconds"
          : "Time-bounded — each algorithm runs for ~60 seconds";

  const controls = (
    <div className="flex flex-col gap-6">
      {phase === "idle" && (
        <>
          <div>
            <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40 mb-3 block">
              Queens to place
            </label>
            <div className="flex gap-2">
              {queenCountButton(8, "8 Queens")}
              {queenCountButton(16, "16 Queens")}
            </div>
            <p className="mt-2 text-[9px] font-mono uppercase tracking-[0.2em] text-white/30">
              {queenCount === 16
                ? "Full spec: 16 non-attacking queens on 16×16."
                : "8 non-attacking queens on 16×16 (vastly more solutions)."}
            </p>
          </div>

          {queenCount === 8 && (
            <div>
              <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40 mb-3 block">
                Time cap
              </label>
              <div className="grid grid-cols-3 gap-2">
                {timeCapButton("15s", "15 sec", "Quick")}
                {timeCapButton("60s", "< 1 min", "Thorough")}
                {timeCapButton("none", "No cap", "Full + cache")}
              </div>
              <p className="mt-2 text-[9px] font-mono uppercase tracking-[0.2em] text-white/30">
                8-queens on 16×16 is intractable. Time cap samples solutions;
                &quot;No cap&quot; runs the full bitmask count (slow first time, instant after).
              </p>
            </div>
          )}

          <div className="text-[10px] font-mono uppercase tracking-[0.2em] text-white/40 leading-relaxed">
            {currentBudgetLabel}
          </div>

          <button
            onClick={handleStartRound}
            className="bg-white text-black py-3 rounded font-bold uppercase tracking-widest hover:bg-blue-600 hover:text-white transition-colors text-xs"
          >
            Deploy Queens
          </button>

          <button
            onClick={handleRun20Times}
            disabled={loadingBenchmark}
            className="bg-transparent border border-white/10 text-white/70 py-2.5 rounded font-bold uppercase tracking-widest hover:border-cyan-400/60 hover:text-cyan-200 transition-colors text-[10px] disabled:opacity-30 disabled:cursor-not-allowed"
          >
            {loadingBenchmark
              ? "Benchmarking..."
              : `Run 20× (${(BENCHMARK_TIME_BUDGET_MS / 1000).toFixed(0)}s timed)`}
          </button>

          {queenCount === 16 && (
            <button
              onClick={handleRun5FullSolve}
              disabled={loadingBenchmark}
              className="bg-transparent border border-white/10 text-white/70 py-2.5 rounded font-bold uppercase tracking-widest hover:border-emerald-400/60 hover:text-emerald-200 transition-colors text-[10px] disabled:opacity-30 disabled:cursor-not-allowed"
            >
              {loadingBenchmark
                ? "Benchmarking..."
                : `Run ${FULL_SOLVE_BENCHMARK_ROUNDS}× (full solve, no cache)`}
            </button>
          )}
        </>
      )}

      {phase === "starting" && (
        <div className="text-center py-8">
          <div className="inline-block w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin mb-4" />
          <div className="text-[10px] font-mono uppercase tracking-[0.2em] text-white/60 mb-2">
            {loadingBenchmark ? "Benchmarking..." : "Solving..."}
          </div>
          <div className="text-[9px] font-mono uppercase tracking-[0.15em] text-white/30 leading-relaxed max-w-[240px] mx-auto">
            Searching {queenCount}-queen placements on {BOARD_SIZE}×{BOARD_SIZE}.
            <br />
            {currentBudgetLabel}
          </div>
        </div>
      )}

      {phase === "running" && progress && (
        <div className="flex flex-col gap-5 rounded-xl border border-cyan-400/30 bg-cyan-400/5 p-4">
          <div className="flex items-center justify-between">
            <div className="text-[10px] font-mono uppercase tracking-[0.3em] text-cyan-200">
              Async Solve Running
            </div>
            <div className="flex items-center gap-2">
              <span className="text-[9px] font-mono text-cyan-300/60">polls: {pollTick}</span>
              <div className="inline-block w-3 h-3 border-2 border-cyan-300 border-t-transparent rounded-full animate-spin" />
            </div>
          </div>
          <div>
            <div className="flex items-center justify-between mb-1">
              <span className="text-[9px] font-mono uppercase tracking-[0.2em] text-amber-300/80">
                Sequential
              </span>
              <span className="text-[10px] font-mono text-white/60 tabular-nums">
                {(progress.sequentialTimeMs / 1000).toFixed(1)}s
                {progress.sequentialDone && " ✓"}
              </span>
            </div>
            <div className="text-lg font-black text-amber-200 tabular-nums">
              {progress.sequentialFound.toLocaleString()}
            </div>
          </div>
          <div>
            <div className="flex items-center justify-between mb-1">
              <span className="text-[9px] font-mono uppercase tracking-[0.2em] text-cyan-300/80">
                Threaded
              </span>
              <span className="text-[10px] font-mono text-white/60 tabular-nums">
                {(progress.threadedTimeMs / 1000).toFixed(1)}s
                {progress.threadedDone && " ✓"}
              </span>
            </div>
            <div className="text-lg font-black text-cyan-200 tabular-nums">
              {progress.threadedFound.toLocaleString()}
            </div>
          </div>
          {progress.sequentialFound > 0 && (
            <div className="pt-2 border-t border-white/10">
              <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-white/40 mb-1">
                Threaded / Sequential
              </div>
              <div className="text-sm font-black text-green-400 tabular-nums">
                {(progress.threadedFound / progress.sequentialFound).toFixed(2)}×
              </div>
            </div>
          )}
          <button
            onClick={handleCancel}
            disabled={cancelling}
            className="bg-transparent border border-red-500/40 text-red-300 py-2 rounded font-bold uppercase tracking-widest hover:bg-red-500/10 transition-colors text-[10px] disabled:opacity-30 disabled:cursor-not-allowed"
          >
            {cancelling ? "Cancelling..." : "Cancel Solve"}
          </button>
          <p className="text-[9px] font-mono uppercase tracking-[0.2em] text-white/40 leading-relaxed">
            Place queens on the board and submit while the solver runs — submissions are validated independently.
          </p>
        </div>
      )}

      {phase === "running" && (
        <div className="flex flex-col gap-2 pt-2">
          <button
            onClick={handleSubmit}
            disabled={!canSubmit}
            className="bg-white text-black py-3 rounded font-bold uppercase tracking-widest hover:bg-blue-600 hover:text-white transition-colors text-xs disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-white disabled:hover:text-black"
          >
            Submit Solution
          </button>
          <button
            onClick={handleClearBoard}
            disabled={queens.length === 0}
            className="bg-transparent border border-white/10 text-white/70 py-2.5 rounded font-bold uppercase tracking-widest hover:border-red-500/50 hover:text-red-400 transition-colors text-[10px] disabled:opacity-30 disabled:cursor-not-allowed"
          >
            Clear Board
          </button>
        </div>
      )}

      {roundData && phase !== "idle" && phase !== "starting" && phase !== "running" && (
        <>
          <PerformanceStats
            sequentialTimeMs={roundData.sequentialTimeMs}
            threadedTimeMs={roundData.threadedTimeMs}
            totalSolutions={roundData.totalSolutions}
            sequentialFound={roundData.sequentialFound}
            threadedFound={roundData.threadedFound}
            sequentialHitLimit={roundData.sequentialHitLimit}
            threadedHitLimit={roundData.threadedHitLimit}
            timeBudgetMs={roundData.timeBudgetMs}
          />

          <div className="pt-4 border-t border-white/5">
            <SolutionProgress discovered={discovered} total={totalSolutions} />
          </div>

          <div className="flex flex-col gap-2 pt-4 border-t border-white/5">
            <button
              onClick={handleSubmit}
              disabled={!canSubmit}
              className="bg-white text-black py-3 rounded font-bold uppercase tracking-widest hover:bg-blue-600 hover:text-white transition-colors text-xs disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-white disabled:hover:text-black"
            >
              {phase === "submitting" ? "Checking..." : "Submit Solution"}
            </button>

            <button
              onClick={handleClearBoard}
              disabled={queens.length === 0 || isBoardDisabled}
              className="bg-transparent border border-white/10 text-white/70 py-2.5 rounded font-bold uppercase tracking-widest hover:border-red-500/50 hover:text-red-400 transition-colors text-[10px] disabled:opacity-30 disabled:cursor-not-allowed"
            >
              Clear Board
            </button>

            <button
              onClick={handleNewRound}
              className="bg-transparent border border-white/10 text-white/50 py-2.5 rounded font-bold uppercase tracking-widest hover:border-white/30 hover:text-white transition-colors text-[10px] mt-1"
            >
              New Round
            </button>

            <button
              onClick={handleRun20Times}
              disabled={loadingBenchmark || isBoardDisabled}
              className="bg-transparent border border-white/10 text-white/70 py-2.5 rounded font-bold uppercase tracking-widest hover:border-cyan-400/60 hover:text-cyan-200 transition-colors text-[10px] disabled:opacity-30 disabled:cursor-not-allowed"
            >
              {loadingBenchmark
              ? "Benchmarking..."
              : `Run 20× (${(BENCHMARK_TIME_BUDGET_MS / 1000).toFixed(0)}s timed)`}
            </button>

            {activeQueenCount === 16 && (
              <button
                onClick={handleRun5FullSolve}
                disabled={loadingBenchmark || isBoardDisabled}
                className="bg-transparent border border-white/10 text-white/70 py-2.5 rounded font-bold uppercase tracking-widest hover:border-emerald-400/60 hover:text-emerald-200 transition-colors text-[10px] disabled:opacity-30 disabled:cursor-not-allowed"
              >
                {loadingBenchmark
                  ? "Benchmarking..."
                  : `Run ${FULL_SOLVE_BENCHMARK_ROUNDS}× (full solve, no cache)`}
              </button>
            )}
          </div>
        </>
      )}

      {error && (
        <div className="border border-red-500/30 bg-red-500/5 rounded p-3 text-[11px] text-red-300 font-mono">
          {error}
        </div>
      )}
    </div>
  );

  const visualization = (
    <div className="w-full h-full flex flex-col items-center justify-center gap-6">
      {phase === "idle" && !roundData ? (
        <div className="text-center text-white/50 border-2 border-dashed border-blue-500/50 bg-blue-500/5 p-12 rounded-xl max-w-xl">
          <span className="font-mono text-xl md:text-2xl uppercase tracking-widest text-white mb-4 block font-black">
            Ready to Deploy
          </span>
          <p className="text-sm leading-relaxed mb-6">
            Click{" "}
            <span className="text-blue-400 font-bold">Deploy Queens</span> to
            begin. The solver will calculate all valid {queenCount}-queen
            placements on a {BOARD_SIZE}×{BOARD_SIZE} board.
          </p>
          <div className="inline-flex items-center gap-2 px-4 py-2 bg-black/50 rounded-full border border-white/10 text-[10px] font-mono">
            <div className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse" />
            Awaiting initialization
          </div>
        </div>
      ) : (
        <>
          <div className="w-full flex items-center justify-between max-w-[680px]">
            <div>
              <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-white/40">
                Queens Placed
              </div>
              <div className="text-2xl font-black text-white tabular-nums">
                {queens.length}{" "}
                <span className="text-white/30 text-sm">/ {activeQueenCount}</span>
              </div>
            </div>

            {lastResult && (
              <div
                className={`px-4 py-2 rounded font-mono text-[10px] uppercase tracking-[0.2em] border ${
                  lastResult.correct && !lastResult.alreadyDiscovered
                    ? "border-green-500/50 bg-green-500/10 text-green-300"
                    : lastResult.alreadyDiscovered
                      ? "border-amber-500/50 bg-amber-500/10 text-amber-300"
                      : "border-red-500/50 bg-red-500/10 text-red-300"
                }`}
              >
                {lastResult.correct && !lastResult.alreadyDiscovered
                  ? "New Solution ✓"
                  : lastResult.alreadyDiscovered
                    ? "Already Discovered"
                    : "Incorrect"}
              </div>
            )}

            {phase === "complete" && (
              <div className="px-4 py-2 rounded font-mono text-[10px] uppercase tracking-[0.2em] border border-green-500/50 bg-green-500/10 text-green-300">
                All Solutions Found ✓
              </div>
            )}
          </div>

          <QueensBoard
            boardSize={BOARD_SIZE}
            queens={queens}
            onCellClick={handleCellClick}
            disabled={isBoardDisabled}
          />

          <div className="text-[10px] font-mono uppercase tracking-[0.2em] text-white/30 text-center max-w-md">
            Click a cell to place a queen. Red tint shows attacked squares.
            Place exactly {activeQueenCount} queens with no conflicts, then submit.
          </div>

          {lastResult && (
            <div className="text-[11px] font-mono text-white/60 text-center max-w-md">
              {lastResult.message}
            </div>
          )}

          {benchmarkData.length > 0 && (
            <div className="w-full max-w-[920px]">
              <div
                className={`mb-4 rounded-xl border p-3 text-[10px] font-mono uppercase tracking-[0.2em] ${
                  benchmarkMode === "full-solve"
                    ? "border-emerald-400/25 bg-emerald-400/5 text-emerald-200/80"
                    : "border-cyan-400/20 bg-cyan-400/5 text-cyan-200/80"
                }`}
              >
                {benchmarkMode === "full-solve"
                  ? `Full-solve benchmark — ${FULL_SOLVE_BENCHMARK_ROUNDS} rounds of 16 queens on 16×16, cache bypassed each round. Elapsed time chart below is the headline metric.`
                  : `Timed benchmark — each round pins both algorithms to a ${(BENCHMARK_TIME_BUDGET_MS / 1000).toFixed(0)}s budget. Solutions-found chart below is the headline metric (higher = better throughput).`}
              </div>

              <PerformanceRunChart
                title="Solutions Found per Round"
                data={benchmarkData}
                series={[
                  {
                    key: "sequentialFound",
                    label: "Sequential",
                    colorClass: "bg-amber-500",
                    stroke: "#f59e0b",
                  },
                  {
                    key: "threadedFound",
                    label: "Threaded",
                    colorClass: "bg-cyan-400",
                    stroke: "#22d3ee",
                  },
                ]}
                yLabel="Solutions Counted"
                valueFormatter={(value) => Math.round(value).toLocaleString()}
                tooltipFormatter={(label, round, value) =>
                  `${label} - Round ${round}: ${Math.round(value).toLocaleString()} solutions`
                }
              />

              <div className="mt-4">
                <PerformanceRunChart
                  title="Elapsed Time per Round"
                  data={benchmarkData}
                  series={[
                    {
                      key: "sequentialTimeMs",
                      label: "Sequential",
                      colorClass: "bg-amber-500",
                      stroke: "#f59e0b",
                    },
                    {
                      key: "threadedTimeMs",
                      label: "Threaded",
                      colorClass: "bg-cyan-400",
                      stroke: "#22d3ee",
                    },
                  ]}
                  yLabel="Time (ms)"
                />
              </div>

              {(() => {
                const rounds = benchmarkData.length;
                const totalSeq = benchmarkData.reduce((s, r) => s + r.sequentialFound, 0);
                const totalThr = benchmarkData.reduce((s, r) => s + r.threadedFound, 0);
                const avgSeq = totalSeq / rounds;
                const avgThr = totalThr / rounds;
                const speedup = avgSeq > 0 ? avgThr / avgSeq : 0;
                return (
                  <div className="mt-6 grid grid-cols-3 gap-3 rounded-2xl border border-white/10 bg-[#050505] p-5">
                    <div>
                      <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-amber-400/70 mb-1">
                        Avg Seq Found / Round
                      </div>
                      <div className="text-lg font-black text-white tabular-nums">
                        {Math.round(avgSeq).toLocaleString()}
                      </div>
                    </div>
                    <div>
                      <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-cyan-400/70 mb-1">
                        Avg Threaded Found / Round
                      </div>
                      <div className="text-lg font-black text-white tabular-nums">
                        {Math.round(avgThr).toLocaleString()}
                      </div>
                    </div>
                    <div>
                      <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-white/40 mb-1">
                        Throughput Speedup
                      </div>
                      <div
                        className={`text-lg font-black tabular-nums ${
                          speedup > 1 ? "text-green-400" : "text-red-400"
                        }`}
                      >
                        {speedup.toFixed(2)}×
                      </div>
                    </div>
                  </div>
                );
              })()}

              <div className="mt-6 rounded-2xl border border-white/10 bg-[#050505] p-5 shadow-2xl">
                <div className="mb-4 text-[10px] font-bold uppercase tracking-[0.3em] text-cyan-400">
                  20-Run Benchmark Table
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full border-collapse text-left text-[11px] text-white/70">
                    <thead className="text-[10px] uppercase tracking-[0.2em] text-white/40">
                      <tr className="border-b border-white/10">
                        <th className="py-2 pr-3">Round</th>
                        <th className="py-2 pr-3">Seq Found</th>
                        <th className="py-2 pr-3">Threaded Found</th>
                        <th className="py-2 pr-3">Seq (ms)</th>
                        <th className="py-2 pr-3">Threaded (ms)</th>
                        <th className="py-2">Speedup</th>
                      </tr>
                    </thead>
                    <tbody className="font-mono">
                      {benchmarkData.map((row) => {
                        const ratio =
                          row.sequentialFound > 0
                            ? row.threadedFound / row.sequentialFound
                            : 0;
                        return (
                          <tr key={row.round} className="border-b border-white/5">
                            <td className="py-2 pr-3 text-white/60">
                              {row.round}
                            </td>
                            <td className="py-2 pr-3 text-amber-300 tabular-nums">
                              {row.sequentialFound.toLocaleString()}
                            </td>
                            <td className="py-2 pr-3 text-cyan-300 tabular-nums">
                              {row.threadedFound.toLocaleString()}
                            </td>
                            <td className="py-2 pr-3 text-amber-300/60 tabular-nums">
                              {formatMs(row.sequentialTimeMs)}
                            </td>
                            <td className="py-2 pr-3 text-cyan-300/60 tabular-nums">
                              {formatMs(row.threadedTimeMs)}
                            </td>
                            <td
                              className={`py-2 tabular-nums ${
                                ratio > 1 ? "text-green-400" : "text-red-400"
                              }`}
                            >
                              {ratio.toFixed(2)}×
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );

  return (
    <AuthGate>
      <GameLayoutShell
        gameId="05"
        title="Sixteen Queens Puzzle"
        description={`Place ${activeQueenCount} queens on a ${BOARD_SIZE}×${BOARD_SIZE} board. Backend compares sequential vs threaded backtracking execution times.`}
        controls={controls}
        visualization={visualization}
      />
    </AuthGate>
  );
}
