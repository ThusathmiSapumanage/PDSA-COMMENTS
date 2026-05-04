"use client";

import React, { ReactNode, useRef, useState, useEffect } from "react";
import Link from "next/link";

interface GameLayoutShellProps {
  title: string;
  gameId: string;
  description: string;
  controls: ReactNode;
  visualization: ReactNode;
  results?: ReactNode;
  bottomSection?: ReactNode;
}

export default function GameLayoutShell({
  title,
  gameId,
  description,
  controls,
  visualization,
  results,
  bottomSection,
}: GameLayoutShellProps) {
  const vizRef = useRef<HTMLDivElement>(null);
  const [isFullscreen, setIsFullscreen] = useState(false);

  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };
    document.addEventListener("fullscreenchange", handleFullscreenChange);
    return () => document.removeEventListener("fullscreenchange", handleFullscreenChange);
  }, []);

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      vizRef.current?.requestFullscreen().catch(err => console.error(err));
    } else {
      document.exitFullscreen().catch(err => console.error(err));
    }
  };

  return (
    <div className="min-h-screen bg-[#050505] text-white flex flex-col pt-24 pb-12 px-6 lg:px-12 relative z-10">
      {/* Top Header / Breadcrumb */}
      <div className="flex justify-between items-end mb-12 border-b border-white/10 pb-6">
        <div>
          <Link
            href="/"
            className="text-[10px] font-mono tracking-[0.3em] uppercase text-white/40 hover:text-blue-500 transition-colors mb-4 inline-flex items-center gap-2"
          >
            &#8592; Back to Dashboard
          </Link>
          <div className="flex items-center gap-4">
            <span className="text-white/20 font-mono text-2xl md:text-4xl">{gameId}</span>
            <h1 className="text-3xl md:text-5xl font-black tracking-tighter uppercase">
              {title}
            </h1>
          </div>
        </div>
        <div className="hidden md:block max-w-sm text-right">
          <p className="text-white/40 text-[10px] font-mono uppercase tracking-widest leading-relaxed">
            {description}
          </p>
        </div>
      </div>

      {/* Main Game Interface */}
      <div className="flex flex-col lg:flex-row gap-8 lg:gap-16 flex-1">
        {/* Left Column: Controls (30%) */}
        <div className="w-full lg:w-1/3 flex flex-col gap-8">
          <div className="bg-[#0a0a0a] p-8 rounded-xl border border-white/5 relative overflow-hidden">
            <div className="absolute top-0 left-0 w-full h-[2px] bg-gradient-to-r from-blue-600 to-transparent opacity-50" />
            <h3 className="text-xs font-mono tracking-[0.3em] uppercase text-blue-500 mb-8 border-b border-white/5 pb-4">
              Operation Parameters
            </h3>
            
            <div className="flex flex-col gap-4">
               {controls}
            </div>
          </div>

          {/* Optional Results Panel */}
          {results && (
            <div className="bg-[#0a0a0a] p-8 rounded-xl border border-white/5 mt-auto">
               <h3 className="text-xs font-mono tracking-[0.3em] uppercase text-green-500 mb-6 border-b border-white/5 pb-4">
                 Execution Results
               </h3>
               {results}
            </div>
          )}
        </div>

        {/* Right Column: Visualization (70%) */}
        <div ref={vizRef} className="w-full lg:w-2/3 min-h-[500px] bg-[#0a0a0a] rounded-xl border border-white/5 relative flex flex-col overflow-hidden">
          {/* Header for visualization */}
          <div className="absolute top-0 left-0 w-full px-8 py-4 flex justify-between items-center border-b border-white/5 bg-black/60 backdrop-blur-md z-20">
             <div className="flex items-center gap-3">
                 <div className="w-2 h-2 rounded-full bg-red-500/50 pulse-animation" />
                 <span className="text-[10px] font-mono tracking-[0.2em] uppercase text-white/50">Live Matrix</span>
             </div>
             
             <div className="flex items-center gap-4">
                 <span className="text-[10px] font-mono tracking-[0.2em] uppercase text-white/30 hidden sm:block">System Active</span>
                 <button onClick={toggleFullscreen} className="text-white/40 hover:text-white transition-colors p-1.5 flex items-center justify-center bg-white/5 rounded-sm hover:bg-white/10" title="Toggle Fullscreen">
                     {isFullscreen ? (
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M8 3v3a2 2 0 0 1-2 2H3"/><path d="M21 8h-3a2 2 0 0 1-2-2V3"/><path d="M3 16h3a2 2 0 0 1 2 2v3"/><path d="M16 21v-3a2 2 0 0 1 2-2h3"/></svg>
                     ) : (
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M8 3H5a2 2 0 0 0-2 2v3"/><path d="M21 8V5a2 2 0 0 0-2-2h-3"/><path d="M3 16v3a2 2 0 0 0 2 2h3"/><path d="M16 21h3a2 2 0 0 0 2-2v-3"/></svg>
                     )}
                 </button>
             </div>
          </div>

          {/* Dynamic Render Area */}
          <div className="flex-1 w-full h-full p-8 pt-20 flex items-center justify-center overflow-hidden relative">
            {/* Soft grid background inside visualization */}
            <div className="absolute inset-0 opacity-[0.02]" 
                 style={{ backgroundImage: 'linear-gradient(#fff 1px, transparent 1px), linear-gradient(90deg, #fff 1px, transparent 1px)', backgroundSize: '40px 40px' }} />
            
            <div className="relative z-10 w-full h-full flex items-center justify-center">
              {visualization}
            </div>
          </div>
        </div>
      </div>

      {bottomSection ? <div className="mt-8">{bottomSection}</div> : null}
    </div>
  );
}
