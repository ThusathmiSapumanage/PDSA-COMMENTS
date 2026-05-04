"use client";

type PerformanceStatsProps = {
  sequentialTimeMs: number;
  threadedTimeMs: number;
  totalSolutions: number;
  sequentialFound?: number;
  threadedFound?: number;
  sequentialHitLimit?: boolean;
  threadedHitLimit?: boolean;
  timeBudgetMs?: number | null;
};

function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms.toFixed(2)} ms`;
  const totalSeconds = ms / 1000;
  if (totalSeconds < 60) return `${totalSeconds.toFixed(2)} s`;
  const minutes = Math.floor(totalSeconds / 60);
  const remainingSeconds = totalSeconds - minutes * 60;
  return `${minutes}m ${remainingSeconds.toFixed(2)}s`;
}

export default function PerformanceStats({
  sequentialTimeMs,
  threadedTimeMs,
  totalSolutions,
  sequentialFound,
  threadedFound,
  sequentialHitLimit,
  threadedHitLimit,
  timeBudgetMs,
}: PerformanceStatsProps) {
  const maxTime = Math.max(sequentialTimeMs, threadedTimeMs, 0.001);
  const seqPct = (sequentialTimeMs / maxTime) * 100;
  const thrPct = (threadedTimeMs / maxTime) * 100;

  const speedup =
    threadedTimeMs > 0 ? sequentialTimeMs / threadedTimeMs : 0;
  const faster = speedup > 1 ? "Threaded" : speedup > 0 ? "Sequential" : null;
  const anyHitLimit = Boolean(sequentialHitLimit || threadedHitLimit);
  const showFoundCounts =
    typeof sequentialFound === "number" && typeof threadedFound === "number";
  const throughputBaseline =
    showFoundCounts ? Math.max(sequentialFound!, threadedFound!, 1) : 1;

  return (
    <div className="flex flex-col gap-5">
      <div>
        <div className="flex items-center justify-between mb-2">
          <span className="text-[10px] font-mono uppercase tracking-[0.2em] text-white/50">
            Sequential
          </span>
          <span className="text-xs font-mono text-white" title={`${sequentialTimeMs.toFixed(2)} ms`}>
            {formatDuration(sequentialTimeMs)}
          </span>
        </div>
        <div className="h-2 bg-white/5 rounded-full overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-amber-500 to-orange-600 transition-all duration-700"
            style={{ width: `${seqPct}%` }}
          />
        </div>
      </div>

      <div>
        <div className="flex items-center justify-between mb-2">
          <span className="text-[10px] font-mono uppercase tracking-[0.2em] text-white/50">
            Threaded
          </span>
          <span className="text-xs font-mono text-white" title={`${threadedTimeMs.toFixed(2)} ms`}>
            {formatDuration(threadedTimeMs)}
          </span>
        </div>
        <div className="h-2 bg-white/5 rounded-full overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-blue-500 to-cyan-500 transition-all duration-700"
            style={{ width: `${thrPct}%` }}
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 pt-3 border-t border-white/5">
        <div>
          <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-white/40 mb-1">
            Solutions
          </div>
          <div className="text-lg font-black text-white tabular-nums">
            {totalSolutions.toLocaleString()}
          </div>
        </div>
        <div>
          <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-white/40 mb-1">
            Speedup
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

      {faster && (
        <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-white/40 text-center">
          {faster} faster
        </div>
      )}

      {showFoundCounts && (
        <div className="grid grid-cols-2 gap-3 pt-3 border-t border-white/5">
          <div>
            <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-amber-400/70 mb-1">
              Seq Found
            </div>
            <div className="text-sm font-bold text-white tabular-nums">
              {sequentialFound!.toLocaleString()}
            </div>
            <div className="h-1 mt-1 bg-white/5 rounded-full overflow-hidden">
              <div
                className="h-full bg-amber-500"
                style={{ width: `${(sequentialFound! / throughputBaseline) * 100}%` }}
              />
            </div>
          </div>
          <div>
            <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-cyan-400/70 mb-1">
              Threaded Found
            </div>
            <div className="text-sm font-bold text-white tabular-nums">
              {threadedFound!.toLocaleString()}
            </div>
            <div className="h-1 mt-1 bg-white/5 rounded-full overflow-hidden">
              <div
                className="h-full bg-cyan-400"
                style={{ width: `${(threadedFound! / throughputBaseline) * 100}%` }}
              />
            </div>
          </div>
        </div>
      )}

      {anyHitLimit && (
        <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-amber-300/80 text-center border border-amber-400/20 bg-amber-400/5 rounded p-2">
          Time-bounded — search hit the {timeBudgetMs ? `${(timeBudgetMs / 1000).toFixed(0)}s` : ""} budget
          (counts are estimates, not exhaustive)
        </div>
      )}
    </div>
  );
}
