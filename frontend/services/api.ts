/// <reference types="node" />

/**
 * Spring Boot backend base URL (no trailing slash, no trailing /api).
 * Registration: POST {origin}/api/players
 * 
 * NOTE: Requires NEXT_PUBLIC_API_BASE_URL to be set in environment variables.
 */
export function getBackendOrigin(): string {
  const raw = process.env.NEXT_PUBLIC_API_BASE_URL?.trim();
  
  if (!raw) {
    throw new Error("NEXT_PUBLIC_API_BASE_URL environment variable is not set. Create a .env.local file with NEXT_PUBLIC_API_BASE_URL=http://localhost:8080");
  }

  let base = raw.replace(/\/+$/, "");

  // Avoid POST .../api/api/players if env was set to .../api
  if (base.endsWith("/api")) {
    base = base.slice(0, -4).replace(/\/+$/, "");
  }

  return base;
}

/**
 * Returns the full API base URL (origin + /api).
 * Use this for building API endpoints.
 */
export function getApiBaseUrl(): string {
  return `${getBackendOrigin()}/api`;
}

const PLAYERS_URL = `${getBackendOrigin()}/api/players`;
const PLAYERS_LOGIN_URL = `${getBackendOrigin()}/api/players/login`;

type ApiErrorBody = {
  detail?: string;
  message?: string;
  error?: string;
  title?: string;
};

export async function parseApiError(response: Response): Promise<string> {
  try {
    const contentType = response.headers.get("content-type") ?? "";

    if (contentType.includes("json")) {
      const payload = (await response.json()) as ApiErrorBody;

      const msg =
        payload.detail ?? payload.message ?? payload.error ?? payload.title;

      if (msg) {
        return msg;
      }
    } else {
      const text = (await response.text()).trim();

      if (text) {
        return `Request failed with status ${response.status}`;
      }
    }
  } catch {
    // ignore
  }

  return `Request failed with status ${response.status}`;
}

/**
 * POST /api/players — create player (register).
 */
export async function registerPlayer(
  playerName: string,
  playerEmail: string,
  playerPassword: string
): Promise<unknown> {
  const response = await fetch(PLAYERS_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      playerName,
      playerEmail,
      playerPassword,
    }),
  });

  if (!response.ok) {
    throw new Error(await parseApiError(response));
  }

  return response.json();
}

export type LoginResponse = {
  tokenType?: string;
  accessToken?: string;
  expiresInSeconds?: number;
};

export async function loginPlayer(
  playerEmail: string,
  playerPassword: string
): Promise<LoginResponse> {
  const response = await fetch(PLAYERS_LOGIN_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      playerEmail,
      playerPassword,
    }),
  });

  if (!response.ok) {
    throw new Error(await parseApiError(response));
  }

  const data = (await response.json()) as LoginResponse;

  // Store JWT token safely (client-side only)
  if (typeof window !== "undefined" && data.accessToken) {
    localStorage.setItem("accessToken", data.accessToken);
  }

  return data;
}