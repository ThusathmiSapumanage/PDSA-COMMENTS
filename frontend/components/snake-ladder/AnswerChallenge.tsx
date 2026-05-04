"use client";

import { motion } from "framer-motion";

interface Props {
  choices:        number[];          // exactly 3 integers from the backend
  selectedChoice: number | null;     // index of what the player clicked, or null
  loading:        boolean;
  disabled?:      boolean;
  outcomeMessage?: string | null;
  onSubmit:       (choiceIndex: number) => void;
}

export default function AnswerChallenge({
  choices,
  selectedChoice,
  loading,
  disabled,
  outcomeMessage,
  onSubmit,
}: Props) {
  return (
    <div className="flex flex-col gap-3">
      {/* Prompt */}
      <div className="border-t border-white/10 pt-3">
        <p className="text-[10px] uppercase tracking-widest text-white/40 font-mono">
          Challenge
        </p>
        <p className="text-sm text-white/80 mt-1 font-mono">
          Choose the minimum number of dice throws to reach the last cell
        </p>
      </div>

      {/* 3 choice buttons */}
      <div className="grid grid-cols-3 gap-2">
        {choices.map((value, index) => (
          <ChoiceButton
            key={value}
            value={value}
            index={index}
            isSelected={selectedChoice === index}
            isDisabled={selectedChoice !== null || loading || Boolean(disabled)}
            delay={index * 0.08}
            onSelect={() => onSubmit(index)}
          />
        ))}
      </div>

      {/* Submitted answer feedback */}
      {selectedChoice !== null && !loading && outcomeMessage && (
        <div className="px-3 py-2 rounded border font-mono text-xs leading-relaxed bg-yellow-500/10 border-yellow-500/30 text-yellow-400">
          {outcomeMessage}
        </div>
      )}

      {/* Hint */}
      {selectedChoice === null && !loading && (
        <p className="text-[10px] text-white/20 font-mono">
          Study the board carefully — count the optimal path using snakes and ladders.
        </p>
      )}
    </div>
  );
}

function ChoiceButton({
  value,
  index,
  isSelected,
  isDisabled,
  delay,
  onSelect,
}: {
  value:      number;
  index:      number;
  isSelected: boolean;
  isDisabled: boolean;
  delay:      number;
  onSelect:   () => void;
}) {
  return (
    <motion.button
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay }}
      whileHover={!isDisabled ? { scale: 1.04 } : {}}
      whileTap={!isDisabled ? { scale: 0.97 } : {}}
      onClick={onSelect}
      disabled={isDisabled}
      className={`
        relative py-4 rounded-lg border font-mono font-black text-xl
        flex flex-col items-center justify-center gap-1
        transition-all duration-200 cursor-pointer
        ${isSelected
          ? "bg-white text-black border-white"
          : isDisabled
            ? "bg-white/5 border-white/10 text-white/30 cursor-not-allowed"
            : "bg-white/5 border-white/20 text-white hover:bg-white/10 hover:border-white/40"
        }
      `}
    >
      {/* Choice letter: A, B, C */}
      <span
        className={`
          absolute top-1.5 left-2 text-[9px] uppercase tracking-widest
          ${isSelected ? "text-black/50" : "text-white/20"}
        `}
      >
        {["A", "B", "C"][index]}
      </span>

      {/* The number */}
      {value}

      <span className={`text-[9px] uppercase tracking-wider ${isSelected ? "text-black/50" : "text-white/30"}`}>
        throws
      </span>
    </motion.button>
  );
}
