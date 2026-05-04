package com.pdsa.games.common.algorithmExecute;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "Algorithm_Execute")
@IdClass(AlgorithmExecuteId.class)
public class AlgorithmExecuteModel {

	@Id
	@Column(name = "Session_Id", nullable = false)
	@Schema(description = "Foreign key referencing the game session", example = "1")
	private Integer sessionId;

	@Id
	@Column(name = "Algorithm_Id", nullable = false)
	@Schema(description = "Foreign key referencing the algorithm", example = "1")
	private Integer algorithmId;

	@Column(name = "Execution_Time_MS", nullable = false, precision = 10, scale = 3)
	@Schema(description = "Execution time in milliseconds", example = "12.345")
	private BigDecimal executionTimeMs;

	@Enumerated(EnumType.STRING)
	@Column(name = "Output_Result", nullable = false)
	@Schema(description = "Execution result of the algorithm", example = "SUCCESS")
	private AlgorithmExecuteOutputResult outputResult;

	@Column(name = "Max_Flow_Result")
    private Integer maxFlowResult;

}
