"use client";

import { motion } from "framer-motion";
import type { AlgorithmResult } from "./types";

interface Props {
  results: AlgorithmResult[];
}

export default function ResultsPanel({ results }: Props) {
  if (!results || results.length === 0) return null;

  // The correct minimum throws — both algorithms should agree, take first
  const minThrows = results.find(r => r.success)?.minimumThrows ?? "—";

  // Find fastest algorithm for highlighting
  const fastestTime = Math.min(...results.map(r => r.executionTimeMs));

  return (
    <div className="flex flex-col gap-3 font-mono">
      {/* Answer */}
      <div className="flex items-baseline gap-3 border-b border-white/10 pb-3">
        <span className="text-[10px] uppercase tracking-widest text-white/40">
          Minimum throws
        </span>
        <motion.span
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-3xl font-black text-white"
        >
          {minThrows}
        </motion.span>
        <span className="text-[10px] text-white/30">dice rolls</span>
      </div>

      {/* Per-algorithm breakdown */}
      <div className="flex flex-col gap-2">
        <p className="text-[10px] uppercase tracking-widest text-white/30">
          Algorithm comparison
        </p>
        {results.map((r, i) => (
          <AlgoRow
            key={r.algorithmName}
            result={r}
            delay={i * 0.1}
            isFastest={r.executionTimeMs === fastestTime}
          />
        ))}
      </div>
    </div>
  );
}

function AlgoRow({
  result,
  delay,
  isFastest,
}: {
  result: AlgorithmResult;
  delay: number;
  isFastest: boolean;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, x: -8 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay }}
      className="flex items-center justify-between rounded bg-white/5 border border-white/10 px-3 py-2"
    >
      <div className="flex flex-col gap-0.5">
        <span className="text-[11px] font-bold text-white/80">
          {result.algorithmName}
        </span>
        <span className="text-[10px] text-white/40">
          Result: {result.success ? result.minimumThrows + " throws" : "failed"}
        </span>
      </div>

      <div className="flex items-center gap-2">
        {/* Timing pill */}
        <span
          className={`
            text-[10px] px-2 py-0.5 rounded-full border
            ${isFastest
              ? "bg-emerald-500/20 border-emerald-500/40 text-emerald-400"
              : "bg-white/5 border-white/10 text-white/50"
            }
          `}
        >
          {result.executionTimeMs.toFixed(3)} ms
          {isFastest && <span className="ml-1">↑</span>}
        </span>

        {/* Status dot */}
        <span
          className={`w-2 h-2 rounded-full ${
            result.success ? "bg-emerald-400" : "bg-red-400"
          }`}
        />
      </div>
    </motion.div>
  );
}
