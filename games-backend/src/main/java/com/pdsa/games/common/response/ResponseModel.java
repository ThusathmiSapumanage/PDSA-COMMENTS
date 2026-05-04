package com.pdsa.games.common.response;

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
@Table(name = "Response")
public class ResponseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Response_Id", nullable = false)
    @Schema(
            description = "Auto-generated unique response identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "1")
    private Integer responseId;

    @Column(name = "Session_Id", nullable = false)
    @Schema(description = "Foreign key referencing the game session", example = "1")
    private Integer sessionId;

    @Column(name = "Response", nullable = false, length = 255)
    @Schema(description = "Submitted answer value", example = "14")
    private String response;

    @Column(name = "Is_Correct", nullable = false)
    @Schema(description = "Whether the submitted answer is correct", example = "true")
    private Boolean isCorrect;
}
