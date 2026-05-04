"use client";

import { motion } from "framer-motion";
import { Move } from "./types";

type KnightBoardProps = {
  boardSize: number;
  selectedMoves: Move[];
  correctMoves?: Move[];
  hintMoves?: Move[];
  onCellClick: (x: number, y: number) => void;
  disabled?: boolean;
  startX?: number;
  startY?: number;
  playbackMoves?: Move[];
  playbackStep?: number;
  showPlayerPath?: boolean;
  compact?: boolean;
};

export default function KnightBoard({
  boardSize,
  selectedMoves,
  correctMoves = [],
  hintMoves = [],
  onCellClick,
  disabled = false,
  startX,
  startY,
  playbackMoves = [],
  playbackStep,
  showPlayerPath = true,
  compact = false,
}: KnightBoardProps) {
  const getPlayerMoveForCell = (x: number, y: number) =>
    showPlayerPath ? selectedMoves.find((move) => move.x === x && move.y === y) : undefined;

  const getCorrectMoveForCell = (x: number, y: number) =>
    correctMoves.find((move) => move.x === x && move.y === y);

  const getHintMoveForCell = (x: number, y: number) =>
    hintMoves.find((move) => move.x === x && move.y === y);

  const playbackActive =
    playbackMoves.length > 0 &&
    typeof playbackStep === "number" &&
    playbackStep >= 0 &&
    playbackStep < playbackMoves.length;

  const playbackMove = playbackActive ? playbackMoves[playbackStep!] : null;

  const latestSelectedMove =
    selectedMoves.length > 0 ? selectedMoves[selectedMoves.length - 1] : null;

  const knightX = playbackMove?.x ?? latestSelectedMove?.x ?? startX;
  const knightY = playbackMove?.y ?? latestSelectedMove?.y ?? startY;

  const cellBaseSize = compact
    ? boardSize === 16
      ? "minmax(28px, 1fr)"
      : "minmax(54px, 1fr)"
    : boardSize === 16
      ? "minmax(0, 1fr)"
      : "minmax(72px, 1fr)";

  const numberSize = compact
    ? boardSize === 16
      ? "text-[10px]"
      : "text-lg"
    : boardSize === 16
      ? "text-sm"
      : "text-xl";

  const coordinateSize = compact
    ? boardSize === 16
      ? "text-[8px]"
      : "text-[10px]"
    : boardSize === 16
      ? "text-[10px]"
      : "text-xs";

  const knightSize = compact
    ? boardSize === 16
      ? "text-xl"
      : "text-4xl"
    : boardSize === 16
      ? "text-3xl"
      : "text-6xl";

  const hintSize = compact
    ? boardSize === 16
      ? "text-[8px]"
      : "text-[10px]"
    : boardSize === 16
      ? "text-[10px]"
      : "text-xs";

  const minHeightClass = compact
    ? boardSize === 16
      ? "min-h-[28px]"
      : "min-h-[42px]"
    : "min-h-[50px]";

  const cellRadiusClass = compact ? "rounded-lg" : "rounded-xl";
  const boardPaddingClass = compact ? "p-3" : "p-4";
  const boardGapClass = compact ? "gap-[3px]" : "gap-[4px]";
  const boardRadiusClass = compact ? "rounded-[22px]" : "rounded-[28px]";

  return (
    <div
      className={`grid ${boardGapClass} ${boardRadiusClass} border border-white/10 bg-black/35 ${boardPaddingClass} shadow-2xl w-fit max-w-full`}
      style={{
        gridTemplateColumns: `repeat(${boardSize}, ${cellBaseSize})`,
      }}
    >
      {Array.from({ length: boardSize * boardSize }, (_, index) => {
        const row = Math.floor(index / boardSize) + 1;
        const col = (index % boardSize) + 1;

        const isDark = (row + col) % 2 === 0;
        const playerMove = getPlayerMoveForCell(col, row);
        const correctMove = getCorrectMoveForCell(col, row);
        const hintMove = getHintMoveForCell(col, row);
        const isStart = startX === col && startY === row;
        const isKnightHere = knightX === col && knightY === row;
        const isPlaybackCurrent = playbackMove?.x === col && playbackMove?.y === row;

        let cellClass = isDark
          ? "border-slate-700 bg-slate-800/90 text-slate-100"
          : "border-slate-300/30 bg-slate-100 text-slate-900";

        if (hintMove) {
          cellClass = "border-amber-300 bg-amber-500 text-slate-950";
        }

        if (playerMove) {
          cellClass = "border-purple-300 bg-purple-600 text-white";
        }

        if (correctMove) {
          cellClass = "border-emerald-300 bg-emerald-600 text-white";
        }

        if (isPlaybackCurrent) {
          cellClass = "border-cyan-300 bg-cyan-700 text-white";
        }

        if (isKnightHere && !playerMove && !correctMove && !hintMove && !isPlaybackCurrent) {
          cellClass = "border-fuchsia-300 bg-fuchsia-700 text-white";
        }

        return (
          <button
            key={`${col}-${row}`}
            type="button"
            onClick={() => onCellClick(col, row)}
            disabled={disabled}
            className={`relative flex aspect-square ${minHeightClass} items-center justify-center overflow-hidden border font-bold transition-all duration-200 ${
              disabled ? "cursor-not-allowed opacity-90" : "hover:scale-[1.03]"
            } ${cellRadiusClass} ${cellClass}`}
            title={`(${col}, ${row})`}
          >
            <span
              className={`absolute left-1.5 top-1.5 z-10 ${coordinateSize} ${boardSize === 16 ? "opacity-0" : "opacity-70"}`}
            >
              {col},{row}
            </span>

            {isStart && !playerMove && !correctMove && !hintMove && !isKnightHere && !isPlaybackCurrent && (
              <span className="z-10 text-[10px] font-extrabold tracking-wide text-emerald-200">
                START
              </span>
            )}

            {correctMove ? (
              <span className={`z-10 ${numberSize}`}>{correctMove.stepNo}</span>
            ) : playerMove ? (
              <span className={`z-10 ${numberSize}`}>{playerMove.stepNo}</span>
            ) : hintMove ? (
              <span className={`z-10 ${hintSize} font-extrabold`}>HINT</span>
            ) : null}

            {isKnightHere && (
              <motion.span
                layoutId="knight-piece"
                transition={{ type: "spring", stiffness: 360, damping: 28 }}
                className={`pointer-events-none absolute inset-0 z-20 flex items-center justify-center ${knightSize} drop-shadow-[0_0_16px_rgba(255,255,255,0.55)]`}
              >
                <span className="inline-block translate-y-[2px] select-none">♞</span>
              </motion.span>
            )}
          </button>
        );
      })}
    </div>
  );
}
