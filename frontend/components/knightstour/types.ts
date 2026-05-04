export type Move = {
  stepNo: number;
  x: number;
  y: number;
};

export type AlgorithmMetric = {
  algorithmName: string;
  executionTimeMs: number;
  moveCount: number;
  status: string;
};

export type StartRequest = {
  boardSize: 8 | 16;
};

export type StartResponse = {
  sessionId: number;
  playerId: number;
  boardSize: number;
  startX: number;
  startY: number;
  expectedMoveCount: number;
  algorithmName: string;
  algorithmExecutionTimeMs: number;
  comparisonSummary: string;
  algorithmMetrics: AlgorithmMetric[];
  status: string;
  message: string;
};

export type SubmitRequest = {
  sessionId: number;
  moves: Move[];
};

export type SubmitResponse = {
  responseId: number;
  sessionId: number;
  playerId: number;

  correct: boolean;
  validTour: boolean;
  matchedSavedSolution: boolean;
  outcome: "WIN" | "LOSE" | "DRAW";

  expectedMoveCount: number;
  submittedMoveCount: number;

  structuralValid: boolean;
  completeTour: boolean;
  deadEnd: boolean;
  warnsdorffValid: boolean;
  backtrackingValid: boolean;
  validationSource: string | null;
  reasonCode: string;

  warnsdorffCorrectMoves: Move[];
  warnsdorffMoveCount: number;
  backtrackingCorrectMoves: Move[];
  backtrackingMoveCount: number;

  message: string;
};

export type HintRequest = {
  sessionId: number;
  moves: Move[];
};

export type HintResponse = {
  sessionId: number;
  playerId: number;
  fromStepNo: number;
  fromX: number;
  fromY: number;
  hintMoves: Move[];
  message: string;
};

export type CorrectAnswerResponse = {
  sessionId: number;
  playerId: number;

  warnsdorffMoves: Move[];
  warnsdorffMoveCount: number;

  backtrackingMoves: Move[];
  backtrackingMoveCount: number;

  message: string;
};

export type ApiError = {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  title?: string;
  detail?: string;
  path?: string;
  instance?: string;
};
