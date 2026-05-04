"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";

type HeroGame = {
  id: string;
  title: string;
  tag: string;
  tagline: string;
  description: string;
  link: string;
  poster: string;
};

const heroGames: HeroGame[] = [
  {
    id: "01",
    title: "Minimum Cost",
    tag: "ASSIGNMENT",
    tagline: "Assign workers to jobs, optimally.",
    description:
      "Solve the classic assignment problem with a Hungarian-style optimizer. Compare greedy choices to the provably optimal matching.",
    link: "/games/minimum-cost",
    poster: "/posters/minimum-cost.png",
  },
  {
    id: "02",
    title: "Knight's Tour",
    tag: "BACKTRACKING",
    tagline: "Visit every square, once.",
    description:
      "Move a knight across the chessboard covering all 64 squares without repeating. Compare backtracking strategies and heuristic tours step by step.",
    link: "/games/knights-tour",
    poster: "/posters/knights-tour.png",
  },
  {
    id: "03",
    title: "Snake & Ladder",
    tag: "SHORTEST PATH",
    tagline: "Find the fastest route home.",
    description:
      "A classic board reframed as a graph search problem. Compute the minimum number of moves to reach the final tile using BFS shortest path.",
    link: "/games/snake-ladder",
    poster: "/posters/snake-ladder.png",
  },
  {
    id: "04",
    title: "Traffic Simulation",
    tag: "MAX FLOW",
    tagline: "Maximize throughput on every edge.",
    description:
      "Model a city network as a flow graph, then solve for the maximum traffic that can move from source to sink using Ford-Fulkerson.",
    link: "/games/traffic-simulation",
    poster: "/posters/traffic-simulation.png",
  },
  {
    id: "05",
    title: "16 Queens' Puzzle",
    tag: "CONCURRENCY",
    tagline: "Sequential vs Threaded",
    description:
      "Place 8 queens on a 16x16 board where none threaten the other. Watch sequential and threaded solvers race across every valid configuration.",
    link: "/games/sixteen-queens",
    poster: "/posters/sixteen-queens.png",
  },
];

const ROTATE_INTERVAL_MS = 7000;

export default function HeroShowcase() {
  const [activeIndex, setActiveIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);

  useEffect(() => {
    if (isPaused) return;
    const timer = window.setInterval(() => {
      setActiveIndex((prev) => (prev + 1) % heroGames.length);
    }, ROTATE_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [isPaused]);

  const active = heroGames[activeIndex];

  return (
    <section
      id="discover"
      className="relative flex flex-col px-6 pt-24 pb-6 md:px-12 md:pt-28 lg:h-screen"
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
    >
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(37,99,235,0.18),transparent_35%),radial-gradient(circle_at_bottom_left,rgba(255,255,255,0.05),transparent_22%)]" />

      <div className="relative mx-auto flex w-full max-w-[1700px] flex-1 flex-col min-h-0">
        <div className="grid grid-cols-12 gap-5 flex-1 min-h-0">
          {/* Featured card */}
          <Link
            href={active.link}
            className="group relative col-span-12 lg:col-span-9 block overflow-hidden rounded-2xl border border-white/10 bg-[#0a0a0a] aspect-[16/9] lg:aspect-auto lg:h-full"
          >
            {heroGames.map((g, i) => (
              <Image
                key={g.id}
                src={g.poster}
                alt={g.title}
                fill
                priority={i === 0}
                sizes="(max-width: 1024px) 100vw, 70vw"
                className={`object-cover transition-opacity duration-700 ${
                  i === activeIndex ? "opacity-100" : "opacity-0"
                }`}
              />
            ))}

            <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/40 to-transparent" />
            <div className="absolute inset-0 bg-gradient-to-r from-black/70 via-transparent to-transparent" />

            <div className="absolute inset-0 flex flex-col justify-end p-8 md:p-12 lg:p-16">
              <span className="text-[10px] font-black tracking-[0.35em] uppercase text-blue-400">
                {active.tag}
              </span>
              <h1 className="mt-4 text-4xl font-black uppercase tracking-tight text-white md:text-6xl lg:text-7xl">
                {active.title}
              </h1>
              <p className="mt-3 max-w-xl text-sm font-semibold uppercase tracking-[0.2em] text-white/60 md:text-base">
                {active.tagline}
              </p>
              <p className="mt-5 max-w-xl text-sm leading-relaxed text-white/60 md:text-base">
                {active.description}
              </p>

              <div className="mt-7 flex flex-wrap items-center gap-4">
                <span className="inline-flex items-center rounded-full bg-blue-600 px-6 py-3 text-[11px] font-black uppercase tracking-[0.2em] text-white transition-colors group-hover:bg-blue-500">
                  Play Now
                </span>
              </div>
            </div>

            <div className="absolute top-6 right-6 flex gap-2">
              {heroGames.map((g, i) => (
                <span
                  key={g.id}
                  className={`h-1 rounded-full transition-all duration-500 ${
                    i === activeIndex ? "w-8 bg-blue-500" : "w-4 bg-white/25"
                  }`}
                />
              ))}
            </div>
          </Link>

          {/* Sidebar list */}
          <div className="col-span-12 lg:col-span-3 flex flex-col gap-2 lg:h-full">
            {heroGames.map((g, i) => {
              const isActive = i === activeIndex;
              return (
                <button
                  key={g.id}
                  type="button"
                  onClick={() => setActiveIndex(i)}
                  onMouseEnter={() => setActiveIndex(i)}
                  className={`group flex items-center gap-4 rounded-xl border px-3 py-3 text-left transition-all duration-300 lg:flex-1 lg:min-h-0 ${
                    isActive
                      ? "border-blue-500/40 bg-white/[0.06]"
                      : "border-white/5 bg-transparent hover:border-white/15 hover:bg-white/[0.03]"
                  }`}
                >
                  <div className="relative h-full max-h-24 aspect-[16/10] shrink-0 overflow-hidden rounded-md border border-white/10 bg-[#0a0a0a]">
                    <Image
                      src={g.poster}
                      alt={g.title}
                      fill
                      sizes="120px"
                      className="object-cover"
                    />
                    {isActive && (
                      <div className="absolute inset-0 bg-blue-500/10" />
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[10px] font-bold uppercase tracking-[0.2em] text-blue-400/80">
                      {g.tag}
                    </p>
                    <p className="truncate text-sm font-bold uppercase tracking-tight text-white">
                      {g.title}
                    </p>
                  </div>
                  {isActive && (
                    <span className="h-2 w-2 shrink-0 rounded-full bg-blue-500 shadow-[0_0_10px_rgba(37,99,235,0.8)]" />
                  )}
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </section>
  );
}
