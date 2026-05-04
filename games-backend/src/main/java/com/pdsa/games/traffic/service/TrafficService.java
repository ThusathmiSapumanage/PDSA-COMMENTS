package com.pdsa.games.traffic.service;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pdsa.games.common.algorithm.AlgorithmModel;
import com.pdsa.games.common.algorithm.AlgorithmRepository;
import com.pdsa.games.common.algorithmExecute.AlgorithmExecuteId;
import com.pdsa.games.common.algorithmExecute.AlgorithmExecuteModel;
import com.pdsa.games.common.algorithmExecute.AlgorithmExecuteOutputResult;
import com.pdsa.games.common.algorithmExecute.AlgorithmExecuteRepository;
import com.pdsa.games.common.gameSession.GameSessionModel;
import com.pdsa.games.common.gameSession.GameSessionRepository;
import com.pdsa.games.common.player.PlayerModel;
import com.pdsa.games.common.player.PlayerRepository;
import com.pdsa.games.common.response.ResponseModel;
import com.pdsa.games.common.response.ResponseRepository;
import com.pdsa.games.traffic.model.Road;
import com.pdsa.games.traffic.model.RoadId;
import com.pdsa.games.traffic.model.TrafficSimGame;
import com.pdsa.games.traffic.repository.RoadRepository;
import com.pdsa.games.traffic.repository.TrafficSimGameRepository;

@Service
public class TrafficService {

    // Fixed source and sink used by the traffic max-flow game.
    private static final String SOURCE = "A";
    private static final String SINK = "T";
    private static final String DINIC_NAME = "Traffic - Dinic";
    private static final String FORD_FULKERSON_NAME = "Traffic - Ford-Fulkerson";

    // Template network. A generated game assigns a random capacity to each directed road.
    private static final String[][] EDGES = {
            {"A", "B"}, {"A", "C"}, {"A", "D"},
            {"B", "E"}, {"B", "F"},
            {"C", "E"}, {"C", "F"},
            {"D", "F"},
            {"E", "G"}, {"E", "H"},
            {"F", "H"},
            {"G", "T"}, {"H", "T"}
    };

    private final RoadRepository roadRepository;
    private final TrafficSimGameRepository trafficSimGameRepository;
    private final AlgorithmRepository algorithmRepository;
    private final AlgorithmExecuteRepository algorithmExecuteRepository;
    private final GameSessionRepository gameSessionRepository;
    private final PlayerRepository playerRepository;
    private final ResponseRepository responseRepository;

