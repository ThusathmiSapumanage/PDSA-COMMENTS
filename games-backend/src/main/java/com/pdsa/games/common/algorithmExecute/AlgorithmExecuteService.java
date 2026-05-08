package com.pdsa.games.common.algorithmExecute;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlgorithmExecuteService {

	private final AlgorithmExecuteRepository algorithmExecuteRepository;

	/**
	 * Construct the algorithm execution service.
	 *
	 * @param algorithmExecuteRepository repository used to access execution records
	 */
	public AlgorithmExecuteService(AlgorithmExecuteRepository algorithmExecuteRepository) {
		this.algorithmExecuteRepository = algorithmExecuteRepository;
	}

	/**
	 * Save a new algorithm execution record after validating required fields.
	 *
	 * @param algorithmExecute execution payload to persist
	 * @return persisted execution record
	 */
	public AlgorithmExecuteModel saveAlgorithmExecute(AlgorithmExecuteModel algorithmExecute) {
		if (algorithmExecute == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
		}

		if (algorithmExecute.getSessionId() == null) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"sessionId is required when creating an execution record"
			);
		}

		if (algorithmExecute.getAlgorithmId() == null) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"algorithmId is required when creating an execution record"
			);
		}

		AlgorithmExecuteId id = new AlgorithmExecuteId(
				algorithmExecute.getSessionId(),
				algorithmExecute.getAlgorithmId()
		);

		if (algorithmExecuteRepository.existsById(id)) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Execution record already exists for the given sessionId and algorithmId"
			);
		}

		validateExecutionTimeMs(algorithmExecute.getExecutionTimeMs(), false);
		validateOutputResult(algorithmExecute.getOutputResult(), false);

		return algorithmExecuteRepository.save(algorithmExecute);
	}

	/**
	 * Retrieve all execution records.
	 *
	 * @return list of all execution records
	 */
	public List<AlgorithmExecuteModel> getAllAlgorithmExecutions() {
		return algorithmExecuteRepository.findAll();
	}

	/**
	 * Retrieve a single execution record by session and algorithm ids.
	 *
	 * @param sessionId session identifier
	 * @param algorithmId algorithm identifier
	 * @return optional execution record
	 */
	public Optional<AlgorithmExecuteModel> getAlgorithmExecuteById(Integer sessionId, Integer algorithmId) {
		validatePathIds(sessionId, algorithmId);
		return algorithmExecuteRepository.findById(new AlgorithmExecuteId(sessionId, algorithmId));
	}

	/**
	 * Retrieve execution records by session id.
	 *
	 * @param sessionId session identifier
	 * @return list of execution records for the session
	 */
	public List<AlgorithmExecuteModel> getAlgorithmExecutionsBySessionId(Integer sessionId) {
		if (sessionId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId is required");
		}
		return algorithmExecuteRepository.findAllBySessionId(sessionId);
	}

	/**
	 * Retrieve execution records by game id.
	 *
	 * @param gameId game identifier
	 * @return list of execution records for the game
	 */
	public List<AlgorithmExecuteModel> getAlgorithmExecutionsByGameId(Integer gameId) {
		if (gameId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gameId is required");
		}
		return algorithmExecuteRepository.findAllByGameId(gameId);
	}

	/**
	 * Retrieve execution records by output result.
	 *
	 * @param outputResult result type to filter by
	 * @return list of execution records matching the output result
	 */
	public List<AlgorithmExecuteModel> getAlgorithmExecutionsByOutputResult(AlgorithmExecuteOutputResult outputResult) {
		if (outputResult == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "outputResult is required");
		}
		return algorithmExecuteRepository.findAllByOutputResult(outputResult.name());
	}

	/**
	 * Replace an existing execution record.
	 *
	 * @param sessionId session identifier
	 * @param algorithmId algorithm identifier
	 * @param algorithmExecute replacement execution payload
	 * @return updated execution record when found, or empty otherwise
	 */
	public Optional<AlgorithmExecuteModel> updateAlgorithmExecute(
			Integer sessionId,
			Integer algorithmId,
			AlgorithmExecuteModel algorithmExecute
	) {
		validatePathIds(sessionId, algorithmId);

		if (algorithmExecute == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
		}

		validatePathAndBodyIds(sessionId, algorithmId, algorithmExecute);
		validateExecutionTimeMs(algorithmExecute.getExecutionTimeMs(), false);
		validateOutputResult(algorithmExecute.getOutputResult(), false);

		return algorithmExecuteRepository.findById(new AlgorithmExecuteId(sessionId, algorithmId))
				.map(existing -> {
					existing.setExecutionTimeMs(algorithmExecute.getExecutionTimeMs());
					existing.setOutputResult(algorithmExecute.getOutputResult());
					return algorithmExecuteRepository.save(existing);
				});
	}

	/**
	 * Partially update an existing execution record.
	 *
	 * @param sessionId session identifier
	 * @param algorithmId algorithm identifier
	 * @param algorithmExecute payload containing fields to patch
	 * @return updated execution record when found, or empty otherwise
	 */
	public Optional<AlgorithmExecuteModel> patchAlgorithmExecute(
			Integer sessionId,
			Integer algorithmId,
			AlgorithmExecuteModel algorithmExecute
	) {
		validatePathIds(sessionId, algorithmId);

		if (algorithmExecute == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
		}

		validatePathAndBodyIds(sessionId, algorithmId, algorithmExecute);
		validateExecutionTimeMs(algorithmExecute.getExecutionTimeMs(), true);
		validateOutputResult(algorithmExecute.getOutputResult(), true);

		if (algorithmExecute.getExecutionTimeMs() == null
				&& algorithmExecute.getOutputResult() == null) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"At least one updatable field must be provided"
			);
		}

		return algorithmExecuteRepository.findById(new AlgorithmExecuteId(sessionId, algorithmId))
				.map(existing -> {
					if (algorithmExecute.getExecutionTimeMs() != null) {
						existing.setExecutionTimeMs(algorithmExecute.getExecutionTimeMs());
					}

					if (algorithmExecute.getOutputResult() != null) {
						existing.setOutputResult(algorithmExecute.getOutputResult());
					}

					return algorithmExecuteRepository.save(existing);
				});
	}

	/**
	 * Delete an execution record by composite id.
	 *
	 * @param sessionId session identifier
	 * @param algorithmId algorithm identifier
	 */
	public void deleteAlgorithmExecute(Integer sessionId, Integer algorithmId) {
		validatePathIds(sessionId, algorithmId);

		AlgorithmExecuteId id = new AlgorithmExecuteId(sessionId, algorithmId);
		if (!algorithmExecuteRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Execution record not found");
		}

		algorithmExecuteRepository.deleteById(id);
	}

	/**
	 * Validate that both composite path identifiers are present.
	 *
	 * @param sessionId session identifier
	 * @param algorithmId algorithm identifier
	 */
	private void validatePathIds(Integer sessionId, Integer algorithmId) {
		if (sessionId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId is required");
		}

		if (algorithmId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "algorithmId is required");
		}
	}

	/**
	 * Validate that path ids and body ids match when supplied.
	 *
	 * @param sessionId session identifier from the path
	 * @param algorithmId algorithm identifier from the path
	 * @param algorithmExecute payload containing body ids
	 */
	private void validatePathAndBodyIds(
			Integer sessionId,
			Integer algorithmId,
			AlgorithmExecuteModel algorithmExecute
	) {
		if (algorithmExecute.getSessionId() != null
				&& !sessionId.equals(algorithmExecute.getSessionId())) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"sessionId in path and body must match"
			);
		}

		if (algorithmExecute.getAlgorithmId() != null
				&& !algorithmId.equals(algorithmExecute.getAlgorithmId())) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"algorithmId in path and body must match"
			);
		}
	}

	/**
	 * Validate the execution time value.
	 *
	 * @param executionTimeMs execution duration in milliseconds
	 * @param allowNull whether null is allowed for patch operations
	 */
	private void validateExecutionTimeMs(BigDecimal executionTimeMs, boolean allowNull) {
		if (executionTimeMs == null) {
			if (allowNull) {
				return;
			}
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "executionTimeMs is required");
		}

		if (executionTimeMs.signum() < 0) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"executionTimeMs must be non-negative"
			);
		}
	}

	/**
	 * Validate the output result enumeration.
	 *
	 * @param outputResult execution output result
	 * @param allowNull whether null is allowed for patch operations
	 */
	private void validateOutputResult(AlgorithmExecuteOutputResult outputResult, boolean allowNull) {
		if (outputResult == null && !allowNull) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "outputResult is required");
		}
	}
}