"use client";

import { useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";
import type { BoardItem, Outcome } from "./types";

// ─── Props ───────────────────────────────────────────────────────────────────

interface Props {
  n:       number;             // board dimension
  items:   BoardItem[];        // snakes + ladders
  loading: boolean;
  outcome: Outcome | null;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Convert a 1-indexed cell number to (col, row) in the visual grid.
 *
 * Standard snake-and-ladder numbering:
 *   - Row 0 (bottom) goes left-to-right:  cells 1 … n
 *   - Row 1 goes right-to-left:            cells 2n … n+1
 *   - Row 2 goes left-to-right:            etc.
 *
 * Returns (col, row) where row=0 is the BOTTOM of the visual grid.
 */
function cellToColRow(cell: number, n: number): { col: number; row: number } {
  const idx  = cell - 1;          // 0-indexed
  const row  = Math.floor(idx / n);
  const col  = row % 2 === 0
    ? idx % n                     // left-to-right row
    : n - 1 - (idx % n);          // right-to-left row
  return { col, row };
}

/** Centre position (as fractions of board width/height) for a given cell. */
function cellCenter(cell: number, n: number): { x: number; y: number } {
  const { col, row } = cellToColRow(cell, n);
  const cellSize = 1 / n;
  return {
    x: (col + 0.5) * cellSize,
    // row 0 is the bottom visually — invert
    y: 1 - (row + 0.5) * cellSize,
  };
}

// ─── Component ─────────────────────────────────────────────────────────────────

export default function SnakeLadderBoard({ n, items, loading, outcome }: Props) {
  const totalCells = n * n;

  // Build lookup maps for fast cell queries
  const portalMap = useMemo(() => {
    const m = new Map<number, BoardItem>();
    items.forEach(item => m.set(item.startCell, item));
    return m;
  }, [items]);

  // Build the grid rows (visual top → bottom)
  const gridRows = useMemo(() => {
    const rows: number[][] = [];
    for (let row = n - 1; row >= 0; row--) {
      const cells: number[] = [];
      for (let col = 0; col < n; col++) {
        const visualCol = row % 2 === 0 ? col : n - 1 - col;
        cells.push(row * n + visualCol + 1);
      }
      rows.push(cells);
    }
    return rows;
  }, [n]);

  // ── Empty / loading state ──────────────────────────────────────────────────
  if (items.length === 0) {
    return <EmptyBoard loading={loading} />;
  }

  // ── Populated board ────────────────────────────────────────────────────────
  return (
    <div className="w-full flex flex-col items-center gap-3">
      {/* Board header */}
      <div className="flex items-center gap-3 w-full max-w-2xl">
        <span className="font-mono text-[10px] uppercase tracking-widest text-white/40">
          {n}×{n} Board · {totalCells} cells
        </span>
        <div className="flex-1 h-px bg-white/10" />
        <span className="font-mono text-[10px] text-white/30">
          {items.filter(i => i.pathType === "LADDER").length} ladders ·{" "}
          {items.filter(i => i.pathType === "SNAKE").length} snakes
        </span>
      </div>

      {/* Relative container so SVG overlay lines up with the grid */}
      <div className="relative w-full max-w-2xl aspect-square">

        {/* ── Cell grid ──────────────────────────────────────────────────── */}
        <div
          className="absolute inset-0 grid gap-0.5"
          style={{ gridTemplateRows: `repeat(${n}, 1fr)` }}
        >
          {gridRows.map((rowCells, rowIdx) => (
            <div
              key={rowIdx}
              className="grid gap-0.5"
              style={{ gridTemplateColumns: `repeat(${n}, 1fr)` }}
            >
              {rowCells.map((cell) => (
                <Cell
                  key={cell}
                  cell={cell}
                  n={n}
                  item={portalMap.get(cell)}
                  isStart={cell === 1}
                  isGoal={cell === totalCells}
                />
              ))}
            </div>
          ))}
        </div>

        {/* ── SVG overlay for snake / ladder lines ───────────────────────── */}
        <svg
          className="absolute inset-0 w-full h-full pointer-events-none"
          viewBox="0 0 1 1"
          preserveAspectRatio="none"
        >
          <AnimatePresence>
            {items.map((item) => (
              <PortalLine
                key={`${item.startCell}-${item.endCell}`}
                item={item}
                n={n}
              />
            ))}
          </AnimatePresence>
        </svg>

        {/* ── Outcome overlay ────────────────────────────────────────────────────── */}
        <AnimatePresence>
          {outcome && <OutcomeOverlay outcome={outcome} />}
        </AnimatePresence>
      </div>
    </div>
  );
}

// ─── Cell ─────────────────────────────────────────────────────────────────────

function Cell({
  cell, n, item, isStart, isGoal,
}: {
  cell: number;
  n: number;
  item: BoardItem | undefined;
  isStart: boolean;
  isGoal: boolean;
}) {
  // Determine background color
  let bg = "bg-white/5 border-white/10";
  let textColor = "text-white/40";

  if (isStart) {
    bg = "bg-yellow-400/20 border-yellow-400/50";
    textColor = "text-yellow-300";
  } else if (isGoal) {
    bg = "bg-blue-400/20 border-blue-400/50";
    textColor = "text-blue-300";
  } else if (item?.pathType === "LADDER") {
    bg = "bg-emerald-500/15 border-emerald-500/40";
    textColor = "text-emerald-400";
  } else if (item?.pathType === "SNAKE") {
    bg = "bg-red-500/15 border-red-500/40";
    textColor = "text-red-400";
  }

  // Scale font size with board size
  const fontSize = n <= 6 ? "text-[11px]" : n <= 8 ? "text-[9px]" : "text-[7px]";

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.2, delay: (cell / (n * n)) * 0.4 }}
      className={`
        relative flex flex-col items-center justify-center
        border rounded-sm aspect-square
        ${bg}
        ${textColor}
      `}
    >
      {/* Cell number */}
      <span className={`font-mono font-bold leading-none ${fontSize}`}>{cell}</span>

      {/* Portal indicator icon */}
      {item && (
        <span className={`leading-none ${n <= 8 ? "text-[8px]" : "text-[6px]"}`}>
          {item.pathType === "LADDER" ? "▲" : "▼"}
        </span>
      )}

      {/* Special markers */}
      {isStart && (
        <span className={`leading-none ${n <= 8 ? "text-[8px]" : "text-[6px]"}`}>★</span>
      )}
      {isGoal && (
        <span className={`leading-none ${n <= 8 ? "text-[8px]" : "text-[6px]"}`}>⚑</span>
      )}
    </motion.div>
  );
}

