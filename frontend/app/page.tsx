"use client";

import React from "react";
import HeroShowcase from "@/components/HeroShowcase";

export default function Home() {
  return (
    <div className="bg-[#050505] min-h-screen selection:bg-blue-600/30 selection:text-white relative overflow-hidden">
      <main>
        <HeroShowcase />
      </main>
    </div>
  );
}
