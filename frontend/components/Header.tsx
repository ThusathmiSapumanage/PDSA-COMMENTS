"use client";

import React, { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { useAuth } from "./auth/AuthProvider";

export default function Header() {
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const { isAuthenticated, userEmail, logout } = useAuth();
  const menuRef = useRef<HTMLDivElement | null>(null);
  const pathname = usePathname();
  const searchParams = useSearchParams();

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 50);
    };
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    };
    if (menuOpen) {
      window.addEventListener("mousedown", handleClickOutside);
      return () => window.removeEventListener("mousedown", handleClickOutside);
    }
  }, [menuOpen]);

  const avatarInitial = userEmail ? userEmail.charAt(0).toUpperCase() : "U";

  const isLoginPage = pathname === "/login";
  const currentTab = searchParams?.get("tab") === "register" ? "register" : "login";
  const showSignIn = isLoginPage && currentTab === "register";
  const showSignUp = isLoginPage && currentTab === "login";
  const showBothButtons = !isLoginPage;

  const nextParam = searchParams?.get("next");
  const signInHref = nextParam ? `/login?next=${encodeURIComponent(nextParam)}` : "/login";
  const signUpHref = nextParam 
    ? `/login?tab=register&next=${encodeURIComponent(nextParam)}` 
    : "/login?tab=register";

  return (
    <header
      className={`fixed top-0 left-0 w-full z-50 transition-all duration-500 ${
        scrolled ? "bg-black/70 backdrop-blur-md py-3" : "bg-transparent py-5"
      }`}
    >
      <div className="max-w-[1700px] mx-auto px-6 md:px-12 flex items-center justify-between gap-6">
        <Link
          href="/"
          className="text-lg md:text-xl font-bold tracking-[0.25em] hover:text-blue-500 transition-colors uppercase text-white whitespace-nowrap"
        >
          ALGO<span className="text-blue-600">CORE</span>
        </Link>

        <div className="flex items-center gap-3">
          {isAuthenticated ? (
            <div className="relative" ref={menuRef}>
              <button
                type="button"
                onClick={() => setMenuOpen((v) => !v)}
                className="flex items-center gap-2 group"
                aria-label="Open profile menu"
              >
                <span className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-blue-500 to-blue-700 text-sm font-black text-white ring-2 ring-white/10 group-hover:ring-blue-500/60 transition-all">
                  {avatarInitial}
                </span>
              </button>
              {menuOpen && (
                <div className="absolute right-0 mt-3 w-60 rounded-xl border border-white/10 bg-[#0b0b0b]/95 backdrop-blur-md py-2 shadow-2xl">
                  <div className="px-4 py-3 border-b border-white/5">
                    <p className="text-[9px] font-bold tracking-[0.25em] uppercase text-white/40">
                      Signed in as
                    </p>
                    <p className="mt-1 truncate text-sm font-semibold text-white">
                      {userEmail}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setMenuOpen(false);
                      logout();
                    }}
                    className="block w-full text-left px-4 py-2.5 text-[11px] font-bold uppercase tracking-widest text-white/70 hover:bg-white/[0.04] hover:text-blue-400 transition-colors"
                  >
                    Logout
                  </button>
                </div>
              )}
            </div>
          ) : (
            <>
              {(showSignIn || showBothButtons) && (
                <Link
                  href={signInHref}
                  className="inline-flex items-center rounded-full bg-blue-600 px-5 py-2.5 text-[11px] font-black tracking-[0.2em] uppercase text-white hover:bg-blue-500 transition-colors"
                >
                  Sign In
                </Link>
              )}
              {(showSignUp || showBothButtons) && (
                <Link
                  href={signUpHref}
                  className="inline-flex items-center rounded-full bg-blue-600 px-5 py-2.5 text-[11px] font-black tracking-[0.2em] uppercase text-white hover:bg-blue-500 transition-colors"
                >
                  Sign Up
                </Link>
              )}
            </>
          )}
        </div>
      </div>
    </header>
  );
}
