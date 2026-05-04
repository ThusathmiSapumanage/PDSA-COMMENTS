// ─── Domain types (mirror the Java models exactly) ───────────────────────────

export type PathType = "SNAKE" | "LADDER";

/** One snake or one ladder on the board — matches SnakeLadderItem.java */
export interface BoardItem {
  sessionId: number;
  startCell: number;
  endCell:   number;
  pathType:  PathType;
}

/** Result from running one algorithm — matches AlgorithmResult.java */
export interface AlgorithmResult {
  algorithmName:  string;   // "BFS" | "Dynamic Programming"
  minimumThrows:  number;
  executionTimeMs: number;
  success:        boolean;
}

/** The full payload returned by POST /api/snake-ladder/start */
export interface RoundData {
  n:                number;
  items:            BoardItem[];
  algorithmResults: AlgorithmResult[];
  choices:          number[];         // 3 integers the player picks from
}

/** Overall game state held in the page component */
export interface GameState extends RoundData {}

/** Outcome after the player submits their answer */
export type Outcome = "WIN" | "LOSE" | "DRAW";
