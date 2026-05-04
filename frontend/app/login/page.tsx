"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useAuth } from "../../components/auth/AuthProvider";
import {
  loginPlayer,
  registerPlayer,
} from "../../services/api";

function isLikelyJwt(token: string | null | undefined): boolean {
  if (!token || typeof token !== "string") return false;
  const parts = token.split(".");
  return parts.length === 3 && parts.every((part) => part.length > 0);
}

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { isAuthenticated, login, isReady } = useAuth();

  const [activeTab, setActiveTab] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const inputClassName =
    "w-full rounded-xl border border-white/10 bg-[#0b1220] px-4 py-3 text-sm text-white placeholder:text-white/35 outline-none transition-all duration-200 focus:border-blue-500/70 focus:bg-[#0f1728] focus:shadow-[0_0_0_3px_rgba(37,99,235,0.18)]";

  const isLoginReady = email.trim().length > 0 && password.trim().length > 0;

  const isRegisterReady =
    fullName.trim().length > 0 &&
    email.trim().length > 0 &&
    password.trim().length > 0 &&
    confirmPassword.trim().length > 0;

  const nextPath = useMemo(() => {
    const next = searchParams?.get("next");
    return next && next.startsWith("/") ? next : "/";
  }, [searchParams]);

  const currentTab = useMemo(() => {
    return searchParams?.get("tab") === "register" ? "register" : "login";
  }, [searchParams]);

  useEffect(() => {
    if (isReady && isAuthenticated) {
      router.replace(nextPath);
    }
  }, [isReady, isAuthenticated, router, nextPath]);

  useEffect(() => {
    setActiveTab(currentTab);
    setEmail("");
    setPassword("");
    setFullName("");
    setConfirmPassword("");
    setShowPassword(false);
    setError(null);
  }, [currentTab]);

  const handleLogin = async (event: React.FormEvent) => {
    event.preventDefault();

    if (!email || !password) {
      setError("Please provide your email and password.");
      return;
    }

    setError(null);
    setIsSubmitting(true);

    try {
      const data = await loginPlayer(email, password);

      if (!data?.accessToken) {
        setError("Sign-in failed. Access token was missing from the server response.");
        return;
      }

      if (!isLikelyJwt(data.accessToken)) {
        setError("Sign-in failed. The returned access token is not a valid JWT format.");
        return;
      }

      login(email, data.accessToken);
      router.replace(nextPath);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to reach server. Please try again."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRegister = async (event: React.FormEvent) => {
    event.preventDefault();

    if (!fullName || !email || !password || !confirmPassword) {
      setError("Please complete all registration fields.");
      return;
    }

    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setError(null);
    setIsSubmitting(true);

    try {
      await registerPlayer(fullName, email, password);

      let loginData;

      try {
        loginData = await loginPlayer(email, password);
      } catch (loginErr) {
        const loginMessage =
          loginErr instanceof Error ? loginErr.message : "Unknown error";

        setError(
          `Registration succeeded, but automatic login failed: ${loginMessage}`
        );
        return;
      }

      if (!loginData?.accessToken) {
        setError("Registration succeeded, but login token was missing.");
        return;
      }

      if (!isLikelyJwt(loginData.accessToken)) {
        setError("Registration succeeded, but the returned token is not a valid JWT format.");
        return;
      }

      login(email, loginData.accessToken);
      router.replace(nextPath);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to reach server. Please try again."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#050505] text-white flex items-center justify-center px-6 py-24 relative">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,rgba(37,99,235,0.18),transparent_55%)]" />

      <div className="relative z-10 w-full max-w-5xl grid grid-cols-1 lg:grid-cols-[1.12fr_1fr] lg:min-h-[620px] gap-10 bg-[#070707] border border-white/5 rounded-2xl overflow-hidden">
        <div className="p-10 lg:p-12 h-full flex flex-col border-b lg:border-b-0 lg:border-r border-white/5">
          <p className="text-[10px] font-mono uppercase tracking-[0.4em] text-blue-500 mb-6">
            User Access
          </p>

          <h1 className="text-[clamp(1.7rem,3.6vw,3.1rem)] leading-[0.95] font-black uppercase mb-4">
            ALGOCORE
          </h1>

          <p className="text-[11px] font-mono uppercase tracking-[0.35em] text-white/40 mb-6">
            Account Access
          </p>

          <p className="text-sm text-white/50">
            Secure access to algorithm simulations. Login or register to continue.
          </p>

          {error && (
            <div className="mt-6 border border-red-500/30 bg-red-500/10 text-red-300 text-xs px-4 py-3 rounded-lg">
              {error}
            </div>
          )}

          <div className="mt-auto pt-10 text-[10px] text-white/30">
            Back to{" "}
            <Link href="/" className="text-blue-500 hover:text-white">
              dashboard
            </Link>
          </div>
        </div>

        <div className="p-10 lg:p-12 flex flex-col">
          {activeTab === "login" ? (
            <form onSubmit={handleLogin} className="flex flex-col gap-6 flex-1">
              <input
                type="email"
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className={inputClassName}
              />

              <input
                type={showPassword ? "text" : "password"}
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className={inputClassName}
              />

              <button
                type="submit"
                disabled={isSubmitting || !isLoginReady}
                className="mt-auto bg-white text-black py-3 rounded font-bold disabled:opacity-50"
              >
                {isSubmitting ? "Signing In..." : "Sign In"}
              </button>
            </form>
          ) : (
            <form onSubmit={handleRegister} className="flex flex-col gap-6 flex-1">
              <input
                type="text"
                placeholder="Player Name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className={inputClassName}
              />

              <input
                type="email"
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className={inputClassName}
              />

              <input
                type="password"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className={inputClassName}
              />

              <input
                type="password"
                placeholder="Confirm Password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className={inputClassName}
              />

              <button
                type="submit"
                disabled={isSubmitting || !isRegisterReady}
                className="mt-auto bg-white text-black py-3 rounded font-bold disabled:opacity-50"
              >
                {isSubmitting ? "Signing Up..." : "Sign Up"}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
