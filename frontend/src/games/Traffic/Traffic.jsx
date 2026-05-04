"use client";

import React, { useEffect, useState } from "react";
import "./Traffic.css";

const SESSION_ID = 1;
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

const NODE_POSITIONS = {
  A: { x: 70, y: 220, kind: "source" },
  B: { x: 210, y: 90 },
  C: { x: 210, y: 220 },
  D: { x: 210, y: 350 },
  E: { x: 390, y: 135 },
  F: { x: 390, y: 305 },
  G: { x: 560, y: 135 },
  H: { x: 560, y: 305 },
  T: { x: 735, y: 220, kind: "sink" },
};

const EDGE_ORDER = [
  ["A", "B"],
  ["A", "C"],
  ["A", "D"],
  ["B", "E"],
  ["B", "F"],
  ["C", "E"],
  ["C", "F"],
  ["D", "F"],
  ["E", "G"],
  ["E", "H"],
  ["F", "H"],
  ["G", "T"],
  ["H", "T"],
];

function getEdgeKey(startNode, endNode) {
  return `${startNode}-${endNode}`;
}

async function readJson(response, fallbackMessage) {
  const text = await response.text();
  const data = text ? JSON.parse(text) : {};

  if (!response.ok) {
    throw new Error(data?.message || data?.detail || fallbackMessage);
  }

  return data;
}

function buildRoadMap(roads) {
  return roads.reduce((map, road) => {
    map[getEdgeKey(road.id.startNode, road.id.endNode)] = road.capacity;
    return map;
  }, {});
}

