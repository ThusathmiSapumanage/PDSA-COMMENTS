"use client";

import { motion } from "framer-motion";
import { AlgorithmMetric } from "./types";

type Props = {
  metrics: AlgorithmMetric[];
};

export default function AlgorithmComparisonChart({ metrics }: Props) {
  if (!metrics || metrics.length === 0) {
    return (
      <div className="rounded-3xl border border-white/10 bg-white/5 p-5 text-sm text-slate-400">
        No algorithm comparison data available yet.
      </div>
    );
  }

  const maxTime = Math.max(...metrics.map((m) => m.executionTimeMs || 0), 1);

  return (
    <div className="bg-[#050505] border border-white/5 rounded-2xl p-6 shadow-2xl">
      <div className="flex justify-between items-center mb-5">
        <div>
          <p className="text-[10px] uppercase tracking-[0.3em] text-cyan-400 font-bold">
            Speed Comparison
          </p>
          <p className="text-[11px] text-white/40 font-mono mt-1">
            Lower execution time is better
          </p>
        </div>
      </div>

      <div className="space-y-5">
        {metrics.map((metric, index) => {
          const widthPercent = ((metric.executionTimeMs || 0) / maxTime) * 100;

          return (
            <motion.div
              key={`${metric.algorithmName}-${index}`}
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.08 }}
              className="rounded-2xl border border-white/10 bg-black/20 p-4"
            >
              <div className="mb-3 flex items-center justify-between gap-4">
                <div>
                  <p className="text-base font-semibold text-white">
                    {metric.algorithmName}
                  </p>
                  <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
                    {metric.status}
                  </p>
                </div>

                <div className="text-right">
                  <p className="text-sm font-semibold text-cyan-300">
                    {metric.executionTimeMs?.toFixed(3)} ms
                  </p>
                  <p className="text-xs text-slate-400">
                    {metric.moveCount} moves
                  </p>
                </div>
              </div>

              <div className="relative h-4 overflow-hidden rounded-full bg-slate-800">
                <motion.div
                  initial={{ width: 0 }}
                  animate={{ width: `${Math.max(widthPercent, 6)}%` }}
                  transition={{ duration: 0.65, ease: "easeOut" }}
                  className="absolute left-0 top-0 h-full rounded-full bg-gradient-to-r from-fuchsia-500 via-purple-500 to-cyan-400"
                />
              </div>
            </motion.div>
          );
        })}
      </div>
    </div>
  );
}
