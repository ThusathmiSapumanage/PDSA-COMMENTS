import {
  ApiError,
  ProgressResponse,
  StartRequest,
  StartResponse,
  StatusResponse,
  SubmitRequest,
  SubmitResponse,
} from "../../components/queens/types";
import { getBackendOrigin } from "../../services/api";

const API_BASE_URL = getBackendOrigin();

function authHeaders(): HeadersInit {
  const headers: HeadersInit = { "Content-Type": "application/json" };
  if (typeof window !== "undefined") {
    const token = localStorage.getItem("accessToken");
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }
  return headers;
}

async function handleResponse<T>(response: Response): Promise<T> {
  const text = await response.text();
  let data: unknown = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { message: text };
    }
  }

  if (!response.ok) {
    const errorData = (data ?? {}) as ApiError;
    throw new Error(
      errorData.message || errorData.error || `Request failed (${response.status})`,
    );
  }

  return data as T;
}

export async function startQueensRound(
  payload: StartRequest,
): Promise<StartResponse> {
  const response = await fetch(`${API_BASE_URL}/sixteen-queens/start`, {
    method: "POST",
    headers: authHeaders(),
    body: JSON.stringify(payload),
  });

  return handleResponse<StartResponse>(response);
}

export async function submitQueensAnswer(
  payload: SubmitRequest,
): Promise<SubmitResponse> {
  const response = await fetch(`${API_BASE_URL}/sixteen-queens/submit`, {
    method: "POST",
    headers: authHeaders(),
    body: JSON.stringify(payload),
  });

  return handleResponse<SubmitResponse>(response);
}

export async function getQueensStatus(
  sessionId: number,
): Promise<StatusResponse> {
  const response = await fetch(
    `${API_BASE_URL}/sixteen-queens/${sessionId}/status`,
    {
      method: "GET",
      headers: authHeaders(),
    },
  );

  return handleResponse<StatusResponse>(response);
}

export async function getQueensProgress(
  sessionId: number,
): Promise<ProgressResponse> {
  const response = await fetch(
    `${API_BASE_URL}/sixteen-queens/${sessionId}/progress`,
    {
      method: "GET",
      headers: authHeaders(),
    },
  );

  return handleResponse<ProgressResponse>(response);
}

export async function cancelQueensRun(sessionId: number): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/sixteen-queens/${sessionId}/cancel`,
    {
      method: "POST",
      headers: authHeaders(),
    },
  );
  if (!response.ok && response.status !== 404) {
    throw new Error(`Cancel failed (${response.status})`);
  }
}
