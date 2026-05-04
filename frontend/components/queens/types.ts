export type Position = {
  x: number;
  y: number;
};

export type StartRequest = {
  boardSize: number;
  queenCount: number;
  timeBudgetMs?: number;
  skipCache?: boolean;
};

export type StartResponse = {
  sessionId: number;
  boardSize: number;
  queenCount: number;
  totalSolutions: number;
  sequentialTimeMs: number;
  threadedTimeMs: number;
  sequentialFound: number;
  threadedFound: number;
  sequentialHitLimit: boolean;
  threadedHitLimit: boolean;
  timeBudgetMs: number | null;
  message: string;
  status: "READY" | "RUNNING";
};

export type ProgressResponse = {
  sessionId: number;
  boardSize: number;
  queenCount: number;
  sequentialFound: number;
  threadedFound: number;
  sequentialTimeMs: number;
  threadedTimeMs: number;
  sequentialDone: boolean;
  threadedDone: boolean;
  status: "RUNNING" | "COMPLETED" | "CANCELLED" | "FAILED";
  totalSolutions: number | null;
  message: string | null;
};

export type SubmitRequest = {
  sessionId: number;
  queens: Position[];
};

export type SubmitResponse = {
  sessionId: number;
  playerId: number;
  correct: boolean;
  alreadyDiscovered: boolean;
  solutionsDiscovered: number;
  totalSolutions: number;
  message: string;
};

export type StatusResponse = {
  sessionId: number;
  totalSolutions: number;
  solutionsDiscovered: number;
  message: string;
};

export type ApiError = {
  message?: string;
  error?: string;
};
