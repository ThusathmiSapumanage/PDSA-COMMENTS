package com.pdsa.games.snakeladder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the result/summary of one Snake and Ladder game session.
 * Maps to the Snake_And_Ladder_Game table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnakeLadderGame {

    private int sessionId;
    private int nValue; // Board size N (6–12), board is N×N
    private int minimumThrows; // The correct answer: fewest dice throws to finish
}