package com.pdsa.games.common.algorithm;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlgorithmService {
    private static final int ALGORITHM_NAME_MAX_LENGTH = 45;

    private final AlgorithmRepository algorithmRepository;

    /**
     * Construct the algorithm service.
     *
     * @param algorithmRepository repository used to access algorithm data
     */
    public AlgorithmService(AlgorithmRepository algorithmRepository) {
        this.algorithmRepository = algorithmRepository;
    }

    /**
     * Save a new algorithm after validating the payload.
     *
     * @param algorithm algorithm data to persist
     * @return persisted algorithm with generated id
     */
    public AlgorithmModel saveAlgorithm(AlgorithmModel algorithm) {
        if (algorithm.getAlgorithmId() != null) {
            throw new IllegalArgumentException("algorithmId must not be provided when creating an algorithm");
        }

        if (algorithm.getGameId() == null) {
            throw new IllegalArgumentException("gameId is required when creating an algorithm");
        }

        validateAlgorithmName(algorithm.getAlgorithmName(), false);
        return algorithmRepository.save(algorithm);
    }

    /**
     * Retrieve all stored algorithms.
     *
     * @return list of algorithm models
     */
    public List<AlgorithmModel> getAllAlgorithms() {
        return algorithmRepository.findAll();
    }

    /**
     * Retrieve a single algorithm by its unique identifier.
     *
     * @param algorithmId identifier of the algorithm to fetch
     * @return optional algorithm model
     */
    public Optional<AlgorithmModel> getAlgorithmById(Integer algorithmId) {
		return algorithmRepository.findById(algorithmId);
	}

    /**
     * Retrieve algorithms by the associated game identifier.
     *
     * @param gameId game id used to filter algorithms
     * @return list of algorithms for the specified game
     */
    public List<AlgorithmModel> getAlgorithmsByGameId(Integer gameId) {
        return algorithmRepository.findAllByGameId(gameId);
    }

    /**
     * Update the algorithm with the given id by replacing mutable fields.
     *
     * @param algorithmId identifier of the algorithm to update
     * @param algorithm payload containing new algorithm details
     * @return updated algorithm when found; empty otherwise
     */
    public Optional<AlgorithmModel> updateAlgorithm(Integer algorithmId, AlgorithmModel algorithm) {
		if (algorithm.getAlgorithmId() != null && !algorithmId.equals(algorithm.getAlgorithmId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gameId in path and body must match");
		}

		validateAlgorithmName(algorithm.getAlgorithmName(), false);

		return algorithmRepository.findById(algorithmId)
				.map(existing -> {
					existing.setAlgorithmName(algorithm.getAlgorithmName());
					return algorithmRepository.save(existing);
				});
	}

    /**
     * Partially update selected fields of an existing algorithm.
     *
     * @param algorithmId identifier of the algorithm to patch
     * @param algorithm algorithm payload containing patch fields
     * @return patched algorithm when found; empty otherwise
     */
    public Optional<AlgorithmModel> patchAlgorithm(Integer algorithmId, AlgorithmModel algorithm) {
        if (algorithm.getAlgorithmId() != null && !algorithmId.equals(algorithm.getAlgorithmId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "algorithmId in path and body must match");
        }
        validateAlgorithmName(algorithm.getAlgorithmName(), true);

        return algorithmRepository.findById(algorithmId)
                .map(existing -> {
                    if (algorithm.getAlgorithmName() != null) {
                        existing.setAlgorithmName(algorithm.getAlgorithmName());
                    }
                    return algorithmRepository.save(existing);
                });
    }

    /**
     * Validate the algorithm name for required constraints.
     *
     * @param algorithmName algorithm name to validate
     * @param allowNull whether null names are permitted for patch operations
     */
    public void validateAlgorithmName(String algorithmName, boolean allowNull) {
        if (algorithmName == null) {
			if (allowNull) {
				return;
			}
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "algorithmName is required");
		}
        
        if (algorithmName.isBlank()) {
            throw new IllegalArgumentException("algorithmName must not be blank");
        }

        if (algorithmName.length() > ALGORITHM_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("algorithmName must be at most " + ALGORITHM_NAME_MAX_LENGTH + " characters");
        }
    }

    /**
     * Delete the algorithm with the provided id.
     *
     * @param algorithmId identifier of the algorithm to delete
     */
    public void deleteAlgorithm(Integer algorithmId) {
        algorithmRepository.deleteById(algorithmId);
    }
}
