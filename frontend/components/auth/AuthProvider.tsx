"use client";

import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

type AuthContextValue = {
  isAuthenticated: boolean;
  isReady: boolean;
  userEmail: string | null;
  accessToken: string | null;
  login: (email: string, token: string) => void;
  logout: () => void;
};

const STORAGE_KEY = "algocore_auth";
const TOKEN_KEY = "token";

type StoredAuth = {
  email: string | null;
  token: string | null;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function isLikelyJwt(token: string | null): boolean {
  if (!token) return false;
  const parts = token.split(".");
  return parts.length === 3 && parts.every((part) => part.length > 0);
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(base64)
        .split("")
        .map((char) => `%${`00${char.charCodeAt(0).toString(16)}`.slice(-2)}`)
        .join("")
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

function isExpiredJwt(token: string): boolean {
  const payload = decodeJwtPayload(token);
  const exp = payload?.exp;

  if (typeof exp !== "number") {
    return false;
  }

  const nowInSeconds = Math.floor(Date.now() / 1000);
  return exp <= nowInSeconds;
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [userEmail, setUserEmail] = useState<string | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (!stored) {
        return;
      }

      const parsed = JSON.parse(stored) as { email?: string; token?: string };
      const storedEmail = parsed.email ?? null;
      const storedToken = parsed.token ?? null;

      if (
        !storedEmail ||
        !storedToken ||
        !isLikelyJwt(storedToken) ||
        isExpiredJwt(storedToken)
      ) {
        localStorage.removeItem(STORAGE_KEY);
        localStorage.removeItem("accessToken");
        return;
      }

      const payload = decodeJwtPayload(storedToken);
      const subjectEmail =
        typeof payload?.sub === "string" && payload.sub.trim()
          ? payload.sub.trim()
          : storedEmail;

      setUserEmail(subjectEmail);
      setAccessToken(storedToken);
      setIsAuthenticated(true);

      if (localStorage.getItem("accessToken") !== storedToken) {
        localStorage.setItem("accessToken", storedToken);
      }
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem("accessToken");
      setIsAuthenticated(false);
      setUserEmail(null);
      setAccessToken(null);
    } finally {
      setIsReady(true);
    }
  }, []);

  const login = (email: string, token: string) => {
    const normalizedEmail = email.trim();

    setIsAuthenticated(true);
    setUserEmail(normalizedEmail);
    setAccessToken(token);
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({ email: normalizedEmail, token })
    );
    localStorage.setItem("accessToken", token);
  };

  const logout = () => {
    setIsAuthenticated(false);
    setUserEmail(null);
    setAccessToken(null);

    localStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem("authToken");
    localStorage.removeItem("accessToken");
    localStorage.removeItem("jwtToken");
  };

  const value = useMemo(
    () => ({
      isAuthenticated,
      isReady,
      userEmail,
      accessToken,
      login,
      logout,
    }),
    [isAuthenticated, isReady, userEmail, accessToken]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