// ─── Portal Line (SVG) ────────────────────────────────────────────────────────

function PortalLine({ item, n }: { item: BoardItem; n: number }) {
  const start = cellCenter(item.startCell, n);
  const end   = cellCenter(item.endCell, n);

  const isLadder = item.pathType === "LADDER";
  const color    = isLadder ? "#10b981" : "#ef4444";   // emerald-500 / red-500
  const opacity  = 0.6;

  // Curved path using a quadratic bezier
  const mx = (start.x + end.x) / 2 + (isLadder ? -0.04 : 0.04);
  const my = (start.y + end.y) / 2;

  return (
    <motion.path
      initial={{ pathLength: 0, opacity: 0 }}
      animate={{ pathLength: 1, opacity }}
      transition={{ duration: 0.6, ease: "easeOut" }}
      d={`M ${start.x} ${start.y} Q ${mx} ${my} ${end.x} ${end.y}`}
      stroke={color}
      strokeWidth="0.008"
      strokeDasharray={isLadder ? "none" : "0.02 0.01"}
      fill="none"
      strokeLinecap="round"
      markerEnd={`url(#${isLadder ? "arrowGreen" : "arrowRed"})`}
    />
  );
}

// ─── Outcome Overlay ─────────────────────────────────────────────────────────

function OutcomeOverlay({ outcome }: { outcome: Outcome }) {
  const config = {
    WIN:  { label: "CORRECT", color: "text-emerald-400", border: "border-emerald-500/50", bg: "bg-emerald-900/80" },
    LOSE: { label: "WRONG",   color: "text-red-400",     border: "border-red-500/50",     bg: "bg-red-900/80" },
    DRAW: { label: "CLOSE!",  color: "text-yellow-400",  border: "border-yellow-500/50",  bg: "bg-yellow-900/80" },
  }[outcome];

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0 }}
      className={`absolute inset-0 flex items-center justify-center rounded-lg border ${config.border} ${config.bg} backdrop-blur-sm`}
    >
      <p className={`font-mono font-black text-5xl tracking-widest uppercase ${config.color}`}>
        {config.label}
      </p>
    </motion.div>
  );
}

// ─── Empty / Loading state ───────────────────────────────────────────────────

function EmptyBoard({ loading }: { loading: boolean }) {
  return (
    <div className="w-full flex flex-col items-center justify-center h-[420px] border-2 border-dashed border-white/10 rounded-xl bg-white/2">
      {loading ? (
        <div className="flex flex-col items-center gap-4">
          {/* Spinner made from animated squares */}
          <div className="grid grid-cols-3 gap-1.5">
            {Array.from({ length: 9 }).map((_, i) => (
              <motion.div
                key={i}
                className="w-4 h-4 rounded-sm bg-blue-400/60"
                animate={{ opacity: [0.2, 1, 0.2] }}
                transition={{
                  duration: 1.2,
                  repeat: Infinity,
                  delay: i * 0.1,
                }}
              />
            ))}
          </div>
          <p className="font-mono text-xs text-white/40 uppercase tracking-widest mt-2">
            Generating board…
          </p>
        </div>
      ) : (
        <div className="flex flex-col items-center gap-3 text-center px-8">
          {/* Decorative grid preview */}
          <div className="grid grid-cols-4 gap-1 opacity-20">
            {Array.from({ length: 16 }).map((_, i) => (
              <div key={i} className="w-8 h-8 rounded-sm border border-white/30 bg-white/5" />
            ))}
          </div>
          <p className="font-mono text-sm text-white/30 uppercase tracking-widest mt-4">
            No board generated
          </p>
          <p className="text-xs text-white/20 max-w-xs">
            Choose a board size and click <span className="text-white/40">Generate Board</span> to start.
          </p>
        </div>
      )}
    </div>
  );
}
