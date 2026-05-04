"use client";

import { useMemo } from "react";
import { Position } from "./types";

type QueensBoardProps = {
  boardSize: number;
  queens: Position[];
  onCellClick: (x: number, y: number) => void;
  disabled?: boolean;
  showConflicts?: boolean;
};

function computeAttackedCells(queens: Position[], boardSize: number): Set<string> {
  const attacked = new Set<string>();

  for (const q of queens) {
    for (let i = 1; i <= boardSize; i++) {
      attacked.add(`${i}:${q.y}`);
      attacked.add(`${q.x}:${i}`);
    }
    for (let d = -boardSize; d <= boardSize; d++) {
      const a = q.x + d;
      const b1 = q.y + d;
      const b2 = q.y - d;
      if (a >= 1 && a <= boardSize && b1 >= 1 && b1 <= boardSize) {
        attacked.add(`${a}:${b1}`);
      }
      if (a >= 1 && a <= boardSize && b2 >= 1 && b2 <= boardSize) {
        attacked.add(`${a}:${b2}`);
      }
    }
  }

  for (const q of queens) {
    attacked.delete(`${q.x}:${q.y}`);
  }

  return attacked;
}

function findConflictingQueens(queens: Position[]): Set<string> {
  const conflicts = new Set<string>();

  for (let i = 0; i < queens.length; i++) {
    for (let j = i + 1; j < queens.length; j++) {
      const a = queens[i];
      const b = queens[j];

      const sameRow = a.y === b.y;
      const sameCol = a.x === b.x;
      const sameDiag = Math.abs(a.x - b.x) === Math.abs(a.y - b.y);

      if (sameRow || sameCol || sameDiag) {
        conflicts.add(`${a.x}:${a.y}`);
        conflicts.add(`${b.x}:${b.y}`);
      }
    }
  }

  return conflicts;
}

export default function QueensBoard({
  boardSize,
  queens,
  onCellClick,
  disabled = false,
  showConflicts = true,
}: QueensBoardProps) {
  const queenLookup = useMemo(() => {
    const set = new Set<string>();
    for (const q of queens) set.add(`${q.x}:${q.y}`);
    return set;
  }, [queens]);

  const attackedCells = useMemo(
    () => computeAttackedCells(queens, boardSize),
    [queens, boardSize],
  );

  const conflictQueens = useMemo(
    () => (showConflicts ? findConflictingQueens(queens) : new Set<string>()),
    [queens, showConflicts],
  );

  return (
    <div className="w-full max-w-[680px] mx-auto">
      <div
        className="grid gap-[2px] rounded-lg bg-black/60 p-2 border border-white/10 shadow-[0_0_60px_-20px_rgba(37,99,235,0.5)]"
        style={{
          gridTemplateColumns: `repeat(${boardSize}, minmax(0, 1fr))`,
        }}
      >
        {Array.from({ length: boardSize * boardSize }, (_, index) => {
          const row = Math.floor(index / boardSize) + 1;
          const col = (index % boardSize) + 1;
          const key = `${col}:${row}`;

          const isLight = (row + col) % 2 === 0;
          const hasQueen = queenLookup.has(key);
          const isAttacked = attackedCells.has(key);
          const isConflict = conflictQueens.has(key);

          let cellClass = isLight
            ? "bg-white/10 hover:bg-white/20"
            : "bg-white/[0.03] hover:bg-white/10";

          if (isAttacked && !hasQueen && showConflicts) {
            cellClass = "bg-red-500/10 hover:bg-red-500/20";
          }

          if (hasQueen) {
            cellClass = isConflict
              ? "bg-red-600/80 hover:bg-red-600"
              : "bg-blue-600 hover:bg-blue-500";
          }

          return (
            <button
              key={key}
              type="button"
              onClick={() => onCellClick(col, row)}
              disabled={disabled}
              title={`(${col}, ${row})`}
              className={`relative aspect-square rounded-[2px] transition-all duration-200 flex items-center justify-center ${cellClass} ${
                disabled ? "cursor-not-allowed" : "cursor-pointer"
              } ${hasQueen ? "ring-1 ring-white/30 shadow-[0_0_12px_rgba(59,130,246,0.6)]" : ""}`}
            >
              {hasQueen && (
                <svg
                  viewBox="0 0 24 24"
                  className="w-[60%] h-[60%] text-white drop-shadow-[0_0_4px_rgba(255,255,255,0.8)]"
                  fill="currentColor"
                >
                  <path d="M5 16l-2-9 5 4 4-7 4 7 5-4-2 9H5zm0 2h14v2H5v-2z" />
                </svg>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}
