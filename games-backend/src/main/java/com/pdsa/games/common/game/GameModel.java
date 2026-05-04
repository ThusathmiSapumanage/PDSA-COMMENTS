package com.pdsa.games.common.game;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "Game")
public class GameModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Game_Id", nullable = false)
    @Schema(
            description = "Auto-generated unique game identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "1")
    private Integer gameId;

    @Column(name = "Game_Name", nullable = false, length = 25)
    @Schema(description = "Name of the game", minLength = 1, maxLength = 25, example = "Knight's Tour")
    private String gameName;
}