    public TrafficService(
            RoadRepository roadRepository,
            TrafficSimGameRepository trafficSimGameRepository,
            AlgorithmRepository algorithmRepository,
            AlgorithmExecuteRepository algorithmExecuteRepository,
            GameSessionRepository gameSessionRepository,
            PlayerRepository playerRepository,
            ResponseRepository responseRepository
    ) {
        this.roadRepository = roadRepository;
        this.trafficSimGameRepository = trafficSimGameRepository;
        this.algorithmRepository = algorithmRepository;
        this.algorithmExecuteRepository = algorithmExecuteRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.playerRepository = playerRepository;
        this.responseRepository = responseRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Road> generateTrafficNetwork(Integer sessionId) {
        validateSession(sessionId);

        // Use the session id as the seed so regenerating the same session is repeatable.
        Random random = new Random(sessionId.longValue());

        TrafficSimGame trafficGame = trafficSimGameRepository.findById(sessionId)
                .orElseGet(() -> trafficSimGameRepository.save(new TrafficSimGame(sessionId, 0)));

        // Replace any previous network for this session before saving the new one.
        List<Road> existing = roadRepository.findByIdSessionId(sessionId);
        if (!existing.isEmpty()) {
            roadRepository.deleteAll(existing);
        }

        List<Road> roads = new ArrayList<>();
        for (String[] edge : EDGES) {
            int capacity = random.nextInt(11) + 5;
            roads.add(new Road(new RoadId(sessionId, edge[0], edge[1]), capacity));
        }

        List<Road> saved = roadRepository.saveAll(roads);

        // Store the correct answer server-side so player submissions can be validated later.
        int maxFlow = runDinic(buildCapacityGraphFromRoads(saved));
        trafficGame.setMaxFlowValue(maxFlow);
        trafficSimGameRepository.save(trafficGame);

        return saved;
    }

    @Transactional
    public AlgorithmRunResult calculateDinicMaxFlow(Integer sessionId) {
        // Clone the residual graph because the algorithm mutates capacities while it runs.
        Map<String, Map<String, Integer>> graph = cloneGraph(buildCapacityGraph(sessionId));
        long start = System.nanoTime();

        int maxFlow = runDinic(graph);
        double timeMs = nanosToMs(System.nanoTime() - start);

        saveAlgorithmExecution(sessionId, DINIC_NAME, timeMs, maxFlow, AlgorithmExecuteOutputResult.SUCCESS);
        return new AlgorithmRunResult("Dinic", maxFlow, timeMs);
    }

    @Transactional
    public AlgorithmRunResult calculateFordFulkersonMaxFlow(Integer sessionId) {
        // Ford-Fulkerson also consumes residual capacity, so it needs its own graph copy.
        Map<String, Map<String, Integer>> graph = cloneGraph(buildCapacityGraph(sessionId));
        long start = System.nanoTime();

        int maxFlow = dfsFordFulkerson(graph, SOURCE, SINK);
        double timeMs = nanosToMs(System.nanoTime() - start);

        saveAlgorithmExecution(sessionId, FORD_FULKERSON_NAME, timeMs, maxFlow, AlgorithmExecuteOutputResult.SUCCESS);
        return new AlgorithmRunResult("Ford-Fulkerson", maxFlow, timeMs);
    }

    @Transactional
    public AnswerSubmissionResult submitAnswer(Integer sessionId, Integer answer) {
        if (answer == null || answer < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "answer must be a non-negative integer");
        }

        GameSessionModel session = validateSession(sessionId);
        Integer correct = getSavedMaxFlow(sessionId);

        boolean alreadyCorrect = responseRepository.findBySessionId(sessionId)
                .stream()
                .anyMatch(r -> Boolean.TRUE.equals(r.getIsCorrect()));

        // Only the first correct submission for a session is marked as correct.
        boolean isCorrect = !alreadyCorrect && correct.equals(answer);

        ResponseModel response = new ResponseModel();
        response.setSessionId(sessionId);
        response.setResponse(String.valueOf(answer));
        response.setIsCorrect(isCorrect);
        responseRepository.save(response);

        return new AnswerSubmissionResult(
                isCorrect,
                correct,
                getPlayerName(session.getPlayerId())
        );
    }

