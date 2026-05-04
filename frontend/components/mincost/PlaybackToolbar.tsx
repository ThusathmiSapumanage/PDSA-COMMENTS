"use client";

import { motion, AnimatePresence } from "framer-motion";

/**
 * PLAYBACK TOOLBAR:
 * This is a professional controller for our algorithm visualizers.
 * It follows the OLED Black theme.
 */
interface PlaybackToolbarProps {
  onNext: () => void;
  onPrev: () => void;
  onPlayToggle: () => void;
  onStop: () => void;
  onSpeedChange: (speed: number) => void;
  isPlaying: boolean;
  currentStep: number;
  totalSteps: number;
  speed: number;
}

export default function PlaybackToolbar({
  onNext,
  onPrev,
  onPlayToggle,
  onStop,
  onSpeedChange,
  isPlaying,
  currentStep,
  totalSteps,
  speed,
}: PlaybackToolbarProps) {
  const speeds = [0.5, 1, 1.5, 2];

  return (
    <motion.div 
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-[#050505] border border-white/10 rounded-xl p-4 shadow-2xl space-y-4"
    >
      <div className="flex items-center justify-between">
        <span className="text-[10px] font-mono text-white/30 uppercase tracking-[0.2em]">
          Step {currentStep + 1} of {totalSteps}
        </span>
        <div className="flex gap-1">
          {speeds.map((s) => (
            <button
              key={s}
              onClick={() => onSpeedChange(s)}
              className={`text-[8px] px-2 py-1 rounded transition-all font-mono ${
                speed === s 
                  ? "bg-blue-600 text-white" 
                  : "bg-white/5 text-white/40 hover:bg-white/10"
              }`}
            >
              {s}x
            </button>
          ))}
        </div>
      </div>

      {/* Progress Bar */}
      <div className="h-1 bg-white/5 rounded-full overflow-hidden">
        <motion.div 
          className="h-full bg-blue-500"
          initial={{ width: 0 }}
          animate={{ width: `${((currentStep + 1) / totalSteps) * 100}%` }}
          transition={{ type: "spring", bounce: 0, duration: 0.5 }}
        />
      </div>

      {/* Control Buttons */}
      <div className="flex items-center justify-center gap-4">
        <button 
          onClick={onStop}
          className="p-2 text-white/40 hover:text-red-400 transition-colors"
          title="Stop & Reset"
        >
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
            <rect x="6" y="6" width="12" height="12" rx="2" />
          </svg>
        </button>

        <button 
          onClick={onPrev}
          disabled={currentStep === 0}
          className="p-2 text-white/40 hover:text-white disabled:opacity-10 transition-colors"
        >
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
            <path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z" />
          </svg>
        </button>

        <button 
          onClick={onPlayToggle}
          className="w-12 h-12 bg-white text-black rounded-full flex items-center justify-center hover:bg-blue-600 hover:text-white transition-all shadow-lg active:scale-95"
        >
          <AnimatePresence mode="wait">
            {isPlaying ? (
              <motion.svg 
                key="pause" 
                initial={{ scale: 0.5, opacity: 0 }} 
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0.5, opacity: 0 }}
                className="w-6 h-6" 
                fill="currentColor" 
                viewBox="0 0 24 24"
              >
                <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" />
              </motion.svg>
            ) : (
              <motion.svg 
                key="play" 
                initial={{ scale: 0.5, opacity: 0 }} 
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0.5, opacity: 0 }}
                className="w-6 h-6 ml-1" 
                fill="currentColor" 
                viewBox="0 0 24 24"
              >
                <path d="M8 5v14l11-7z" />
              </motion.svg>
            )}
          </AnimatePresence>
        </button>

        <button 
          onClick={onNext}
          disabled={currentStep === totalSteps - 1}
          className="p-2 text-white/40 hover:text-white disabled:opacity-10 transition-colors"
        >
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
            <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z" />
          </svg>
        </button>
      </div>
    </motion.div>
  );
}
