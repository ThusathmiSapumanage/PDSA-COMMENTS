"use client";

import { useState, useCallback } from "react";
import GameLayoutShell from "../../../components/layouts/GameLayoutShell";
import AuthGate from "../../../components/auth/AuthGate";
import { useAuth } from "../../../components/auth/AuthProvider";
import AlgoDropdown from "../../../components/forms/AlgoDropdown";
import {
  SnakeLadderBoard,
} from "../../../components/snake-ladder";
import ResultsPanel from "../../../components/snake-ladder/ResultsPanel";
import AnswerChallenge from "../../../components/snake-ladder/AnswerChallenge";
import PerformanceRunChart from "../../../components/snake-ladder/PerformanceRunChart";
import type { GameState } from "../../../components/snake-ladder/types";
import { parseApiError, getBackendOrigin } from "../../../services/api";

// ─── Helpers ──────────────────────────────────────────────────────────────

const BACKEND_URL = getBackendOrigin();
const BOARD_SIZE_OPTIONS = [6, 7, 8, 9, 10, 11, 12];
const BOARD_SIZE_DROPDOWN_OPTIONS = BOARD_SIZE_OPTIONS.map((n) => ({
  value: String(n),
  label: `${n}×${n}`,
}));

type PlayerProfile = {
  playerId?: number;
  playerEmail?: string;
};

type StartRoundData = GameState & {
  sessionId: number;
};

type BenchmarkRow = {
  round: number;
  n: number;
  bfs: number;
  dp: number;
  minimumThrows: number;
};

async function resolvePlayerId(email: string, token: string): Promise<number> {
  const res = await fetch(`${BACKEND_URL}/api/players`, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!res.ok) {
    throw new Error(await parseApiError(res));
  }

  const players = (await res.json()) as PlayerProfile[];
  const normalizedEmail = email.trim().toLowerCase();
  const currentPlayer = Array.isArray(players)
    ? players.find(
        (player) => player.playerEmail?.trim().toLowerCase() === normalizedEmail
      )
    : null;

  if (!currentPlayer?.playerId) {
    throw new Error("Player account not found for the current login.");
  }

  return currentPlayer.playerId;
}

async function startRound(n: number, playerId: number, token: string) {
  const res = await fetch(`${BACKEND_URL}/api/snake-ladder/start`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ n, playerId }),
  });
  if (!res.ok) throw new Error(await parseApiError(res));
  return res.json();          // { items, algorithmResults, choices, n, sessionId }
}

async function submitAnswer(sessionId: number, playerAnswer: number, token: string) {
  const res = await fetch(`${BACKEND_URL}/api/snake-ladder/answer`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ sessionId, playerAnswer }),
  });
  if (!res.ok) throw new Error(await parseApiError(res));
  return res.json();          // { outcome: "WIN" | "LOSE" | "DRAW", message: string }
}

async function cleanupBenchmarkSession(data: StartRoundData, token: string) {
  const correctAnswer = getMinimumThrows(data);
  await submitAnswer(data.sessionId, correctAnswer, token);
}

function getAlgorithmTime(data: StartRoundData, algorithmNameSubstring: string) {
  const result = data.algorithmResults.find(
    (item) => item.algorithmName.toLowerCase().includes(algorithmNameSubstring.toLowerCase())
  );

  return Number(result?.executionTimeMs ?? 0);
}

function getMinimumThrows(data: StartRoundData) {
  return Number(data.algorithmResults[0]?.minimumThrows ?? 0);
}

// ─── Page ────────────────────────────────────────────────────────────────────

