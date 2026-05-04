const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

function authHeaders(token) {
  return {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  };
}

async function handleResponse(response, fallbackMessage) {
  if (!response.ok) {
    let message = fallbackMessage;
    try {
      const error = await response.json();
      message = error.detail || error.message || fallbackMessage;
    } catch (_) {}
    throw new Error(message);
  }
  return response.json();
}

export async function generateTrafficNetwork(sessionId, token) {
  const response = await fetch(
    `${API_BASE}/api/traffic-sim/generate/${sessionId}`,
    {
      method: "POST",
      headers: authHeaders(token),
    }
  );

  return handleResponse(response, "Failed to generate traffic network");
}

export async function runDinic(sessionId, token) {
  const response = await fetch(
    `${API_BASE}/api/traffic-sim/dinic/${sessionId}`,
    {
      headers: authHeaders(token),
    }
  );

  return handleResponse(response, "Failed to run Dinic");
}

export async function runFord(sessionId, token) {
  const response = await fetch(
    `${API_BASE}/api/traffic-sim/ford/${sessionId}`,
    {
      headers: authHeaders(token),
    }
  );

  return handleResponse(response, "Failed to run Ford-Fulkerson");
}

export async function submitTrafficAnswer(sessionId, answer, token) {
  const response = await fetch(
    `${API_BASE}/api/traffic-sim/submit/${sessionId}?answer=${answer}`,
    {
      method: "POST",
      headers: authHeaders(token),
    }
  );

  return handleResponse(response, "Failed to submit answer");
}