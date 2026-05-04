"use client";

type SolutionProgressProps = {
  discovered: number;
  total: number;
};

export default function SolutionProgress({
  discovered,
  total,
}: SolutionProgressProps) {
  const pct = total > 0 ? (discovered / total) * 100 : 0;

  return (
    <div>
      <div className="flex items-center justify-between mb-2">
        <span className="text-[10px] font-mono uppercase tracking-[0.2em] text-white/50">
          Discovered
        </span>
        <span className="text-xs font-mono text-white tabular-nums">
          {discovered.toLocaleString()} / {total.toLocaleString()}
        </span>
      </div>
      <div className="h-1.5 bg-white/5 rounded-full overflow-hidden">
        <div
          className="h-full bg-gradient-to-r from-green-500 to-emerald-400 transition-all duration-500"
          style={{ width: `${pct}%` }}
        />
      </div>
      <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-white/30 mt-1 text-right">
        {pct.toFixed(1)}%
      </div>
    </div>
  );
}