export default function SnakeLadderPage() {
  const [algorithm, setAlgorithm]   = useState("bfs");
  const [boardSize, setBoardSize]   = useState(8);
  const [loading, setLoading]       = useState(false);
  const [error, setError]           = useState<string | null>(null);
  const [gameState, setGameState]   = useState<GameState | null>(null);
  const [outcome, setOutcome]       = useState<"WIN" | "LOSE" | "DRAW" | null>(null);
  const [outcomeMessage, setOutcomeMessage] = useState<string | null>(null);
  const [selectedChoice, setSelectedChoice] = useState<number | null>(null);
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [benchmarking, setBenchmarking] = useState(false);
  const [benchmarkMode, setBenchmarkMode] = useState(false);
  const [benchmarkData, setBenchmarkData] = useState<BenchmarkRow[]>([]);
  const { accessToken, userEmail } = useAuth();

  const handleStart = useCallback(async () => {
    setLoading(true);
    setError(null);
    setOutcome(null);
    setOutcomeMessage(null);
    setSelectedChoice(null);
    setGameState(null);
    setBenchmarkMode(false);

    if (!accessToken || !userEmail) {
      setError("Please login before starting the game.");
      setLoading(false);
      return;
    }

    try {
      const currentPlayerId = await resolvePlayerId(userEmail, accessToken);
      const data = await startRound(boardSize, currentPlayerId, accessToken) as StartRoundData;
      setGameState(data);
      setSessionId(data.sessionId);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Something went wrong");
    } finally {
      setLoading(false);
    }
  }, [accessToken, boardSize, userEmail]);

  const handleRun20Times = useCallback(async () => {
    setBenchmarking(true);
    setError(null);
    setOutcome(null);
    setOutcomeMessage(null);
    setSelectedChoice(null);
    setGameState(null);
    setBenchmarkData([]);

    if (!accessToken || !userEmail) {
      setError("Please login before running the benchmark.");
      setBenchmarking(false);
      return;
    }

    try {
      const currentPlayerId = await resolvePlayerId(userEmail, accessToken);
      const runs: BenchmarkRow[] = [];

      setBenchmarkMode(true);

      for (let i = 1; i <= 20; i++) {
        const data = await startRound(boardSize, currentPlayerId, accessToken) as StartRoundData;

        // Clean up the session server-side by submitting the known correct answer.
        await cleanupBenchmarkSession(data, accessToken);

        runs.push({
          round: i,
          n: data.n,
          bfs: getAlgorithmTime(data, "BFS"),
          dp: getAlgorithmTime(data, "Dynamic Programming"),
          minimumThrows: getMinimumThrows(data),
        });

        setGameState(data);
        setSessionId(data.sessionId);
        setBenchmarkData([...runs]);
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to run 20-round benchmark.");
    } finally {
      setBenchmarking(false);
    }
  }, [accessToken, boardSize, userEmail]);

  const handleSubmitAnswer = useCallback(async (choiceIndex: number) => {
    if (!gameState || sessionId == null || !accessToken) return;
    setSelectedChoice(choiceIndex);
    setLoading(true);
    try {
      // Get the actual answer value from the choices array
      const playerAnswer = gameState.choices[choiceIndex];
      const data = await submitAnswer(sessionId, playerAnswer, accessToken);
      setOutcome(data.outcome);
      setOutcomeMessage(data.message);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to submit");
    } finally {
      setLoading(false);
    }
  }, [accessToken, gameState, sessionId]);

  return (
    <AuthGate>
      <GameLayoutShell
        gameId="03"
        title="Snake and Ladder"
        description="Calculation of minimum dice throws required to navigate a dynamic board. Analyzes pathfinding efficiency."

        controls={
          <div className="flex flex-col gap-4">
            {/* Board size selector */}
            <AlgoDropdown
              label="Board Size (N×N)"
              value={String(boardSize)}
              onChange={(value) => setBoardSize(Number(value))}
              options={BOARD_SIZE_DROPDOWN_OPTIONS}
              disabled={loading || benchmarking}
            />

            {/* Algorithm selector */}
            <AlgoDropdown
              label="Algorithm Selection"
              value={algorithm}
              onChange={setAlgorithm}
              options={[
                { value: "bfs", label: "Breadth-First Search (BFS)" },
                { value: "dp",  label: "Dynamic Programming (DP)" },
              ]}
              disabled={loading || benchmarking}
            />

            {/* Start button */}
            <button
              onClick={handleStart}
              disabled={loading || benchmarking}
              className="
                bg-white text-black py-3 rounded font-bold uppercase tracking-widest
                hover:bg-blue-400 hover:text-white transition-colors text-xs
                disabled:opacity-40 disabled:cursor-not-allowed
              "
            >
              {loading ? "Generating board…" : "Generate Board"}
            </button>

            {/* Error */}
            {error && (
              <p className="text-red-400 text-xs font-mono border border-red-500/30 bg-red-500/10 rounded px-3 py-2">
                {error}
              </p>
            )}

            {/* Performance Benchmark Button */}
            <button
              onClick={handleRun20Times}
              disabled={loading || benchmarking}
              className="
                bg-cyan-500/20 text-cyan-400 py-2 rounded font-bold uppercase tracking-widest
                hover:bg-cyan-500/30 transition-colors text-xs border border-cyan-500/30 hover:border-cyan-500/50
                disabled:opacity-40 disabled:cursor-not-allowed
              "
            >
              {benchmarking ? "RUNNING 20 TIMES..." : "RUN 20 TIMES"}
            </button>

            {/* Quick legend */}
            <div className="border-t border-white/10 pt-3 flex flex-col gap-1.5">
              <p className="text-[10px] uppercase tracking-widest text-white/30 font-mono mb-1">Legend</p>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-sm bg-emerald-500 flex-shrink-0" />
                <span className="text-[11px] text-white/60 font-mono">Ladder (climb up)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-sm bg-red-500 flex-shrink-0" />
                <span className="text-[11px] text-white/60 font-mono">Snake (slide down)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-sm bg-yellow-400 flex-shrink-0" />
                <span className="text-[11px] text-white/60 font-mono">Start cell</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-sm bg-blue-400 flex-shrink-0" />
                <span className="text-[11px] text-white/60 font-mono">Goal cell</span>
              </div>
            </div>
          </div>
        }

        visualization={
          <div className="flex flex-col gap-6 w-full max-w-5xl mx-auto">
            <SnakeLadderBoard
              n={gameState?.n ?? boardSize}
              items={gameState?.items ?? []}
              loading={loading || benchmarking}
              outcome={outcome}
            />

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
                        <th className="p-2">Board Size</th>
                        <th className="p-2">BFS (ms)</th>
                        <th className="p-2">DP (ms)</th>
                        <th className="p-2">Throws</th>
                      </tr>
                    </thead>
                    <tbody>
                      {benchmarkData.map((row) => (
                        <tr key={row.round} className="border-b border-white/5">
                          <td className="p-2 text-white/75">{row.round}</td>
                          <td className="p-2 text-white/75">{row.n}×{row.n}</td>
                          <td className="p-2 text-blue-300">{row.bfs.toFixed(3)}</td>
                          <td className="p-2 text-emerald-300">{row.dp.toFixed(3)}</td>
                          <td className="p-2 text-yellow-300">{row.minimumThrows}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        }

        results={
          gameState ? (
            <div className="flex flex-col gap-4">
              {/* Algorithm timing results */}
              {outcome && (
                <ResultsPanel
                  results={gameState.algorithmResults}
                />
              )}

              {/* Answer challenge — always shown */}
              <AnswerChallenge
                choices={gameState.choices}
                selectedChoice={selectedChoice}
                loading={loading || benchmarking}
                disabled={benchmarkMode}
                outcomeMessage={outcomeMessage}
                onSubmit={handleSubmitAnswer}
              />

              {/* Play again button — shown after outcome */}
              {outcome && (
                <button
                  onClick={handleStart}
                  disabled={loading || benchmarking}
                  className="
                    bg-white text-black py-3 rounded font-bold uppercase tracking-widest
                    hover:bg-blue-400 hover:text-white transition-colors text-xs
                    disabled:opacity-40 disabled:cursor-not-allowed
                  "
                >
                  {loading ? "Generating…" : "Play Again"}
                </button>
              )}

              {benchmarkData.length > 0 && (
                <div className="mt-2 rounded-lg border border-cyan-500/20 bg-cyan-500/10 p-3">
                  <p className="mb-2 text-[10px] font-bold uppercase tracking-[0.18em] text-cyan-300">
                    20-Run Summary
                  </p>
                  <div className="space-y-1 text-[11px] font-mono text-white/75">
                    <p>
                      Avg BFS:{" "}
                      {(
                        benchmarkData.reduce((sum, row) => sum + row.bfs, 0) /
                        benchmarkData.length
                      ).toFixed(3)}{" "}
                      ms
                    </p>
                    <p>
                      Avg DP:{" "}
                      {(
                        benchmarkData.reduce((sum, row) => sum + row.dp, 0) /
                        benchmarkData.length
                      ).toFixed(3)}{" "}
                      ms
                    </p>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <IdleResults />
          )
        }
        bottomSection={
          benchmarkData.length > 0 ? (
            <div className="rounded-xl border border-white/5 bg-[#0a0a0a] p-8">
              <PerformanceRunChart
                title="20-Round Performance Chart"
                data={benchmarkData}
                series={[
                  {
                    key: "bfs",
                    label: "BFS",
                    colorClass: "bg-blue-500",
                    stroke: "#3b82f6",
                  },
                  {
                    key: "dp",
                    label: "DP",
                    colorClass: "bg-emerald-500",
                    stroke: "#10b981",
                  },
                ]}
                yLabel="Time (ms)"
              />
            </div>
          ) : null
        }
      />

    </AuthGate>
  );
}

// ─── Small inline sub-components ─────────────────────────────────────────────

function IdleResults() {
  return (
    <div className="flex flex-col gap-2 font-mono text-[10px] text-white/50">
      <p className="text-white">
        Status: <span className="text-yellow-500">Awaiting Board Generation</span>
      </p>
      <p>Minimum Dice Rolls: --</p>
      <p>BFS Time: -- ms</p>
      <p>DP Time:  -- ms</p>
      <p>Board Items: []</p>
    </div>
  );
}


