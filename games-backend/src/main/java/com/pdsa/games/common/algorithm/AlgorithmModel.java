package com.pdsa.games.common.algorithm;

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
@Table(name = "Algorithm")
public class AlgorithmModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Algorithm_Id", nullable = false)
    @Schema(
            description = "Auto-generated unique algorithm identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "1")
    private Integer algorithmId;

    @Column(name = "Algorithm_Name", nullable = false, length = 45, unique = true)
    @Schema(description = "Name of the algorithm", minLength = 1, maxLength = 45, example = "Knight Tour - Warnsdorff")
    private String algorithmName;

    @Column(name = "Game_Id", nullable = false)
    @Schema(
        description = "Foreign key referencing the related game",
        example = "1")
    private Integer gameId;
}
