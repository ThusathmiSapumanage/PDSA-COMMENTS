package com.pdsa.games.common.gameSession;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Game_Session", indexes = @Index(name = "idx_created_at", columnList = "Created_At"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameSessionModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Session_Id")
    private Integer sessionId;

    @Column(name = "Game_Id", nullable = false)
    private Integer gameId;

    @Column(name = "Player_Id", nullable = false)
    private Integer playerId;

    @Column(name = "Created_At", nullable = false, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
