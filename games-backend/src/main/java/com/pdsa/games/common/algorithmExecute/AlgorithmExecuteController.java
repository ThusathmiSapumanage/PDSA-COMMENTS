package com.pdsa.games.common.algorithmExecute;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/algorithm-executions")
@Tag(name = "Algorithm Executions", description = "Create, list, fetch, update and delete algorithm execution records")
public class AlgorithmExecuteController {

	private final AlgorithmExecuteService algorithmExecuteService;

	public AlgorithmExecuteController(AlgorithmExecuteService algorithmExecuteService) {
		this.algorithmExecuteService = algorithmExecuteService;
	}

	@PostMapping
	@Operation(
			summary = "Create algorithm execution record",
			description = "Creates a new execution record keyed by sessionId and algorithmId.",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					description = "Execution payload used to create a new record",
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = AlgorithmExecuteModel.class),
							examples = @ExampleObject(
									name = "Create execution request",
									value = "{\n  \"sessionId\": 1,\n  \"algorithmId\": 1,\n  \"executionTimeMs\": 12.345,\n  \"outputResult\": \"SUCCESS\"\n}"
							)
					)
			)
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "201",
					description = "Execution record created successfully",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlgorithmExecuteModel.class))
			),
			@ApiResponse(
					responseCode = "400",
					description = "Invalid request body",
					content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
			),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	public ResponseEntity<AlgorithmExecuteModel> createAlgorithmExecute(@RequestBody AlgorithmExecuteModel algorithmExecute) {
		AlgorithmExecuteModel savedAlgorithmExecute = algorithmExecuteService.saveAlgorithmExecute(algorithmExecute);
		return ResponseEntity.status(HttpStatus.CREATED).body(savedAlgorithmExecute);
	}

	@GetMapping
	@Operation(summary = "List execution records", description = "Returns all algorithm execution records.")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Execution records retrieved successfully",
					content = @Content(
							mediaType = "application/json",
							array = @ArraySchema(schema = @Schema(implementation = AlgorithmExecuteModel.class))
					)
			),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	public ResponseEntity<List<AlgorithmExecuteModel>> getAllAlgorithmExecutions() {
		return ResponseEntity.ok(algorithmExecuteService.getAllAlgorithmExecutions());
	}

	@GetMapping("/session/{sessionId}")
	@Operation(summary = "List execution records by session id", description = "Returns execution records for the specified session id.")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Execution records retrieved successfully",
					content = @Content(
							mediaType = "application/json",
							array = @ArraySchema(schema = @Schema(implementation = AlgorithmExecuteModel.class))
					)
			),
			@ApiResponse(responseCode = "404", description = "No execution records found for the provided session id"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	public ResponseEntity<List<AlgorithmExecuteModel>> getAlgorithmExecutionsBySessionId(
			@Parameter(description = "Unique session identifier", example = "1")
			@PathVariable Integer sessionId) {
		List<AlgorithmExecuteModel> records = algorithmExecuteService.getAlgorithmExecutionsBySessionId(sessionId);
		if (records.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(records);
	}

	@GetMapping("/game/{gameId}")
	@Operation(summary = "List execution records by game id", description = "Returns execution records for the specified game id.")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Execution records retrieved successfully",
					content = @Content(
							mediaType = "application/json",
							array = @ArraySchema(schema = @Schema(implementation = AlgorithmExecuteModel.class))
					)
			),
			@ApiResponse(responseCode = "404", description = "No execution records found for the provided game id"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	public ResponseEntity<List<AlgorithmExecuteModel>> getAlgorithmExecutionsByGameId(
			@Parameter(description = "Unique game identifier", example = "1")
			@PathVariable Integer gameId) {
		List<AlgorithmExecuteModel> records = algorithmExecuteService.getAlgorithmExecutionsByGameId(gameId);
		if (records.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(records);
	}

	@GetMapping("/output/{outputResult}")
	@Operation(summary = "List execution records by output result", description = "Returns execution records filtered by output result type.")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Execution records retrieved successfully",
					content = @Content(
							mediaType = "application/json",
							array = @ArraySchema(schema = @Schema(implementation = AlgorithmExecuteModel.class))
					)
			),
			@ApiResponse(responseCode = "404", description = "No execution records found for the provided output result"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	public ResponseEntity<List<AlgorithmExecuteModel>> getAlgorithmExecutionsByOutputResult(
			@Parameter(description = "Output result value", example = "SUCCESS")
			@PathVariable AlgorithmExecuteOutputResult outputResult) {
		List<AlgorithmExecuteModel> records = algorithmExecuteService.getAlgorithmExecutionsByOutputResult(outputResult);
		if (records.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(records);
	}

	@GetMapping("/{sessionId}/{algorithmId}")
	@Operation(summary = "Get execution record by composite id", description = "Fetches a single execution record by sessionId and algorithmId.")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Execution record found",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlgorithmExecuteModel.class))
			),
			@ApiResponse(responseCode = "404", description = "No execution record exists with the provided ids"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	public ResponseEntity<AlgorithmExecuteModel> getAlgorithmExecuteById(
			@Parameter(description = "Unique session identifier", example = "1")
			@PathVariable Integer sessionId,
			@Parameter(description = "Unique algorithm identifier", example = "1")
			@PathVariable Integer algorithmId) {
		return algorithmExecuteService.getAlgorithmExecuteById(sessionId, algorithmId)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PutMapping("/{sessionId}/{algorithmId}")
	@Operation(summary = "Replace execution record", description = "Fully updates the execution record identified by sessionId and algorithmId.")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Execution record updated successfully",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlgorithmExecuteModel.class))
			),
			@ApiResponse(
					responseCode = "400",
					description = "Invalid request body",
					content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
			),
			@ApiResponse(responseCode = "404", description = "No execution record exists with the provided ids"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	public ResponseEntity<AlgorithmExecuteModel> updateAlgorithmExecute(
			@Parameter(description = "Unique session identifier", example = "1")
			@PathVariable Integer sessionId,
			@Parameter(description = "Unique algorithm identifier", example = "1")
			@PathVariable Integer algorithmId,
			@RequestBody AlgorithmExecuteModel algorithmExecute) {
		return algorithmExecuteService.updateAlgorithmExecute(sessionId, algorithmId, algorithmExecute)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PatchMapping("/{sessionId}/{algorithmId}")
	@Operation(summary = "Partially update execution record", description = "Partially updates mutable fields of the execution record identified by sessionId and algorithmId.")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Execution record updated successfully",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlgorithmExecuteModel.class))
			),
			@ApiResponse(
					responseCode = "400",
					description = "Invalid patch body",
					content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
			),
			@ApiResponse(responseCode = "404", description = "No execution record exists with the provided ids"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	public ResponseEntity<AlgorithmExecuteModel> patchAlgorithmExecute(
			@Parameter(description = "Unique session identifier", example = "1")
			@PathVariable Integer sessionId,
			@Parameter(description = "Unique algorithm identifier", example = "1")
			@PathVariable Integer algorithmId,
			@RequestBody AlgorithmExecuteModel algorithmExecute) {
		return algorithmExecuteService.patchAlgorithmExecute(sessionId, algorithmId, algorithmExecute)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{sessionId}/{algorithmId}")
	@Operation(summary = "Delete execution record", description = "Deletes the execution record identified by sessionId and algorithmId.")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Execution record deleted successfully"),
			@ApiResponse(responseCode = "404", description = "No execution record exists with the provided ids"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	public ResponseEntity<Void> deleteAlgorithmExecute(
			@Parameter(description = "Unique session identifier", example = "1")
			@PathVariable Integer sessionId,
			@Parameter(description = "Unique algorithm identifier", example = "1")
			@PathVariable Integer algorithmId) {
		if (algorithmExecuteService.getAlgorithmExecuteById(sessionId, algorithmId).isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		algorithmExecuteService.deleteAlgorithmExecute(sessionId, algorithmId);
		return ResponseEntity.noContent().build();
	}

}
