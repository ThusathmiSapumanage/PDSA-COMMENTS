package com.pdsa.games.traffic.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pdsa.games.traffic.model.Road;
import com.pdsa.games.traffic.service.TrafficService;

@RestController
@RequestMapping("/api/traffic-sim")
@CrossOrigin(origins = "*") // Allow localhost frontend
public class TrafficController {

    private final TrafficService trafficService;

    // Connects the controller to the traffic service so each endpoint can use the game logic.
    public TrafficController(TrafficService trafficService) {
        this.trafficService = trafficService;
    }

    // Creates a new traffic network for the given session and sends the roads to the frontend.
    @PostMapping("/generate/{sessionId}")
    public ResponseEntity<List<Road>> generateNetwork(@PathVariable Integer sessionId) {
        List<Road> generatedRoads = trafficService.generateTrafficNetwork(sessionId);
        return ResponseEntity.ok(generatedRoads);
    }

    // Runs Dinic's algorithm and returns both the max flow and the execution time.
    @GetMapping("/dinic/{sessionId}")
    public ResponseEntity<Map<String, Object>> calculateDinic(@PathVariable Integer sessionId) {
        TrafficService.AlgorithmRunResult result = trafficService.calculateDinicMaxFlow(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("algorithm", result.getAlgorithm());
        response.put("maxFlow", result.getMaxFlow());
        response.put("timeMs", result.getTimeMs());

        return ResponseEntity.ok(response);
    }

    // Runs Ford-Fulkerson for the same traffic network so its result and speed can be compared.
    @GetMapping("/ford/{sessionId}")
    public ResponseEntity<Map<String, Object>> calculateFord(@PathVariable Integer sessionId) {
        TrafficService.AlgorithmRunResult result = trafficService.calculateFordFulkersonMaxFlow(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("algorithm", result.getAlgorithm());
        response.put("maxFlow", result.getMaxFlow());
        response.put("timeMs", result.getTimeMs());
        return ResponseEntity.ok(response);
    }

    // Checks the player's submitted answer against the saved max flow.
    @PostMapping("/submit/{sessionId}")
    public ResponseEntity<Map<String, Object>> submitAnswer(
            @PathVariable Integer sessionId,
            @RequestParam Integer answer
    ) {
        TrafficService.AnswerSubmissionResult result = trafficService.submitAnswer(sessionId, answer);

        Map<String, Object> response = new HashMap<>();
        response.put("correct", result.isCorrect());
        response.put("playerName", result.getPlayerName());
        response.put("correctAnswer", result.getCorrectAnswer());

        return ResponseEntity.ok(response);
    }
}
