package com.pdsa.games.snakeladder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single snake or ladder on the board.
 * Maps to the Snake_And_Ladder_Item table.
 *
 * For a LADDER: startCell < endCell (you climb up)
 * For a SNAKE: startCell > endCell (you slide down)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnakeLadderItem {

    public enum PathType {
        SNAKE, LADDER
    }

    private int sessionId;
    private int startCell;
    private int endCell;
    private PathType pathType;

    @Override
    public String toString() {
        return pathType + ": " + startCell + " -> " + endCell;
    }
}