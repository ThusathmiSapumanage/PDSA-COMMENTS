"use client";

import { useEffect, useRef, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import GameLayoutShell from "../../../components/layouts/GameLayoutShell";
import AuthGate from "../../../components/auth/AuthGate";
import AlgoNotify, { NotifyType } from "../../../components/forms/AlgoNotify";
import MinCostAnalytics from "../../../components/mincost/MinCostAnalytics";
import PlaybackToolbar from "../../../components/mincost/PlaybackToolbar";
import AlgoDropdown from "../../../components/forms/AlgoDropdown";
import PerformanceRunChart from "../../../components/common/PerformanceRunChart";
import { getApiBaseUrl } from "../../../services/api";

type Assignment = {
  row: number;
  col: number;
  cost: number;
};

type HungarianStep = {
  type: string;
  matrix: number[][];
  description: string;
  rowMinVals: number[] | null;
  colMinVals: number[] | null;
  markedRows: boolean[] | null;
  markedCols: boolean[] | null;
  delta: number | null;
};

type ApiResponse = {
  matrix: number[][];
  greedyTotalCost: number;
  hungarianTotalCost: number;
  greedyAssignments: Assignment[];
  hungarianAssignments: Assignment[];
  greedyLogs: string[];
  hungarianLogs: string[];
  hungarianSteps: HungarianStep[];
  greedyTimeMs: number;
  hungarianTimeMs: number;
};

type HistoryItem = {
  date: string;
  n: number;
  cost: number;
  greedyTime: number;
  hungarianTime: number;
};

type BenchmarkRow = {
  round: number;
  n: number;
  greedy: number;
  hungarian: number;
};

function MatrixTable({
  title,
  matrix,
  assignments,
  currentStep,
  isGreedy,
  hungarianStep,
}: {
  title: string;
  matrix: number[][];
  assignments: Assignment[];
  currentStep?: number;
  isGreedy?: boolean;
  hungarianStep?: HungarianStep;
}) {
  const activeStep = currentStep !== undefined ? currentStep : assignments.length - 1;
  const showStepByStep = currentStep !== undefined;
  const displayMatrix = hungarianStep?.matrix || matrix;

  const blockedRows = new Set<number>();
  const blockedCols = new Set<number>();

  if (showStepByStep && isGreedy) {
    for (let i = 0; i < activeStep; i++) {
      if (assignments[i]) {
        blockedRows.add(assignments[i].row);
        blockedCols.add(assignments[i].col);
      }
    }
  }

  const assignmentMap = new Map<string, boolean>();
  if (!hungarianStep || hungarianStep.type === "INITIAL" || !showStepByStep) {
    assignments.slice(0, activeStep + 1).forEach((a) => {
      assignmentMap.set(`${a.row}-${a.col}`, true);
    });
  }

  return (
    <div className="w-full">
      <div className="mb-2 flex items-center justify-between px-1">
        <p className="font-mono text-[10px] uppercase tracking-widest text-white/30">{title}</p>
        <AnimatePresence>
          {hungarianStep && (
            <motion.span
              initial={{ opacity: 0, x: 10 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0 }}
              className="rounded border border-blue-500/20 bg-blue-500/20 px-2 py-0.5 font-mono text-[9px] italic text-blue-300"
            >
              {hungarianStep.type.replace("_", " ")}
            </motion.span>
          )}
        </AnimatePresence>
      </div>

      <div className="custom-scrollbar relative max-h-[420px] overflow-auto rounded-xl border border-white/5 bg-[#030303] shadow-2xl">
        <table className="relative z-10 w-full border-collapse text-[10px]">
          <tbody>
            {displayMatrix.map((row, rowIndex) => (
              <tr key={rowIndex}>
                {row.map((value, colIndex) => {
                  const cellKey = `${rowIndex}-${colIndex}`;
                  const isAssigned = assignmentMap.has(cellKey);
                  const isCurrentPick =
                    showStepByStep &&
                    !hungarianStep &&
                    assignments[activeStep]?.row === rowIndex &&
                    assignments[activeStep]?.col === colIndex;
                  const isBlocked =
                    showStepByStep &&
                    isGreedy &&
                    (blockedRows.has(rowIndex) || blockedCols.has(colIndex));
                  const isReduced =
                    hungarianStep &&
                    (hungarianStep.markedRows?.[rowIndex] || hungarianStep.markedCols?.[colIndex]);
                  const isZero = value === 0;

                  let cellClass =
                    "border border-white/3 p-2 text-center transition-all duration-300 ";

                  if (isCurrentPick) {
                    cellClass +=
                      "relative z-20 scale-110 bg-purple-500/40 font-bold text-purple-200 shadow-lg shadow-purple-500/30";
                  } else if (isAssigned) {
                    cellClass += "bg-green-500/20 font-medium text-green-300";
                  } else if (isBlocked) {
                    cellClass += "text-white/20 opacity-10";
                  } else if (isReduced) {
                    cellClass += "bg-blue-500/10 text-blue-200/50";
                  } else if (isZero && hungarianStep) {
                    cellClass += "font-bold text-green-500/80";
                  } else {
                    cellClass += "text-white/40";
                  }

                  return (
                    <motion.td
                      key={cellKey}
                      className={cellClass}
                      layout
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                    >
                      {value}
                    </motion.td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>

        {hungarianStep?.delta && (
          <div className="pointer-events-none absolute inset-0 z-30 flex items-center justify-center">
            <motion.div
              initial={{ scale: 0.5, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              className="rounded-full border border-white/20 bg-purple-600 px-4 py-2 text-[10px] font-bold text-white shadow-2xl"
            >
              DELTA ADJUST: {hungarianStep.delta}
            </motion.div>
          </div>
        )}
      </div>
    </div>
  );
}

export default function MinimumCostPage() {
  const MIN_N = 50;
  const MAX_N = 100;

  const [n, setN] = useState(() => String(MIN_N));
  const [selectedAlgo, setSelectedAlgo] = useState<"greedy" | "hungarian">("greedy");
  const [isLoading, setIsLoading] = useState(false);
  const [isBenchmarking, setIsBenchmarking] = useState(false);
  const [isRollingN, setIsRollingN] = useState(false);
  const [result, setResult] = useState<ApiResponse | null>(null);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [benchmarkData, setBenchmarkData] = useState<BenchmarkRow[]>([]);
  const [currentStep, setCurrentStep] = useState<number | undefined>(undefined);
  const [isPlaying, setIsPlaying] = useState(false);
  const [speed, setSpeed] = useState(1);
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  const rollingTimerRef = useRef<NodeJS.Timeout | null>(null);

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

  useEffect(() => {
    if (result) {
      setCurrentStep(undefined);
      setIsPlaying(false);
    }
    fetchHistory();
  }, [result]);

  useEffect(() => {
    return () => {
      if (rollingTimerRef.current) {
        clearInterval(rollingTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    const initialRandomN = Math.floor(Math.random() * (MAX_N - MIN_N + 1)) + MIN_N;
    setN(String(initialRandomN));
  }, [MAX_N, MIN_N]);

  useEffect(() => {
    if (isPlaying && result) {
      const maxSteps =
        selectedAlgo === "greedy"
          ? result.greedyAssignments.length
          : result.hungarianSteps.length;

      timerRef.current = setInterval(() => {
        setCurrentStep((prev) => {
          const next = prev === undefined ? 0 : prev + 1;
          if (next >= maxSteps) {
            setIsPlaying(false);
            return prev;
          }
          return next;
        });
      }, 1000 / speed);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
    }

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isPlaying, result, speed, selectedAlgo]);

  const fetchHistory = async () => {
    try {
      const response = await fetch(`${getApiBaseUrl()}/mincost/history`);
      if (!response.ok) return;
      const data = await response.json();
      setHistory(data);
    } catch {
      console.log("History failed to load");
    }
  };

  const validateInputs = () => {
    const nValue = Number(n);

    if (!Number.isInteger(nValue) || nValue < MIN_N || nValue > MAX_N) {
      setNotify({
        show: true,
        type: "error",
        title: "Input Error",
        message: `Enter a valid N value between ${MIN_N} and ${MAX_N}.`,
      });
      return null;
    }

    return { nValue };
  };

  const callStartApi = async () => {
    const validated = validateInputs();
    if (!validated) return null;

    const response = await fetch(`${getApiBaseUrl()}/mincost/start`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        n: validated.nValue,
      }),
    });

    const raw = await response.text();
    const data = raw ? JSON.parse(raw) : null;

    if (!response.ok) {
      const backendMessage =
        data && typeof data === "object"
          ? (data as { error?: string; message?: string }).error ??
            (data as { error?: string; message?: string }).message
          : null;
      throw new Error(backendMessage || `Request failed (${response.status})`);
    }

    if (!data) throw new Error("Server returned an empty response.");
    return data as ApiResponse;
  };

  const clearBoardForRetry = () => {
    setIsPlaying(false);
    setCurrentStep(undefined);
    setResult(null);
    setBenchmarkData([]);
  };

  const runGame = async () => {
    if (!validateInputs()) return;

    clearBoardForRetry();
    setIsLoading(true);
    try {
      const data = await callStartApi();
      if (!data) return;
      setResult(data);
      setNotify({
        show: true,
        type: "success",
        title: "Calculated!",
        message: "Explore the steps using the playback toolbar.",
      });
    } catch (error) {
      setNotify({
        show: true,
        type: "error",
        title: "Server Error",
        message: error instanceof Error ? error.message : "Backend offline.",
      });
    } finally {
      setIsLoading(false);
    }
  };

  const runBenchmark20 = async () => {
    if (!validateInputs()) return;

    clearBoardForRetry();
    setIsBenchmarking(true);
    const runs: BenchmarkRow[] = [];

    try {
      for (let i = 1; i <= 20; i++) {
        const data = await callStartApi();
        if (!data) continue;

        runs.push({
          round: i,
          n: data.matrix.length,
          greedy: Number(data.greedyTimeMs ?? 0),
          hungarian: Number(data.hungarianTimeMs ?? 0),
        });

        setResult(data);
        setBenchmarkData([...runs]);
      }

      setNotify({
        show: true,
        type: "success",
        title: "20 Runs Completed",
        message: "Benchmark chart generated for Greedy and Hungarian algorithms.",
      });
    } catch (error) {
      setNotify({
        show: true,
        type: "error",
        title: "Benchmark Failed",
        message: error instanceof Error ? error.message : "Failed during 20-run test.",
      });
    } finally {
      setIsBenchmarking(false);
    }
  };

  const getRandomN = () =>
    Math.floor(Math.random() * (MAX_N - MIN_N + 1)) + MIN_N;

  const spinRandomN = () => {
    if (isLoading || isBenchmarking || isRollingN) return;

    clearBoardForRetry();
    setIsRollingN(true);

    const initialValue = Number(n);

    let tickCount = 0;
    const totalTicks = 22;
    let latestValue = getRandomN();
    setN(String(latestValue));

    if (rollingTimerRef.current) {
      clearInterval(rollingTimerRef.current);
    }

    rollingTimerRef.current = setInterval(() => {
      tickCount += 1;
      latestValue = getRandomN();
      setN(String(latestValue));

      if (tickCount >= totalTicks) {
        if (rollingTimerRef.current) {
          clearInterval(rollingTimerRef.current);
          rollingTimerRef.current = null;
        }

        if (Number.isFinite(initialValue) && latestValue === initialValue) {
          let attempts = 0;
          while (attempts < 10 && latestValue === initialValue) {
            latestValue = getRandomN();
            attempts += 1;
          }
        }

        setN(String(latestValue));
        setIsRollingN(false);
      }
    }, 50);
  };

  const maxSteps = result
    ? selectedAlgo === "greedy"
      ? result.greedyAssignments.length
      : result.hungarianSteps.length
    : 0;

  const displayN = n.padStart(3, "0");

  const benchmarkChart =
    benchmarkData.length > 0 ? (
      <div className="rounded-xl border border-white/5 bg-[#0a0a0a] p-8">
        <PerformanceRunChart
          title="20-Round Performance Chart"
          data={benchmarkData}
          series={[
            {
              key: "greedy",
              label: "Greedy",
              colorClass: "bg-blue-400",
              stroke: "#60a5fa",
            },
            {
              key: "hungarian",
              label: "Hungarian",
              colorClass: "bg-emerald-400",
              stroke: "#34d399",
            },
          ]}
        />
      </div>
    ) : null;

  return (
    <AuthGate>
      <GameLayoutShell
        gameId="01"
        title="MinCost Assignment"
        description="Optimize task-to-employee allocation using Greedy and Hungarian algorithms."
        controls={
          <div className="flex flex-col gap-6">
            <div className="space-y-4">
              <div className="rounded-lg border border-blue-500/20 bg-blue-600/10 px-3 py-3">
                <p className="font-mono text-[10px] uppercase tracking-widest text-blue-300">
                  Coursework Rules
                </p>
                <p className="mt-1 text-[11px] text-blue-100/90">
                  N is randomized between 50 and 100 using the Randomize N button. For each round, the backend generates matrix costs randomly from 20 to 200.
                </p>
              </div>

              <div className="rounded-lg border border-white/10 bg-white/5 px-3 py-3">
                <p className="font-mono text-[10px] uppercase tracking-widest text-white/50">
                  Number of Tasks / Employees (N)
                </p>
                <div className="relative mt-2 overflow-hidden rounded-md border border-cyan-500/20 bg-[#020508] px-4 py-3 text-center font-mono text-2xl font-bold text-cyan-100">
                  <motion.span
                    className="relative z-10 inline-block w-[3ch] tabular-nums tracking-[0.18em]"
                    animate={
                      isRollingN
                        ? {
                            opacity: [0.7, 1, 0.75, 1],
                            scale: [1, 1.04, 0.98, 1],
                            textShadow: [
                              "0 0 6px rgba(34,211,238,0.45)",
                              "0 0 18px rgba(34,211,238,0.8)",
                              "0 0 8px rgba(34,211,238,0.55)",
                            ],
                          }
                        : {
                            opacity: 1,
                            scale: 1,
                            textShadow: "0 0 8px rgba(34,211,238,0.35)",
                          }
                    }
                    transition={isRollingN ? { duration: 0.18, repeat: Infinity, ease: "linear" } : { duration: 0.2 }}
                  >
                    {displayN}
                  </motion.span>
                </div>
              </div>

              <div className="-mt-2">
                <button
                  onClick={spinRandomN}
                  disabled={isLoading || isBenchmarking || isRollingN}
                  className="w-full rounded-lg border border-amber-500/30 bg-amber-500/10 py-3 text-[10px] font-bold uppercase tracking-[0.2em] text-amber-300 transition-all hover:bg-amber-500/20 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  {isRollingN ? "SPINNING..." : "RANDOMIZE N"}
                </button>
              </div>

              <button
                onClick={runGame}
                disabled={isLoading || isBenchmarking || isRollingN}
                className="w-full rounded-lg bg-white py-4 text-[10px] font-bold uppercase tracking-[0.2em] text-black shadow-lg transition-all hover:bg-blue-600 hover:text-white disabled:opacity-30"
              >
                {isLoading ? "CALCULATING..." : "GENERATE & RUN"}
              </button>

              <button
                onClick={runBenchmark20}
                disabled={isLoading || isBenchmarking || isRollingN}
                className="w-full rounded-lg border border-cyan-500/30 bg-cyan-500/10 py-3 text-[10px] font-bold uppercase tracking-[0.2em] text-cyan-300 transition-all hover:bg-cyan-500/20 disabled:opacity-30"
              >
                {isBenchmarking ? "RUNNING 20 TIMES..." : "RUN 20 TIMES"}
              </button>
            </div>

            {result && (
              <motion.div
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                className="space-y-6 border-t border-white/5 pt-6"
              >
                <AlgoDropdown
                  label="Visualize Algorithm"
                  value={selectedAlgo}
                  onChange={(val) => {
                    setSelectedAlgo(val as "greedy" | "hungarian");
                    setCurrentStep(undefined);
                    setIsPlaying(false);
                  }}
                  options={[
                    { value: "greedy", label: "Greedy Strategy" },
                    { value: "hungarian", label: "Hungarian (Optimal)" },
                  ]}
                />

                <PlaybackToolbar
                  currentStep={currentStep === undefined ? -1 : currentStep}
                  totalSteps={maxSteps}
                  isPlaying={isPlaying}
                  speed={speed}
                  onNext={() => {
                    setIsPlaying(false);
                    setCurrentStep((prev) =>
                      prev === undefined ? 0 : Math.min(prev + 1, maxSteps - 1)
                    );
                  }}
                  onPrev={() => {
                    setIsPlaying(false);
                    setCurrentStep((prev) =>
                      prev === undefined || prev === 0 ? 0 : prev - 1
                    );
                  }}
                  onPlayToggle={() => setIsPlaying(!isPlaying)}
                  onStop={() => {
                    setIsPlaying(false);
                    setCurrentStep(undefined);
                  }}
                  onSpeedChange={setSpeed}
                />
              </motion.div>
            )}
          </div>
        }
        visualization={
          result ? (
            <div className="custom-scrollbar flex h-full w-full flex-col gap-8 overflow-auto pb-10 pr-2">
              <div className="grid grid-cols-1 gap-8">
                <AnimatePresence mode="wait">
                  {selectedAlgo === "greedy" ? (
                    <motion.div
                      key="greedy-view"
                      initial={{ opacity: 0, scale: 0.95 }}
                      animate={{ opacity: 1, scale: 1 }}
                      exit={{ opacity: 0, scale: 0.95 }}
                    >
                      <MatrixTable
                        title={`Greedy Strategy: ${
                          currentStep !== undefined
                            ? `Choosing Step ${currentStep + 1}`
                            : "Final Assignments"
                        }`}
                        matrix={result.matrix}
                        assignments={result.greedyAssignments}
                        currentStep={currentStep}
                        isGreedy
                      />
                    </motion.div>
                  ) : (
                    <motion.div
                      key="hungarian-view"
                      initial={{ opacity: 0, scale: 0.95 }}
                      animate={{ opacity: 1, scale: 1 }}
                      exit={{ opacity: 0, scale: 0.95 }}
                    >
                      <MatrixTable
                        title={`Hungarian Optimization: ${
                          currentStep !== undefined
                            ? `Phase: ${result.hungarianSteps[currentStep].type}`
                            : "Final Result"
                        }`}
                        matrix={result.matrix}
                        assignments={result.hungarianAssignments}
                        currentStep={currentStep}
                        hungarianStep={
                          currentStep !== undefined
                            ? result.hungarianSteps[currentStep]
                            : undefined
                        }
                      />
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>

              <div className="grid grid-cols-1 gap-8 xl:grid-cols-2">
                <div className="rounded-2xl border border-white/5 bg-[#050505] p-6 shadow-2xl">
                  <p className="mb-4 text-[10px] font-bold uppercase tracking-[0.3em] text-blue-500">
                    Execution Steps
                  </p>
                  <div className="custom-scrollbar max-h-64 space-y-1 overflow-auto text-[11px] font-mono">
                    {selectedAlgo === "greedy"
                      ? result.greedyLogs.map((line, index) => (
                          <div
                            key={index}
                            onClick={() => {
                              setIsPlaying(false);
                              setCurrentStep(index);
                            }}
                            className={`cursor-pointer rounded border border-transparent p-2 transition-all ${
                              currentStep === index
                                ? "border-blue-500/30 bg-blue-600/20 font-bold text-blue-200"
                                : "text-white/40 hover:bg-white/5 hover:text-white/60"
                            }`}
                          >
                            <span className="mr-2 opacity-30">[{index + 1}]</span>
                            {line}
                          </div>
                        ))
                      : result.hungarianSteps.map((step, index) => (
                          <div
                            key={index}
                            onClick={() => {
                              setIsPlaying(false);
                              setCurrentStep(index);
                            }}
                            className={`cursor-pointer rounded border border-transparent p-2 transition-all ${
                              currentStep === index
                                ? "border-purple-500/30 bg-purple-600/20 font-bold text-purple-200"
                                : "text-white/40 hover:bg-white/5 hover:text-white/60"
                            }`}
                          >
                            <span className="mr-2 opacity-30">[{index + 1}]</span>
                            {step.description}
                          </div>
                        ))}
                  </div>
                </div>

                <div className="group relative overflow-hidden rounded-2xl border border-white/5 bg-[#050505] p-6 shadow-2xl">
                  <div className="absolute right-0 top-0 p-4 opacity-10 transition-opacity group-hover:opacity-20">
                    <svg className="h-12 w-12 text-green-500" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" />
                    </svg>
                  </div>

                  <p className="mb-4 text-[10px] font-bold uppercase tracking-[0.3em] text-green-500">
                    Algorithm Logic
                  </p>

                  <div className="space-y-4 font-mono text-[11px] leading-relaxed text-white/40">
                    <div className="rounded border border-white/5 bg-white/3 p-3">
                      <p className="mb-1 font-bold text-white/70">Greedy Strategy</p>
                      <p>
                        Greedy repeatedly picks the smallest currently available cost while
                        making sure each task and employee is used only once.
                      </p>
                    </div>

                    <div className="rounded border border-white/5 bg-white/3 p-3">
                      <p className="mb-1 font-bold text-white/70">Hungarian Algorithm</p>
                      <p>
                        Hungarian performs row and column reduction, covers zeros, and
                        adjusts the matrix to find the optimal assignment.
                      </p>
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
                          <th className="p-2">N</th>
                          <th className="p-2">Greedy (ms)</th>
                          <th className="p-2">Hungarian (ms)</th>
                        </tr>
                      </thead>
                      <tbody>
                        {benchmarkData.map((row) => (
                          <tr key={row.round} className="border-b border-white/5">
                            <td className="p-2 text-white/75">{row.round}</td>
                            <td className="p-2 text-white/75">{row.n}</td>
                            <td className="p-2 text-blue-300">{row.greedy.toFixed(3)}</td>
                            <td className="p-2 text-emerald-300">{row.hungarian.toFixed(3)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="flex h-full flex-col items-center justify-center gap-4 text-white/10">
              <motion.div animate={{ scale: [1, 1.1, 1] }} transition={{ repeat: Infinity, duration: 2 }}>
                <svg className="h-20 w-20" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={0.5}
                    d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
                  />
                </svg>
              </motion.div>
              <p className="font-mono text-[10px] uppercase tracking-[0.5em]">
                Algorithm Processor Idle
              </p>
            </div>
          )
        }
        results={
          <div className="space-y-4">
            <div className="flex items-center justify-between rounded-lg border border-white/5 bg-white/5 p-4 shadow-inner">
              <span className="font-mono text-[10px] uppercase text-white/40">Greedy Total</span>
              <span className="text-xl font-black text-white">
                {result?.greedyTotalCost ?? "--"}
              </span>
            </div>

            <div className="flex items-center justify-between rounded-lg border border-white/5 bg-white/5 p-4 shadow-inner">
              <span className="font-mono text-[10px] uppercase text-white/40">
                Hungarian Total
              </span>
              <span className="text-xl font-black text-green-400">
                {result?.hungarianTotalCost ?? "--"}
              </span>
            </div>

            <div className="flex items-center justify-between rounded-lg border border-green-500/20 bg-green-600/10 p-3">
              <span className="font-mono text-[10px] font-bold uppercase tracking-widest text-green-400">
                Status
              </span>
                <span>
                  {result
                    ? result.hungarianTotalCost < result.greedyTotalCost
                      ? "Hungarian Wins!"
                      : result.hungarianTotalCost > result.greedyTotalCost
                      ? "Greedy Wins!"
                      : "Perfect Match!"
                    : "Waiting..."}
                </span>
            </div>

            <div className="grid grid-cols-2 gap-2 pt-4">
              <div className="rounded-lg border border-blue-500/20 bg-blue-600/10 p-3 text-center">
                <p className="mb-1 text-[9px] font-bold uppercase text-blue-400">
                  Greedy Time
                </p>
                <p className="text-xs font-mono text-white">
                  {result?.greedyTimeMs ?? "--"}ms
                </p>
              </div>

              <div className="rounded-lg border border-purple-500/20 bg-purple-600/10 p-3 text-center">
                <p className="mb-1 text-[9px] font-bold uppercase text-purple-400">
                  Hungarian Time
                </p>
                <p className="text-xs font-mono text-white">
                  {result?.hungarianTimeMs ?? "--"}ms
                </p>
              </div>
            </div>

            {benchmarkData.length > 0 && (
              <div className="mt-6 rounded-lg border border-cyan-500/20 bg-cyan-500/10 p-3">
                <p className="mb-2 text-[10px] font-bold uppercase tracking-[0.18em] text-cyan-300">
                  20-Run Summary
                </p>
                <div className="space-y-1 text-[11px] font-mono text-white/75">
                  <p>
                    Avg Greedy:{" "}
                    {(
                      benchmarkData.reduce((sum, row) => sum + row.greedy, 0) /
                      benchmarkData.length
                    ).toFixed(3)}{" "}
                    ms
                  </p>
                  <p>
                    Avg Hungarian:{" "}
                    {(
                      benchmarkData.reduce((sum, row) => sum + row.hungarian, 0) /
                      benchmarkData.length
                    ).toFixed(3)}{" "}
                    ms
                  </p>
                </div>
              </div>
            )}

            {history.length > 0 && (
              <div className="mt-8 border-t border-white/5 pt-8">
                <p className="mb-4 font-mono text-[10px] uppercase tracking-widest text-white/30">
                  Previous Runs
                </p>
                <div className="custom-scrollbar max-h-40 space-y-2 overflow-auto pr-2">
                  {history.slice(0, 5).map((item, idx) => (
                    <div
                      key={idx}
                      className="flex items-center justify-between rounded border border-white/5 bg-white/[0.02] p-2 transition-colors hover:bg-white/5"
                    >
                      <div className="flex flex-col">
                        <span className="font-mono text-[8px] italic text-white/20">
                          {new Date(item.date).toLocaleTimeString()}
                        </span>
                        <span className="text-[10px] text-white/60">
                          Matrix {item.n}x{item.n}
                        </span>
                      </div>
                      <span className="text-xs font-bold text-green-400/70">{item.cost}</span>
                    </div>
                  ))}
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
