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

    public AlgorithmService(AlgorithmRepository algorithmRepository) {
        this.algorithmRepository = algorithmRepository;
    }

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

    public List<AlgorithmModel> getAllAlgorithms() {
        return algorithmRepository.findAll();
    }

    public Optional<AlgorithmModel> getAlgorithmById(Integer algorithmId) {
		return algorithmRepository.findById(algorithmId);
	}

    public List<AlgorithmModel> getAlgorithmsByGameId(Integer gameId) {
        return algorithmRepository.findAllByGameId(gameId);
    }

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

    public void deleteAlgorithm(Integer algorithmId) {
        algorithmRepository.deleteById(algorithmId);
    }
}
