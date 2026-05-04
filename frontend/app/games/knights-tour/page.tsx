"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { motion } from "framer-motion";

import GameLayoutShell from "../../../components/layouts/GameLayoutShell";
import AuthGate from "../../../components/auth/AuthGate";
import AlgoNotify, { NotifyType } from "../../../components/forms/AlgoNotify";
import AlgoDropdown from "../../../components/forms/AlgoDropdown";
import KnightBoard from "../../../components/knightstour/KnightBoard";
import AlgorithmComparisonChart from "../../../components/knightstour/AlgorithmComparisonChart";
import PerformanceRunChart from "../../../components/common/PerformanceRunChart";

import type {
  CorrectAnswerResponse,
  HintResponse,
  Move,
  StartResponse,
  SubmitResponse,
} from "../../../components/knightstour/types";

import {
  getKnightTourCorrectAnswer,
  getKnightTourHint,
  hasKnightTourAuth,
  startKnightTourRound,
  submitKnightTourAnswer,
} from "../../../src/lib/KnightsTourApi";

type BenchmarkRow = {
  round: number;
  warnsdorff: number;
  backtracking: number;
};

function isValidKnightMove(fromX: number, fromY: number, toX: number, toY: number) {
  const dx = Math.abs(toX - fromX);
  const dy = Math.abs(toY - fromY);
  return (dx === 2 && dy === 1) || (dx === 1 && dy === 2);
}

function getReasonLabel(reasonCode?: string) {
  switch (reasonCode) {
    case "WRONG_START_POSITION":
      return "Wrong start position";
    case "OUT_OF_BOUNDS":
      return "Out of bounds";
    case "DUPLICATE_CELL":
      return "Duplicate cell";
    case "INVALID_STEP_SEQUENCE":
      return "Invalid step sequence";
    case "ILLEGAL_MOVE_SEQUENCE":
      return "Illegal move sequence";
    case "DEAD_END_BEFORE_COMPLETION":
      return "Dead end before completion";
    case "INCOMPLETE_TOUR":
      return "Incomplete tour";
    case "VALID_TOUR_DIFFERENT_FROM_SAVED_SOLUTIONS":
      return "Valid tour but different";
    case "EXACT_MATCH_WARNSDORFF":
      return "Exact Warnsdorff match";
    case "EXACT_MATCH_BACKTRACKING":
      return "Exact Backtracking match";
    case "EXACT_MATCH_BOTH":
      return "Exact match to both";
    default:
      return reasonCode || "--";
  }
}

function findMetricTime(metrics: StartResponse["algorithmMetrics"], names: string[]) {
  const found = metrics.find((m) =>
    names.some((name) => m.algorithmName.toLowerCase().includes(name.toLowerCase()))
  );
  return Number(found?.executionTimeMs ?? 0);
}