export default function Traffic() {
  const [roads, setRoads] = useState([]);
  const [userAnswer, setUserAnswer] = useState("");
  const [dinicRun, setDinicRun] = useState(null);
  const [fordRun, setFordRun] = useState(null);
  const [submission, setSubmission] = useState(null);
  const [statusMessage, setStatusMessage] = useState("Preparing traffic network...");
  const [isGenerating, setIsGenerating] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    generateRound();
  }, []);

  const roadMap = buildRoadMap(roads);

  async function generateRound() {
    setIsGenerating(true);
    setErrorMessage("");
    setSubmission(null);
    setUserAnswer("");
    setStatusMessage("Generating a new network and measuring both algorithms...");

    try {
      const generated = await fetch(
        `${API_BASE_URL}/api/traffic-sim/generate/${SESSION_ID}`,
        { method: "POST" }
      );
      const nextRoads = await readJson(
        generated,
        "Unable to generate the traffic network."
      );
      setRoads(Array.isArray(nextRoads) ? nextRoads : []);

      const [dinicResponse, fordResponse] = await Promise.all([
        fetch(`${API_BASE_URL}/api/traffic-sim/dinic/${SESSION_ID}`),
        fetch(`${API_BASE_URL}/api/traffic-sim/ford/${SESSION_ID}`),
      ]);

      const dinicData = await readJson(
        dinicResponse,
        "Unable to evaluate the network using Dinic."
      );
      const fordData = await readJson(
        fordResponse,
        "Unable to evaluate the network using Ford-Fulkerson."
      );

      setDinicRun(dinicData);
      setFordRun(fordData);
      setStatusMessage("Network ready. Study the capacities and submit the maximum flow.");
    } catch (error) {
      setRoads([]);
      setDinicRun(null);
      setFordRun(null);
      setErrorMessage(error.message || "Something went wrong while loading the traffic round.");
      setStatusMessage("Round setup failed.");
    } finally {
      setIsGenerating(false);
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (!userAnswer.trim()) {
      setErrorMessage("Enter a maximum flow value before submitting.");
      return;
    }

    setIsSubmitting(true);
    setErrorMessage("");

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/traffic-sim/submit/${SESSION_ID}?answer=${encodeURIComponent(
          userAnswer
        )}`,
        { method: "POST" }
      );

      const data = await readJson(response, "Unable to submit the answer.");
      setSubmission(data);

      if (data.correct) {
        setStatusMessage(
          `${data.playerName || "Player"} solved the network correctly.`
        );
      } else {
        setStatusMessage("Answer recorded. The submitted flow is not correct.");
      }
    } catch (error) {
      setErrorMessage(error.message || "Something went wrong while submitting the answer.");
    } finally {
      setIsSubmitting(false);
    }
  }

  const edgeCards = EDGE_ORDER.map(([startNode, endNode]) => ({
    startNode,
    endNode,
    capacity: roadMap[getEdgeKey(startNode, endNode)],
  }));

  const metrics = [
    {
      label: "Dinic runtime",
      value: dinicRun ? `${Number(dinicRun.timeMs).toFixed(3)} ms` : "Pending",
      accent: "traffic-metric-card--blue",
    },
    {
      label: "Ford-Fulkerson runtime",
      value: fordRun ? `${Number(fordRun.timeMs).toFixed(3)} ms` : "Pending",
      accent: "traffic-metric-card--amber",
    },
    {
      label: "Road count",
      value: String(roads.length),
      accent: "traffic-metric-card--slate",
    },
    {
      label: "Player answer",
      value: userAnswer.trim() ? userAnswer.trim() : "Not submitted",
      accent: "traffic-metric-card--rose",
    },
  ];

  return (
    <div className="traffic-page">
      <section className="traffic-hero">
        <div className="traffic-hero__copy">
          <span className="traffic-eyebrow">Maximum Flow Challenge</span>
          <h1>Traffic Simulation Problem</h1>
          <p>
            Inspect the directed road network from A to T, calculate the maximum
            throughput, and test your answer against Dinic and Ford-Fulkerson.
          </p>
        </div>

        <div className="traffic-hero__actions">
          <button
            type="button"
            className="traffic-button traffic-button--secondary"
            onClick={generateRound}
            disabled={isGenerating}
          >
            {isGenerating ? "Generating..." : "Generate New Network"}
          </button>
        </div>
      </section>

      <section className="traffic-status-card">
        <div>
          <strong>Round status</strong>
          <p>{statusMessage}</p>
        </div>
        {errorMessage ? <p className="traffic-status-card__error">{errorMessage}</p> : null}
      </section>

      <section className="traffic-grid">
        <article className="traffic-panel traffic-panel--graph">
          <div className="traffic-panel__header">
            <div>
              <span className="traffic-panel__kicker">Network graph</span>
              <h2>Directed road map</h2>
            </div>
            <p>Capacities are vehicles per minute. Source is A and sink is T.</p>
          </div>

          <div className="traffic-network">
            <svg
              className="traffic-network__svg"
              viewBox="0 0 810 430"
              role="img"
              aria-label="Traffic network graph"
            >
              <defs>
                <marker
                  id="traffic-arrow"
                  markerWidth="10"
                  markerHeight="10"
                  refX="8"
                  refY="3"
                  orient="auto"
                  markerUnits="strokeWidth"
                >
                  <path d="M0,0 L0,6 L9,3 z" />
                </marker>
              </defs>

              {edgeCards.map((edge) => {
                const start = NODE_POSITIONS[edge.startNode];
                const end = NODE_POSITIONS[edge.endNode];
                const labelX = (start.x + end.x) / 2;
                const labelY = (start.y + end.y) / 2 - 10;

                return (
                  <g key={getEdgeKey(edge.startNode, edge.endNode)}>
                    <line
                      x1={start.x}
                      y1={start.y}
                      x2={end.x}
                      y2={end.y}
                      className="traffic-network__edge"
                    />
                    <rect
                      x={labelX - 28}
                      y={labelY - 12}
                      width="56"
                      height="24"
                      rx="12"
                      className="traffic-network__edge-label-bg"
                    />
                    <text
                      x={labelX}
                      y={labelY + 4}
                      className="traffic-network__edge-label"
                      textAnchor="middle"
                    >
                      {edge.capacity ?? "--"}
                    </text>
                  </g>
                );
              })}

              {Object.entries(NODE_POSITIONS).map(([node, position]) => (
                <g key={node}>
                  <circle
                    cx={position.x}
                    cy={position.y}
                    r="28"
                    className={`traffic-network__node ${
                      position.kind ? `traffic-network__node--${position.kind}` : ""
                    }`}
                  />
                  <text
                    x={position.x}
                    y={position.y + 5}
                    className="traffic-network__node-label"
                    textAnchor="middle"
                  >
                    {node}
                  </text>
                </g>
              ))}
            </svg>
          </div>
        </article>

        <article className="traffic-panel">
          <div className="traffic-panel__header">
            <div>
              <span className="traffic-panel__kicker">Player input</span>
              <h2>Submit the max flow</h2>
            </div>
            <p>Use the network or edge list below, then send your best answer to the backend.</p>
          </div>

          <form className="traffic-form" onSubmit={handleSubmit}>
            <label className="traffic-field">
              <span>Maximum flow from A to T</span>
              <input
                type="number"
                min="0"
                inputMode="numeric"
                placeholder="Enter the max flow"
                value={userAnswer}
                onChange={(event) => setUserAnswer(event.target.value)}
                disabled={isGenerating || isSubmitting}
              />
            </label>

            <div className="traffic-form__actions">
              <button
                type="submit"
                className="traffic-button traffic-button--primary"
                disabled={isGenerating || isSubmitting || roads.length === 0}
              >
                {isSubmitting ? "Submitting..." : "Submit Answer"}
              </button>
            </div>
          </form>

          <div className="traffic-metrics">
            {metrics.map((metric) => (
              <div key={metric.label} className={`traffic-metric-card ${metric.accent}`}>
                <span>{metric.label}</span>
                <strong>{metric.value}</strong>
              </div>
            ))}
          </div>
        </article>
      </section>

      <section className="traffic-grid traffic-grid--bottom">
        <article className="traffic-panel">
          <div className="traffic-panel__header">
            <div>
              <span className="traffic-panel__kicker">Edge list</span>
              <h2>Road capacities</h2>
            </div>
            <p>Each generated round keeps the same routes but randomizes capacities from 5 to 15.</p>
          </div>

          <div className="traffic-road-list">
            {edgeCards.map((edge) => (
              <div key={getEdgeKey(edge.startNode, edge.endNode)} className="traffic-road-card">
                <div>
                  <strong>
                    {edge.startNode} to {edge.endNode}
                  </strong>
                  <span>Directed road segment</span>
                </div>
                <span className="traffic-road-card__capacity">
                  {edge.capacity ?? "--"} veh/min
                </span>
              </div>
            ))}
          </div>
        </article>

        <article className="traffic-panel">
          <div className="traffic-panel__header">
            <div>
              <span className="traffic-panel__kicker">Round result</span>
              <h2>Evaluation log</h2>
            </div>
            <p>The backend checks the submitted answer and controls the official result.</p>
          </div>

          <div className="traffic-result-card">
            {!submission ? (
              <p className="traffic-result-card__placeholder">
                No answer submitted yet. Generate a network, calculate the flow, and submit a value.
              </p>
            ) : submission.correct ? (
              <>
                <span className="traffic-badge traffic-badge--success">Correct</span>
                <h3>
                  {submission.playerName || "Player"} found the maximum flow.
                </h3>
                <p>
                  Confirmed answer: <strong>{submission.correctAnswer}</strong>
                </p>
                <div className="traffic-result-card__algorithms">
                  <div>
                    <span>Dinic</span>
                    <strong>{dinicRun ? dinicRun.maxFlow : "--"}</strong>
                  </div>
                  <div>
                    <span>Ford-Fulkerson</span>
                    <strong>{fordRun ? fordRun.maxFlow : "--"}</strong>
                  </div>
                </div>
              </>
            ) : (
              <>
                <span className="traffic-badge traffic-badge--warning">Incorrect</span>
                <h3>That answer does not match the network’s maximum flow.</h3>
                <p>Review the cut points and try another calculation.</p>
              </>
            )}
          </div>
        </article>
      </section>
    </div>
  );
}
