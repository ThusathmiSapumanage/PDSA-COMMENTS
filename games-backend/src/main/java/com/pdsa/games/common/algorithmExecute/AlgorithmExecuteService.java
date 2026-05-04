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

	public AlgorithmExecuteService(AlgorithmExecuteRepository algorithmExecuteRepository) {
		this.algorithmExecuteRepository = algorithmExecuteRepository;
	}

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

	public List<AlgorithmExecuteModel> getAllAlgorithmExecutions() {
		return algorithmExecuteRepository.findAll();
	}

	public Optional<AlgorithmExecuteModel> getAlgorithmExecuteById(Integer sessionId, Integer algorithmId) {
		validatePathIds(sessionId, algorithmId);
		return algorithmExecuteRepository.findById(new AlgorithmExecuteId(sessionId, algorithmId));
	}

	public List<AlgorithmExecuteModel> getAlgorithmExecutionsBySessionId(Integer sessionId) {
		if (sessionId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId is required");
		}
		return algorithmExecuteRepository.findAllBySessionId(sessionId);
	}

	public List<AlgorithmExecuteModel> getAlgorithmExecutionsByGameId(Integer gameId) {
		if (gameId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gameId is required");
		}
		return algorithmExecuteRepository.findAllByGameId(gameId);
	}

	public List<AlgorithmExecuteModel> getAlgorithmExecutionsByOutputResult(AlgorithmExecuteOutputResult outputResult) {
		if (outputResult == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "outputResult is required");
		}
		return algorithmExecuteRepository.findAllByOutputResult(outputResult.name());
	}

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

	public void deleteAlgorithmExecute(Integer sessionId, Integer algorithmId) {
		validatePathIds(sessionId, algorithmId);

		AlgorithmExecuteId id = new AlgorithmExecuteId(sessionId, algorithmId);
		if (!algorithmExecuteRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Execution record not found");
		}

		algorithmExecuteRepository.deleteById(id);
	}

	private void validatePathIds(Integer sessionId, Integer algorithmId) {
		if (sessionId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId is required");
		}

		if (algorithmId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "algorithmId is required");
		}
	}

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

	private void validateOutputResult(AlgorithmExecuteOutputResult outputResult, boolean allowNull) {
		if (outputResult == null && !allowNull) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "outputResult is required");
		}
	}
}