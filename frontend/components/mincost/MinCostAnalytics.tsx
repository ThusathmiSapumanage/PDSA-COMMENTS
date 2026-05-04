"use client";

import React from "react";

/* 
 * HistoryItem: matches what we get from Spring Boot.
 * date: string date
 * n: matrix size
 * cost: optimal cost
 * greedyTime: time for greedy algo
 * hungarianTime: time for hungarian algo
 */
interface HistoryItem {
  date: string;
  n: number;
  cost: number;
  greedyTime: number;
  hungarianTime: number;
}

/* 
 * MIN COST ANALYTICS:
 * This component takes the last 10 game runs and makes them visual.
 * We use SVG for charts because its light and we don't need pro libraries. 
 * Perfect for a student viva!
 */
export default function MinCostAnalytics({ history }: { history: HistoryItem[] }) {
  // If no data, just show a nice message
  if (!history || history.length === 0) {
    return (
      <div className="bg-[#0A0A0A] border border-white/5 rounded-2xl p-8 text-center">
        <p className="text-white/20 text-xs font-mono uppercase tracking-widest">
          No history data yet. Run the game to see charts!
        </p>
      </div>
    );
  }

  // We only want the last 10, and we show them oldest to newest (left to right)
  const data = [...history].reverse().slice(-10);
  
  // Find max time to scale the chart height
  const maxTime = Math.max(...data.flatMap(d => [d.greedyTime, d.hungarianTime]), 0.1);
  const chartHeight = 120;
  const chartWidth = 400;

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 w-full mt-10">
      
      {/* 1. TIME EFFICIENCY CHART (Line Chart) */}
      <div className="bg-[#0A0A0A] border border-white/5 rounded-2xl p-6 shadow-xl">
        <p className="text-[10px] uppercase tracking-[0.3em] text-blue-400 mb-6 font-bold">
          Time Efficiency (ms)
        </p>
        
        <div className="relative h-40 w-full">
          <svg viewBox={`0 0 ${chartWidth} ${chartHeight}`} className="w-full h-full overflow-visible">
            {/* Background Grid Lines */}
            {[0, 0.5, 1].map((p) => (
              <line 
                key={p} 
                x1="0" y1={chartHeight * p} x2={chartWidth} y2={chartHeight * p} 
                stroke="white" strokeOpacity="0.05" strokeWidth="1"
              />
            ))}

            {/* Greedy Time Line (Blue-ish) */}
            <polyline
              fill="none"
              stroke="#3b82f6"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              points={data.map((d, i) => {
                const x = (i / (data.length - 1)) * chartWidth;
                const y = chartHeight - (d.greedyTime / maxTime) * chartHeight;
                return `${x},${y}`;
              }).join(" ")}
            />

            {/* Hungarian Time Line (Purple-ish) */}
            <polyline
              fill="none"
              stroke="#a855f7"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeDasharray="4 2"
              points={data.map((d, i) => {
                const x = (i / (data.length - 1)) * chartWidth;
                const y = chartHeight - (d.hungarianTime / maxTime) * chartHeight;
                return `${x},${y}`;
              }).join(" ")}
            />

            {/* Data Points */}
            {data.map((d, i) => {
              const x = (i / (data.length - 1)) * chartWidth;
              const yH = chartHeight - (d.hungarianTime / maxTime) * chartHeight;
              return (
                <circle key={i} cx={x} cy={yH} r="3" fill="#a855f7" />
              );
            })}
          </svg>
        </div>

        {/* Legend for the chart */}
        <div className="flex gap-4 mt-4 justify-center">
          <div className="flex items-center gap-2">
            <div className="w-3 h-1 bg-blue-500 rounded-full"></div>
            <span className="text-[9px] text-white/40 uppercase font-mono">Greedy</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-1 bg-purple-500 rounded-full border-t border-dashed border-white"></div>
            <span className="text-[9px] text-white/40 uppercase font-mono">Hungarian</span>
          </div>
        </div>
      </div>

      {/* 2. COST ACCURACY (Bar Chart) */}
      <div className="bg-[#0A0A0A] border border-white/5 rounded-2xl p-6 shadow-xl">
        <p className="text-[10px] uppercase tracking-[0.3em] text-green-400 mb-6 font-bold">
          Optimization Margin (Min Cost)
        </p>
        
        <div className="flex items-end justify-between h-40 w-full gap-2 px-2">
          {data.map((d, i) => {
            // We scale the bar based on N so we see how "hard" the game was
            const heightPerc = Math.max((d.cost / (d.n * 100)) * 100, 10);
            return (
              <div key={i} className="flex-1 flex flex-col items-center group relative cursor-help">
                {/* Tooltip on hover */}
                <div className="absolute -top-8 bg-white text-black p-1 rounded text-[8px] font-bold opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-20">
                  N={d.n} Cost={d.cost}
                </div>
                {/* The actual Bar */}
                <div 
                  style={{ height: `${heightPerc}%` }}
                  className="w-full bg-green-500/20 border-t-2 border-green-500/40 rounded-t-sm transition-all group-hover:bg-green-500/40"
                ></div>
                <span className="text-[8px] text-white/10 font-mono mt-2">v{i + 1}</span>
              </div>
            );
          })}
        </div>
        
        <p className="text-center text-[9px] text-white/20 mt-4 font-mono italic">
          * Bars show total optimal cost relative to matrix size.
        </p>
      </div>

    </div>
  );
}