    public Integer getSavedMaxFlow(Integer sessionId) {
        validateSession(sessionId);

        return trafficSimGameRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Traffic game result not found"
                ))
                .getMaxFlowValue();
    }

    private Map<String, Map<String, Integer>> buildCapacityGraph(Integer sessionId) {
        validateSession(sessionId);

        List<Road> roads = roadRepository.findByIdSessionId(sessionId);
        if (roads.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Traffic network not found for session " + sessionId
            );
        }

        return buildCapacityGraphFromRoads(roads);
    }

    private Map<String, Map<String, Integer>> buildCapacityGraphFromRoads(List<Road> roads) {
        Map<String, Map<String, Integer>> graph = new HashMap<>();

        for (Road road : roads) {
            String from = road.getId().getStartNode();
            String to = road.getId().getEndNode();
            int cap = road.getCapacity();

            graph.putIfAbsent(from, new HashMap<>());
            graph.putIfAbsent(to, new HashMap<>());
            graph.get(from).put(to, cap);
            // Add reverse edges with zero capacity so residual updates are always valid.
            graph.get(to).putIfAbsent(from, 0);
        }

        if (!graph.containsKey(SOURCE) || !graph.containsKey(SINK)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Traffic network is missing source or sink nodes");
        }

        return graph;
    }

    private Map<String, Map<String, Integer>> cloneGraph(Map<String, Map<String, Integer>> original) {
        Map<String, Map<String, Integer>> copy = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : original.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return copy;
    }

    private int runDinic(Map<String, Map<String, Integer>> graph) {
        List<String> nodes = sortedNodes(graph);
        int maxFlow = 0;

        while (true) {
            // Dinic repeatedly builds levels, then sends blocking flow through valid levels.
            Map<String, Integer> level = buildLevelGraph(graph, nodes);
            if (!level.containsKey(SINK)) {
                return maxFlow;
            }

            Map<String, Integer> nextEdge = new HashMap<>();
            int flow;
            do {
                flow = sendFlow(graph, SOURCE, SINK, Integer.MAX_VALUE, level, nextEdge, nodes);
                maxFlow += flow;
            } while (flow > 0);
        }
    }

    private Map<String, Integer> buildLevelGraph(Map<String, Map<String, Integer>> graph, List<String> nodes) {
        Map<String, Integer> level = new HashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(SOURCE);
        level.put(SOURCE, 0);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String neighbor : orderedNeighbors(graph, current, nodes)) {
                if (graph.get(current).getOrDefault(neighbor, 0) > 0 && !level.containsKey(neighbor)) {
                    level.put(neighbor, level.get(current) + 1);
                    queue.add(neighbor);
                }
            }
        }

        return level;
    }

    private int sendFlow(
            Map<String, Map<String, Integer>> graph,
            String current,
            String sink,
            int flow,
            Map<String, Integer> level,
            Map<String, Integer> nextEdge,
            List<String> nodes
    ) {
        if (current.equals(sink)) {
            return flow;
        }

        List<String> neighbors = orderedNeighbors(graph, current, nodes);
        int startIndex = nextEdge.getOrDefault(current, 0);

        for (int i = startIndex; i < neighbors.size(); i++) {
            // Remember the next neighbor to try so dead edges are not rescanned in this phase.
            nextEdge.put(current, i + 1);

            String neighbor = neighbors.get(i);
            int capacity = graph.get(current).getOrDefault(neighbor, 0);

            if (capacity <= 0 || level.getOrDefault(neighbor, -1) != level.get(current) + 1) {
                continue;
            }

            int bottleneck = sendFlow(
                    graph,
                    neighbor,
                    sink,
                    Math.min(flow, capacity),
                    level,
                    nextEdge,
                    nodes
            );

            if (bottleneck > 0) {
                // Update the residual network after pushing flow through this edge.
                graph.get(current).put(neighbor, capacity - bottleneck);
                graph.get(neighbor).put(current, graph.get(neighbor).getOrDefault(current, 0) + bottleneck);
                return bottleneck;
            }
        }

        return 0;
    }

    private int dfsFordFulkerson(Map<String, Map<String, Integer>> graph, String source, String sink) {
        int maxFlow = 0;

        while (true) {
            // Each DFS finds one augmenting path in the current residual graph.
            Set<String> visited = new HashSet<>();
            int flow = dfs(graph, source, sink, Integer.MAX_VALUE, visited);
            if (flow == 0) {
                break;
            }
            maxFlow += flow;
        }

        return maxFlow;
    }

    private int dfs(Map<String, Map<String, Integer>> graph, String current, String sink, int flow, Set<String> visited) {
        if (current.equals(sink)) {
            return flow;
        }

        visited.add(current);

        for (String neighbor : orderedNeighbors(graph, current, sortedNodes(graph))) {
            int capacity = graph.get(current).getOrDefault(neighbor, 0);
            if (capacity > 0 && !visited.contains(neighbor)) {
                int bottleneck = dfs(graph, neighbor, sink, Math.min(flow, capacity), visited);

                if (bottleneck > 0) {
                    graph.get(current).put(neighbor, capacity - bottleneck);
                    graph.get(neighbor).put(current, graph.get(neighbor).getOrDefault(current, 0) + bottleneck);
                    return bottleneck;
                }
            }
        }

        return 0;
    }

    private List<String> sortedNodes(Map<String, Map<String, Integer>> graph) {
        List<String> nodes = new ArrayList<>(graph.keySet());
        nodes.sort(Comparator.naturalOrder());
        return nodes;
    }

    private List<String> orderedNeighbors(Map<String, Map<String, Integer>> graph, String node, List<String> nodes) {
        List<String> neighbors = new ArrayList<>(graph.getOrDefault(node, Map.of()).keySet());
        // Stable ordering makes both algorithms deterministic for the same network.
        neighbors.sort(Comparator.<String>comparingInt(nodes::indexOf).thenComparing(String::compareTo));
        return neighbors;
    }

    private void saveAlgorithmExecution(
            Integer sessionId,
            String algorithmName,
            double timeMs,
            int maxFlow,
            AlgorithmExecuteOutputResult result
    ) {
        Integer algorithmId = getExistingAlgorithmId(algorithmName);

        AlgorithmExecuteId executionId = new AlgorithmExecuteId(sessionId, algorithmId);
        Optional<AlgorithmExecuteModel> existing = algorithmExecuteRepository.findById(executionId);
        if (existing.isPresent()) {
            // Keep one execution row per session and algorithm.
            return;
        }

        AlgorithmExecuteModel execution = new AlgorithmExecuteModel();
        execution.setSessionId(sessionId);
        execution.setAlgorithmId(algorithmId);
        execution.setExecutionTimeMs(BigDecimal.valueOf(timeMs));
        execution.setOutputResult(result);
        execution.setMaxFlowResult(maxFlow);

        algorithmExecuteRepository.save(execution);
    }

    private Integer getExistingAlgorithmId(String algorithmName) {
        AlgorithmModel algorithm = algorithmRepository.findByAlgorithmName(algorithmName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Algorithm not found in database: " + algorithmName
                ));
        return algorithm.getAlgorithmId();
    }

    private GameSessionModel validateSession(Integer sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId must be a positive integer");
        }

        return gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Game session not found for sessionId " + sessionId
                                + ". Create a Game_Session first, then generate traffic."
                ));
    }

    private String getPlayerName(Integer playerId) {
        PlayerModel player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Player not found for playerId " + playerId
                ));
        return player.getPlayerName();
    }

    private double nanosToMs(long nanos) {
        return Math.round((nanos / 1_000_000.0) * 1000.0) / 1000.0;
    }

    public static class AlgorithmRunResult {
        // Small response DTO returned by the algorithm endpoints.
        private final String algorithm;
        private final int maxFlow;
        private final double timeMs;

        public AlgorithmRunResult(String algorithm, int maxFlow, double timeMs) {
            this.algorithm = algorithm;
            this.maxFlow = maxFlow;
            this.timeMs = timeMs;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public int getMaxFlow() {
            return maxFlow;
        }

        public double getTimeMs() {
            return timeMs;
        }
    }

    public static class AnswerSubmissionResult {
        // Small response DTO returned after a player submits an answer.
        private final boolean correct;
        private final Integer correctAnswer;
        private final String playerName;

        public AnswerSubmissionResult(boolean correct, Integer correctAnswer, String playerName) {
            this.correct = correct;
            this.correctAnswer = correctAnswer;
            this.playerName = playerName;
        }

        public boolean isCorrect() {
            return correct;
        }

        public Integer getCorrectAnswer() {
            return correctAnswer;
        }

        public String getPlayerName() {
            return playerName;
        }
    }
}