export default function KnightsTourPage() {
  const router = useRouter();
  const pathname = usePathname();
  const playbackTimerRef = useRef<NodeJS.Timeout | null>(null);

  const [boardSize, setBoardSize] = useState<8 | 16>(8);

  const [roundData, setRoundData] = useState<StartResponse | null>(null);
  const [submitResult, setSubmitResult] = useState<SubmitResponse | null>(null);
  const [hintResult, setHintResult] = useState<HintResponse | null>(null);
  const [correctAnswer, setCorrectAnswer] = useState<CorrectAnswerResponse | null>(null);
  const [benchmarkData, setBenchmarkData] = useState<BenchmarkRow[]>([]);

  const [selectedMoves, setSelectedMoves] = useState<Move[]>([]);

  const [loadingStart, setLoadingStart] = useState(false);
  const [loadingSubmit, setLoadingSubmit] = useState(false);
  const [loadingHint, setLoadingHint] = useState(false);
  const [loadingCorrectAnswer, setLoadingCorrectAnswer] = useState(false);
  const [loadingBenchmark, setLoadingBenchmark] = useState(false);

  const [hintCooldown, setHintCooldown] = useState(0);

  const [notify, setNotify] = useState<{
    show: boolean;
    type: NotifyType;
    title: string;
    message: string;
  }>({
    show: false,
    type: "warning",
    title: "",
    message: "",
  });

  const [viewMode, setViewMode] = useState<"player" | "warnsdorff" | "backtracking">("player");
  const [isPlaying, setIsPlaying] = useState(false);
  const [playbackStep, setPlaybackStep] = useState(0);
  const [playbackSpeed, setPlaybackSpeed] = useState<0.5 | 1 | 2>(1);

  const maxMoves = useMemo(() => boardSize * boardSize, [boardSize]);

  const warnsdorffMoves =
    submitResult?.warnsdorffCorrectMoves || correctAnswer?.warnsdorffMoves || [];

  const backtrackingMoves =
    submitResult?.backtrackingCorrectMoves || correctAnswer?.backtrackingMoves || [];

  const shownHintMoves = hintResult?.hintMoves || [];

  const activePlaybackMoves =
    viewMode === "warnsdorff"
      ? warnsdorffMoves
      : viewMode === "backtracking"
      ? backtrackingMoves
      : [];

  useEffect(() => {
    if (hintCooldown <= 0) return;
    const timer = setTimeout(() => setHintCooldown((prev) => prev - 1), 1000);
    return () => clearTimeout(timer);
  }, [hintCooldown]);

  useEffect(() => {
    if (playbackTimerRef.current) clearInterval(playbackTimerRef.current);

    if (isPlaying && activePlaybackMoves.length > 0) {
      playbackTimerRef.current = setInterval(() => {
        setPlaybackStep((prev) => {
          if (prev >= activePlaybackMoves.length - 1) {
            setIsPlaying(false);
            return prev;
          }
          return prev + 1;
        });
      }, 850 / playbackSpeed);
    }

    return () => {
      if (playbackTimerRef.current) clearInterval(playbackTimerRef.current);
    };
  }, [isPlaying, activePlaybackMoves, playbackSpeed]);

  useEffect(() => {
    setPlaybackStep(0);
    setIsPlaying(false);
  }, [viewMode]);

  const showNotify = (type: NotifyType, title: string, message: string) => {
    setNotify({ show: true, type, title, message });
  };

  const handleApiError = (error: unknown, fallbackMessage: string) => {
    const message = error instanceof Error ? error.message : fallbackMessage;
    const normalized = message.toLowerCase();

    if (
      normalized.includes("401") ||
      normalized.includes("unauthorized") ||
      normalized.includes("authentication")
    ) {
      showNotify(
        "error",
        "Login Required",
        "Your session is missing, expired, or invalid. Please log in again."
      );
      const nextPath = pathname ? `?next=${encodeURIComponent(pathname)}` : "";
      setTimeout(() => router.replace(`/login${nextPath}`), 1400);
      return;
    }

    if (normalized.includes("403") || normalized.includes("forbidden")) {
      showNotify(
        "error",
        "Access Denied",
        "You are logged in, but you do not have permission for this action."
      );
      return;
    }

    showNotify("error", "Request Failed", message || fallbackMessage);
  };

  const resetRoundState = () => {
    setSubmitResult(null);
    setHintResult(null);
    setCorrectAnswer(null);
    setSelectedMoves([]);
    setViewMode("player");
    setPlaybackStep(0);
    setIsPlaying(false);
  };

  const handleBoardSizeChange = (value: string) => {
    const isBusy =
      loadingStart ||
      loadingSubmit ||
      loadingHint ||
      loadingCorrectAnswer ||
      loadingBenchmark;
    if (isBusy) {
      showNotify("warning", "Please Wait", "Finish the current request before changing board size.");
      return;
    }

    const nextBoardSize = Number(value) as 8 | 16;

    if (nextBoardSize === boardSize) return;

    resetRoundState();
    setRoundData(null);
    setBenchmarkData([]);
    setHintCooldown(0);
    setBoardSize(nextBoardSize);
  };

  const handleStartRound = async () => {
    if (!hasKnightTourAuth()) {
      showNotify("error", "Not Logged In", "Please log in before starting a round.");
      const nextPath = pathname ? `?next=${encodeURIComponent(pathname)}` : "";
      setTimeout(() => router.replace(`/login${nextPath}`), 1200);
      return;
    }

    try {
      resetRoundState();
      setLoadingStart(true);

      const response = await startKnightTourRound({ boardSize });
      setRoundData(response);

      showNotify(
        "success",
        "Round Ready",
        `Start from (${response.startX}, ${response.startY}) and build the tour.`
      );
    } catch (error) {
      handleApiError(error, "Failed to start round.");
    } finally {
      setLoadingStart(false);
    }
  };

  const handleRun20Times = async () => {
    if (!hasKnightTourAuth()) {
      showNotify("error", "Not Logged In", "Please log in before running the benchmark.");
      const nextPath = pathname ? `?next=${encodeURIComponent(pathname)}` : "";
      setTimeout(() => router.replace(`/login${nextPath}`), 1200);
      return;
    }

    try {
      setLoadingBenchmark(true);
      const runs: BenchmarkRow[] = [];

      for (let i = 1; i <= 20; i++) {
        const response = await startKnightTourRound({ boardSize });
        setRoundData(response);

        const warnsdorff = findMetricTime(response.algorithmMetrics, ["warnsdorff"]);
        const backtracking = findMetricTime(response.algorithmMetrics, ["backtracking"]);

        runs.push({
          round: i,
          warnsdorff,
          backtracking,
        });

        setBenchmarkData([...runs]);
      }

      showNotify(
        "success",
        "20 Runs Completed",
        "Knight's Tour timing chart generated for Warnsdorff and Backtracking."
      );
    } catch (error) {
      handleApiError(error, "Failed to run 20-round benchmark.");
    } finally {
      setLoadingBenchmark(false);
    }
  };

  const handleCellClick = (x: number, y: number) => {
    if (!roundData || submitResult || viewMode !== "player") return;

    const existing = selectedMoves.find((move) => move.x === x && move.y === y);

    if (existing) {
      const lastMove = selectedMoves[selectedMoves.length - 1];
      if (lastMove.x === x && lastMove.y === y) {
        setHintResult(null);
        setSelectedMoves((prev) => prev.slice(0, -1));
      } else {
        showNotify("warning", "Cannot Remove", "You can only remove the latest move.");
      }
      return;
    }

    if (selectedMoves.length >= maxMoves) {
      showNotify("warning", "Path Full", "You already selected the maximum number of moves.");
      return;
    }

    if (selectedMoves.length === 0) {
      if (x !== roundData.startX || y !== roundData.startY) {
        showNotify(
          "warning",
          "Wrong Start",
          `Your first move must be (${roundData.startX}, ${roundData.startY}).`
        );
        return;
      }

      setHintResult(null);
      setSelectedMoves([{ stepNo: 1, x, y }]);
      return;
    }

    const lastMove = selectedMoves[selectedMoves.length - 1];
    const legal = isValidKnightMove(lastMove.x, lastMove.y, x, y);

    if (!legal) {
      showNotify(
        "warning",
        "Illegal Move",
        `A knight must move in an L-shape from (${lastMove.x}, ${lastMove.y}).`
      );
      return;
    }

    setHintResult(null);
    setSelectedMoves((prev) => [...prev, { stepNo: prev.length + 1, x, y }]);
  };

  const handleUndoLast = () => {
    if (submitResult || viewMode !== "player") return;
    setHintResult(null);
    setSelectedMoves((prev) => prev.slice(0, -1));
  };

  const handleClearAll = () => {
    if (submitResult || viewMode !== "player") return;
    setHintResult(null);
    setSelectedMoves([]);
  };

  const handleHint = async () => {
    if (!roundData) {
      showNotify("warning", "No Round", "Generate a round first.");
      return;
    }

    if (selectedMoves.length === 0) {
      showNotify("warning", "No Moves Yet", "Make at least one move before requesting a hint.");
      return;
    }

    if (hintCooldown > 0) {
      showNotify("warning", "Hint Cooldown", `Please wait ${hintCooldown}s.`);
      return;
    }

    try {
      setLoadingHint(true);

      const response = await getKnightTourHint({
        sessionId: roundData.sessionId,
        moves: selectedMoves,
      });

      setHintResult(response);
      setHintCooldown(3);
      showNotify("success", "Hint Ready", response.message);
    } catch (error) {
      handleApiError(error, "Failed to get hint.");
    } finally {
      setLoadingHint(false);
    }
  };

  const handleSubmit = async () => {
    if (!roundData) {
      showNotify("warning", "No Round", "Generate a round first.");
      return;
    }

    if (selectedMoves.length === 0) {
      showNotify("warning", "No Moves", "Please select at least one move before submitting.");
      return;
    }

    try {
      setLoadingSubmit(true);

      const response = await submitKnightTourAnswer({
        sessionId: roundData.sessionId,
        moves: selectedMoves,
      });

      setSubmitResult(response);
      setHintResult(null);
      setViewMode("player");

      if (response.outcome === "WIN") {
        showNotify("success", "You Win!", response.message);
      } else if (response.outcome === "DRAW") {
        showNotify("warning", "Draw", response.message);
      } else {
        showNotify("error", "You Lose", response.message);
      }
    } catch (error) {
      handleApiError(error, "Failed to submit answer.");
    } finally {
      setLoadingSubmit(false);
    }
  };

  const handleToggleCorrectAnswer = async () => {
    if (!roundData) {
      showNotify("warning", "No Round", "Generate a round first.");
      return;
    }

    if (correctAnswer) {
      setCorrectAnswer(null);
      setViewMode("player");
      setIsPlaying(false);
      setPlaybackStep(0);
      showNotify("success", "Answer Hidden", "You can continue playing.");
      return;
    }

    try {
      setLoadingCorrectAnswer(true);
      const response = await getKnightTourCorrectAnswer(roundData.sessionId);
      setCorrectAnswer(response);

      if (response.warnsdorffMoves?.length > 0) {
        setViewMode("warnsdorff");
      } else if (response.backtrackingMoves?.length > 0) {
        setViewMode("backtracking");
      } else {
        setViewMode("player");
      }

      setPlaybackStep(0);
      showNotify("success", "Answer Loaded", response.message);
    } catch (error) {
      handleApiError(error, "Failed to load correct answer.");
    } finally {
      setLoadingCorrectAnswer(false);
    }
  };

  const displayedCorrectMoves =
    viewMode === "warnsdorff"
      ? warnsdorffMoves
      : viewMode === "backtracking"
      ? backtrackingMoves
      : [];

  const statusClass =
    submitResult?.outcome === "WIN"
      ? "text-green-300 border-green-500/20 bg-green-600/10"
      : submitResult?.outcome === "DRAW"
      ? "text-amber-300 border-amber-500/20 bg-amber-600/10"
      : submitResult?.outcome === "LOSE"
      ? "text-rose-300 border-rose-500/20 bg-rose-600/10"
      : "text-white/70 border-white/10 bg-white/5";

  const benchmarkChart =
    benchmarkData.length > 0 ? (
      <div className="rounded-xl border border-white/5 bg-[#0a0a0a] p-8">
        <PerformanceRunChart
          title="20-Round Performance Chart"
          data={benchmarkData}
          series={[
            {
              key: "warnsdorff",
              label: "Warnsdorff",
              colorClass: "bg-fuchsia-400",
              stroke: "#e879f9",
            },
            {
              key: "backtracking",
              label: "Backtracking",
              colorClass: "bg-cyan-400",
              stroke: "#22d3ee",
            },
          ]}
        />
      </div>
    ) : null;

  return (
    <AuthGate>
      <GameLayoutShell
        gameId="04"
        title="Knight's Tour"
        description="Build a complete knight path, validate it, and compare it with the generated algorithm solutions."
        controls={
          <div className="flex flex-col gap-4">
            <div className="rounded-xl border border-white/10 bg-white/5 p-4">
              <p className="mb-3 text-[10px] font-bold uppercase tracking-[0.28em] text-fuchsia-300">
                How to Play
              </p>
              <div className="space-y-2 font-mono text-[11px] leading-relaxed text-white/55">
                <p>1. Generate a round.</p>
                <p>2. Start from the given square.</p>
                <p>3. Move like a knight only.</p>
                <p>4. Visit every square once.</p>
                <p>5. Submit to get WIN / DRAW / LOSE.</p>
              </div>
            </div>

            <div className="space-y-3 rounded-xl border border-white/10 bg-white/5 p-4">
              <AlgoDropdown
                label="Board Size"
                value={String(boardSize)}
                onChange={handleBoardSizeChange}
                options={[
                  { value: "8", label: "8 × 8 Board" },
                  { value: "16", label: "16 × 16 Board" },
                ]}
              />

              <AlgoDropdown
                label="View Mode"
                value={viewMode}
                onChange={(value) =>
                  setViewMode(value as "player" | "warnsdorff" | "backtracking")
                }
                options={[
                  { value: "player", label: "Player Path" },
                  { value: "warnsdorff", label: "Warnsdorff View" },
                  { value: "backtracking", label: "Backtracking View" },
                ]}
              />

              <button
                onClick={handleStartRound}
                disabled={loadingStart || loadingBenchmark}
                className="w-full rounded-lg bg-white py-3 text-[10px] font-bold uppercase tracking-[0.2em] text-black transition hover:bg-fuchsia-600 hover:text-white disabled:opacity-30"
              >
                {loadingStart ? "Generating..." : "Generate Round"}
              </button>

              <button
                onClick={handleRun20Times}
                disabled={loadingStart || loadingBenchmark}
                className="w-full rounded-lg border border-cyan-500/30 bg-cyan-500/10 py-3 text-[10px] font-bold uppercase tracking-[0.2em] text-cyan-300 transition hover:bg-cyan-500/20 disabled:opacity-30"
              >
                {loadingBenchmark ? "RUNNING 20 TIMES..." : "RUN 20 TIMES"}
              </button>

              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={handleHint}
                  disabled={
                    !roundData ||
                    loadingHint ||
                    !!submitResult ||
                    hintCooldown > 0 ||
                    viewMode !== "player"
                  }
                  className="rounded-lg bg-amber-500 py-2.5 text-[10px] font-bold uppercase tracking-[0.16em] text-black transition hover:bg-amber-400 disabled:opacity-30"
                >
                  {loadingHint ? "Hint..." : hintCooldown > 0 ? `Hint ${hintCooldown}` : "Hint"}
                </button>

                <button
                  onClick={handleSubmit}
                  disabled={
                    !roundData ||
                    selectedMoves.length === 0 ||
                    loadingSubmit ||
                    !!submitResult ||
                    viewMode !== "player"
                  }
                  className="rounded-lg bg-green-500 py-2.5 text-[10px] font-bold uppercase tracking-[0.16em] text-black transition hover:bg-green-400 disabled:opacity-30"
                >
                  {loadingSubmit ? "Submit..." : "Submit"}
                </button>

                <button
                  onClick={handleUndoLast}
                  disabled={selectedMoves.length === 0 || !!submitResult || viewMode !== "player"}
                  className="rounded-lg bg-white/10 py-2.5 text-[10px] font-bold uppercase tracking-[0.16em] text-white transition hover:bg-white/20 disabled:opacity-30"
                >
                  Undo
                </button>

                <button
                  onClick={handleClearAll}
                  disabled={selectedMoves.length === 0 || !!submitResult || viewMode !== "player"}
                  className="rounded-lg bg-white/10 py-2.5 text-[10px] font-bold uppercase tracking-[0.16em] text-white transition hover:bg-white/20 disabled:opacity-30"
                >
                  Clear
                </button>
              </div>

              <button
                onClick={handleToggleCorrectAnswer}
                disabled={!roundData || loadingCorrectAnswer}
                className="w-full rounded-lg bg-cyan-500 py-2.5 text-[10px] font-bold uppercase tracking-[0.16em] text-black transition hover:bg-cyan-400 disabled:opacity-30"
              >
                {loadingCorrectAnswer ? "Loading..." : correctAnswer ? "Hide Answer" : "Show Answer"}
              </button>
            </div>

            {activePlaybackMoves.length > 0 && viewMode !== "player" && (
              <div className="space-y-3 rounded-xl border border-white/10 bg-white/5 p-4">
                <p className="text-[10px] font-bold uppercase tracking-[0.28em] text-cyan-400">
                  Playback
                </p>

                <div className="grid grid-cols-2 gap-2">
                  <button
                    onClick={() => setPlaybackStep((prev) => Math.max(prev - 1, 0))}
                    className="rounded-lg bg-white/10 py-2 text-[10px] font-bold uppercase tracking-widest hover:bg-white/20"
                  >
                    Prev
                  </button>
                  <button
                    onClick={() =>
                      setPlaybackStep((prev) => Math.min(prev + 1, activePlaybackMoves.length - 1))
                    }
                    className="rounded-lg bg-white/10 py-2 text-[10px] font-bold uppercase tracking-widest hover:bg-white/20"
                  >
                    Next
                  </button>
                  <button
                    onClick={() => setIsPlaying((prev) => !prev)}
                    className="rounded-lg bg-fuchsia-600 py-2 text-[10px] font-bold uppercase tracking-widest hover:bg-fuchsia-500"
                  >
                    {isPlaying ? "Pause" : "Play"}
                  </button>
                  <button
                    onClick={() => {
                      setIsPlaying(false);
                      setPlaybackStep(0);
                    }}
                    className="rounded-lg bg-white/10 py-2 text-[10px] font-bold uppercase tracking-widest hover:bg-white/20"
                  >
                    Reset
                  </button>
                </div>

                <div>
                  <label className="mb-2 block text-[10px] font-bold uppercase tracking-[0.18em] text-white/55">
                    Speed
                  </label>
                  <div className="grid grid-cols-3 gap-2">
                    {([0.5, 1, 2] as const).map((speed) => (
                      <button
                        key={speed}
                        onClick={() => setPlaybackSpeed(speed)}
                        className={`rounded-lg py-2 text-[10px] font-bold uppercase tracking-widest transition ${
                          playbackSpeed === speed
                            ? "bg-cyan-500 text-black"
                            : "bg-white/10 text-white hover:bg-white/20"
                        }`}
                      >
                        {speed}x
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        }
        visualization={
          roundData ? (
            <div className="custom-scrollbar flex h-full min-w-0 w-full flex-col gap-5 overflow-x-hidden overflow-y-auto pb-6 pr-2">
              <div className="min-w-0 rounded-2xl border border-white/5 bg-[#050505] p-4 shadow-2xl">
                <div className="mb-3 flex items-center justify-between px-1">
                  <p className="font-mono text-[10px] uppercase tracking-widest text-white/30">
                    Interactive Knight Board
                  </p>
                  <span className="rounded border border-purple-500/20 bg-purple-500/20 px-2 py-0.5 font-mono text-[9px] italic text-purple-300">
                    {viewMode === "player" ? "Manual Play" : `Playback: ${viewMode}`}
                  </span>
                </div>

                <div className="w-full overflow-x-auto">
                  <div className="min-w-max">
                    <KnightBoard
                      boardSize={boardSize}
                      selectedMoves={selectedMoves}
                      correctMoves={displayedCorrectMoves}
                      hintMoves={viewMode === "player" ? shownHintMoves : []}
                      onCellClick={handleCellClick}
                      disabled={!roundData || !!submitResult || viewMode !== "player"}
                      startX={roundData.startX}
                      startY={roundData.startY}
                      playbackMoves={activePlaybackMoves}
                      playbackStep={viewMode === "player" ? undefined : playbackStep}
                      showPlayerPath={viewMode === "player"}
                    />
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-5 xl:grid-cols-2">
                <AlgorithmComparisonChart metrics={roundData.algorithmMetrics} />

                <div className="rounded-2xl border border-white/5 bg-[#050505] p-4 shadow-2xl">
                  <p className="mb-3 text-[10px] font-bold uppercase tracking-[0.3em] text-green-500">
                    Round Info
                  </p>

                  <div className="space-y-3 font-mono text-[11px]">
                    <div className="flex items-center justify-between rounded-lg border border-white/5 bg-white/[0.03] p-3">
                      <span className="uppercase text-white/35">Session</span>
                      <span className="font-bold text-white/80">{roundData.sessionId}</span>
                    </div>
                    <div className="flex items-center justify-between rounded-lg border border-white/5 bg-white/[0.03] p-3">
                      <span className="uppercase text-white/35">Start</span>
                      <span className="font-bold text-white/80">
                        ({roundData.startX}, {roundData.startY})
                      </span>
                    </div>
                    <div className="flex items-center justify-between rounded-lg border border-white/5 bg-white/[0.03] p-3">
                      <span className="uppercase text-white/35">Expected</span>
                      <span className="font-bold text-white/80">{roundData.expectedMoveCount}</span>
                    </div>
                  </div>
                </div>
              </div>

              {benchmarkData.length > 0 && (
                <div className="rounded-2xl border border-white/10 bg-[#050505] p-5 shadow-2xl">
                  <p className="mb-4 text-[10px] font-bold uppercase tracking-[0.3em] text-cyan-400">
                    20-Run Benchmark Table
                  </p>

                  <div className="custom-scrollbar max-h-64 overflow-auto">
                    <table className="w-full text-left text-[11px] font-mono">
                      <thead className="border-b border-white/10 text-white/50">
                        <tr>
                          <th className="p-2">Round</th>
                          <th className="p-2">Warnsdorff (ms)</th>
                          <th className="p-2">Backtracking (ms)</th>
                        </tr>
                      </thead>
                      <tbody>
                        {benchmarkData.map((row) => (
                          <tr key={row.round} className="border-b border-white/5">
                            <td className="p-2 text-white/75">{row.round}</td>
                            <td className="p-2 text-fuchsia-300">{row.warnsdorff.toFixed(3)}</td>
                            <td className="p-2 text-cyan-300">{row.backtracking.toFixed(3)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {(warnsdorffMoves.length > 0 || backtrackingMoves.length > 0) && (
                <div className="grid grid-cols-1 items-start gap-5 2xl:grid-cols-2">
                  <div className="min-w-0 rounded-2xl border border-white/10 bg-white/5 p-3 shadow-xl backdrop-blur">
                    <h3 className="mb-2 text-sm font-semibold text-white">Warnsdorff Solution</h3>

                    <div className="w-full overflow-x-auto">
                      <div className="min-w-max">
                        <KnightBoard
                          boardSize={boardSize}
                          selectedMoves={[]}
                          correctMoves={warnsdorffMoves}
                          hintMoves={[]}
                          onCellClick={() => {}}
                          disabled
                          startX={roundData.startX}
                          startY={roundData.startY}
                          compact
                        />
                      </div>
                    </div>
                  </div>

                  <div className="min-w-0 rounded-2xl border border-white/10 bg-white/5 p-3 shadow-xl backdrop-blur">
                    <h3 className="mb-2 text-sm font-semibold text-white">Backtracking Solution</h3>

                    <div className="w-full overflow-x-auto">
                      <div className="min-w-max">
                        <KnightBoard
                          boardSize={boardSize}
                          selectedMoves={[]}
                          correctMoves={backtrackingMoves}
                          hintMoves={[]}
                          onCellClick={() => {}}
                          disabled
                          startX={roundData.startX}
                          startY={roundData.startY}
                          compact
                        />
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="flex h-full flex-col items-center justify-center gap-4 text-white/10">
              <motion.div
                animate={{ scale: [1, 1.08, 1] }}
                transition={{ repeat: Infinity, duration: 2 }}
              >
                <svg className="h-16 w-16" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={0.5}
                    d="M16 4c.88 0 1.67.39 2.22 1A2.99 2.99 0 0121 8v2c0 1.1-.9 2-2 2h-1l1 6H5l1-6H5c-1.1 0-2-.9-2-2V8c0-1.2.7-2.25 1.72-2.74A2.99 2.99 0 017 4h9z"
                  />
                </svg>
              </motion.div>
              <p className="font-mono text-[10px] uppercase tracking-[0.45em]">
                Knight Processor Idle
              </p>
            </div>
          )
        }
        results={
          <div className="flex h-full flex-col justify-start space-y-3 pt-1">
            <div className={`flex items-center justify-between rounded-lg border p-3 ${statusClass}`}>
              <span className="font-mono text-[10px] font-bold uppercase tracking-widest">
                Status
              </span>
              <span className="text-[10px] font-bold">
                {submitResult ? submitResult.outcome : "Waiting"}
              </span>
            </div>

            <div className="flex items-center justify-between rounded-lg border border-white/5 bg-white/5 p-3 shadow-inner">
              <span className="font-mono text-[10px] uppercase text-white/40">Moves</span>
              <span className="text-lg font-black text-white">{selectedMoves.length}</span>
            </div>

            {roundData && (
              <div className="rounded-lg border border-cyan-500/20 bg-cyan-500/10 p-3">
                <p className="mb-2 text-[10px] font-bold uppercase tracking-[0.18em] text-cyan-300">
                  Current Algorithm Times
                </p>
                <div className="space-y-1 text-[11px] font-mono text-white/80">
                  <p>
                    Warnsdorff:{" "}
                    {findMetricTime(roundData.algorithmMetrics, ["warnsdorff"]).toFixed(3)} ms
                  </p>
                  <p>
                    Backtracking:{" "}
                    {findMetricTime(roundData.algorithmMetrics, ["backtracking"]).toFixed(3)} ms
                  </p>
                </div>
              </div>
            )}

            {benchmarkData.length > 0 && (
              <div className="rounded-lg border border-cyan-500/20 bg-cyan-500/10 p-3">
                <p className="mb-2 text-[10px] font-bold uppercase tracking-[0.18em] text-cyan-300">
                  20-Run Summary
                </p>
                <div className="space-y-1 text-[11px] font-mono text-white/80">
                  <p>
                    Avg Warnsdorff:{" "}
                    {(
                      benchmarkData.reduce((sum, row) => sum + row.warnsdorff, 0) /
                      benchmarkData.length
                    ).toFixed(3)}{" "}
                    ms
                  </p>
                  <p>
                    Avg Backtracking:{" "}
                    {(
                      benchmarkData.reduce((sum, row) => sum + row.backtracking, 0) /
                      benchmarkData.length
                    ).toFixed(3)}{" "}
                    ms
                  </p>
                </div>
              </div>
            )}

            {submitResult && (
              <div className="space-y-2 pt-1">
                <div className="flex items-center justify-between rounded border border-white/5 bg-white/[0.03] p-3">
                  <span className="font-mono text-[10px] uppercase text-white/35">Reason</span>
                  <span className="text-right text-[11px] font-bold text-white/80">
                    {getReasonLabel(submitResult.reasonCode)}
                  </span>
                </div>

                <div className="flex items-center justify-between rounded border border-white/5 bg-white/[0.03] p-3">
                  <span className="font-mono text-[10px] uppercase text-white/35">Structural</span>
                  <span className="text-[11px] font-bold text-white/80">
                    {submitResult.structuralValid ? "Valid" : "Invalid"}
                  </span>
                </div>
              </div>
            )}
          </div>
        }
        bottomSection={benchmarkChart}
      />

      <AlgoNotify
        show={notify.show}
        type={notify.type}
        title={notify.title}
        message={notify.message}
        onClose={() => setNotify((prev) => ({ ...prev, show: false }))}
      />
    </AuthGate>
  );
}
