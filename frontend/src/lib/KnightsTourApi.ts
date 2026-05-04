import {
  ApiError,
  CorrectAnswerResponse,
  HintRequest,
  HintResponse,
  StartRequest,
  StartResponse,
  SubmitRequest,
  SubmitResponse,
} from "../../components/knightstour/types";
import { getBackendOrigin } from "../../services/api";

const API_BASE_URL = getBackendOrigin();

function getAuthToken(): string | null {
  if (typeof window === "undefined") return null;

  const possibleKeys = ["algocore_auth", "token", "authToken", "accessToken", "jwtToken"];

  for (const key of possibleKeys) {
    const rawValue = localStorage.getItem(key);
    if (!rawValue) continue;

    const trimmed = rawValue.trim();
    if (!trimmed) continue;

    try {
      const parsed = JSON.parse(trimmed);

      if (typeof parsed === "string" && parsed.trim()) {
        return parsed.trim();
      }

      if (parsed && typeof parsed === "object") {
        if (typeof parsed.token === "string" && parsed.token.trim()) {
          return parsed.token.trim();
        }
        if (typeof parsed.accessToken === "string" && parsed.accessToken.trim()) {
          return parsed.accessToken.trim();
        }
      }
    } catch {
      return trimmed;
    }
  }

  return null;
}

function buildHeaders(): HeadersInit {
  const token = getAuthToken();

  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

async function parseResponse<T>(response: Response): Promise<T> {
  const contentType = response.headers.get("content-type") || "";
  const isJson = contentType.includes("application/json");

  const body = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    const errorBody = body as ApiError | string;

    const message =
      typeof errorBody === "string"
        ? errorBody
        : errorBody?.message ||
          errorBody?.detail ||
          errorBody?.title ||
          errorBody?.error ||
          `Request failed with status ${response.status}`;

    throw new Error(message);
  }

  return body as T;
}

export async function startKnightTourRound(
  payload: StartRequest
): Promise<StartResponse> {
  const response = await fetch(`${API_BASE_URL}/knights-tour/start`, {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify(payload),
  });

  return parseResponse<StartResponse>(response);
}

export async function getKnightTourHint(
  payload: HintRequest
): Promise<HintResponse> {
  const response = await fetch(`${API_BASE_URL}/knights-tour/hint`, {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify(payload),
  });

  return parseResponse<HintResponse>(response);
}

export async function submitKnightTourAnswer(
  payload: SubmitRequest
): Promise<SubmitResponse> {
  const response = await fetch(`${API_BASE_URL}/knights-tour/submit`, {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify(payload),
  });

  return parseResponse<SubmitResponse>(response);
}

export async function getKnightTourCorrectAnswer(
  sessionId: number
): Promise<CorrectAnswerResponse> {
  const response = await fetch(
    `${API_BASE_URL}/knights-tour/${sessionId}/correct-answer`,
    {
      method: "GET",
      headers: buildHeaders(),
    }
  );

  return parseResponse<CorrectAnswerResponse>(response);
}

export function hasKnightTourAuth(): boolean {
  return !!getAuthToken();
}