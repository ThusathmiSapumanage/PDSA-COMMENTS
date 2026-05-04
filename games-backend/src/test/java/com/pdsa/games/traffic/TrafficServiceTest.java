package com.pdsa.games.traffic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import com.pdsa.games.traffic.service.TrafficService;

@ExtendWith(MockitoExtension.class)
class TrafficServiceTest {

    private static final Integer SESSION_ID = 10;
    private static final Integer PLAYER_ID = 7;
    private static final String PLAYER_NAME = "Alice";

    private static final String[][] EDGES = {
            {"A", "B"}, {"A", "C"}, {"A", "D"},
            {"B", "E"}, {"B", "F"},
            {"C", "E"}, {"C", "F"},
            {"D", "F"},
            {"E", "G"}, {"E", "H"},
            {"F", "H"},
            {"G", "T"}, {"H", "T"}
    };

    @Mock
    private RoadRepository roadRepository;

    @Mock
    private TrafficSimGameRepository trafficSimGameRepository;

    @Mock
    private AlgorithmRepository algorithmRepository;

    @Mock
    private AlgorithmExecuteRepository algorithmExecuteRepository;

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private ResponseRepository responseRepository;

    @InjectMocks
    private TrafficService trafficService;

    @Captor
    private ArgumentCaptor<List<Road>> roadsCaptor;

    @Captor
    private ArgumentCaptor<TrafficSimGame> trafficGameCaptor;

    @Captor
    private ArgumentCaptor<AlgorithmExecuteModel> algorithmExecutionCaptor;

    @Captor
    private ArgumentCaptor<ResponseModel> responseCaptor;

    @Test
    void generateTrafficNetwork_shouldGenerateSeededRoadsAndPersistMaxFlow() {
        stubValidSession();
        List<Road> expectedRoads = expectedSeededRoads(SESSION_ID);
        when(trafficSimGameRepository.findById(SESSION_ID)).thenReturn(Optional.empty());
        when(roadRepository.findByIdSessionId(SESSION_ID)).thenReturn(List.of());
        when(trafficSimGameRepository.save(any(TrafficSimGame.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roadRepository.saveAll(any())).thenReturn(expectedRoads);

        List<Road> result = trafficService.generateTrafficNetwork(SESSION_ID);

        assertEquals(EDGES.length, result.size());
        assertRoadListEquals(expectedRoads, result);

        verify(roadRepository).saveAll(roadsCaptor.capture());
        assertRoadListEquals(expectedRoads, roadsCaptor.getValue());

        verify(trafficSimGameRepository, org.mockito.Mockito.times(2)).save(trafficGameCaptor.capture());
        List<TrafficSimGame> savedGames = trafficGameCaptor.getAllValues();
        assertEquals(SESSION_ID, savedGames.get(0).getSessionId());
        assertEquals(SESSION_ID, savedGames.get(1).getSessionId());
        assertEquals(calculateExpectedMaxFlow(expectedRoads), savedGames.get(savedGames.size() - 1).getMaxFlowValue());
    }

    @Test
    void generateTrafficNetwork_shouldDeleteExistingRoadsBeforeSavingNewOnes() {
        stubValidSession();
        List<Road> existingRoads = List.of(
                road(SESSION_ID, "A", "B", 9),
                road(SESSION_ID, "B", "E", 6)
        );
        List<Road> expectedRoads = expectedSeededRoads(SESSION_ID);

        when(trafficSimGameRepository.findById(SESSION_ID)).thenReturn(Optional.of(new TrafficSimGame(SESSION_ID, 0)));
        when(roadRepository.findByIdSessionId(SESSION_ID)).thenReturn(existingRoads);
        when(roadRepository.saveAll(any())).thenReturn(expectedRoads);
        when(trafficSimGameRepository.save(any(TrafficSimGame.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Road> result = trafficService.generateTrafficNetwork(SESSION_ID);

        assertEquals(EDGES.length, result.size());
        verify(roadRepository).deleteAll(existingRoads);
    }

    @Test
    void calculateDinicMaxFlow_shouldReturnMaxFlowAndSaveExecution() {
        stubValidSession();
        List<Road> roads = sampleFlowNetwork();
        AlgorithmModel dinic = algorithm(101, "Traffic - Dinic");

        when(roadRepository.findByIdSessionId(SESSION_ID)).thenReturn(roads);
        when(algorithmRepository.findByAlgorithmName("Traffic - Dinic")).thenReturn(Optional.of(dinic));
        when(algorithmExecuteRepository.findById(new AlgorithmExecuteId(SESSION_ID, 101))).thenReturn(Optional.empty());

        TrafficService.AlgorithmRunResult result = trafficService.calculateDinicMaxFlow(SESSION_ID);

        assertEquals("Dinic", result.getAlgorithm());
        assertEquals(15, result.getMaxFlow());
        assertTrue(result.getTimeMs() >= 0.0);

        verify(algorithmExecuteRepository).save(algorithmExecutionCaptor.capture());
        AlgorithmExecuteModel execution = algorithmExecutionCaptor.getValue();
        assertEquals(SESSION_ID, execution.getSessionId());
        assertEquals(101, execution.getAlgorithmId());
        assertEquals(15, execution.getMaxFlowResult());
        assertEquals(AlgorithmExecuteOutputResult.SUCCESS, execution.getOutputResult());
        assertNotNull(execution.getExecutionTimeMs());
        assertTrue(execution.getExecutionTimeMs().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void calculateDinicMaxFlow_shouldNotSaveExecutionWhenRecordAlreadyExists() {
        stubValidSession();
        List<Road> roads = sampleFlowNetwork();
        AlgorithmModel dinic = algorithm(101, "Traffic - Dinic");

        when(roadRepository.findByIdSessionId(SESSION_ID)).thenReturn(roads);
        when(algorithmRepository.findByAlgorithmName("Traffic - Dinic")).thenReturn(Optional.of(dinic));
        when(algorithmExecuteRepository.findById(new AlgorithmExecuteId(SESSION_ID, 101)))
                .thenReturn(Optional.of(new AlgorithmExecuteModel()));

        TrafficService.AlgorithmRunResult result = trafficService.calculateDinicMaxFlow(SESSION_ID);

        assertEquals(15, result.getMaxFlow());
        verify(algorithmExecuteRepository, never()).save(any(AlgorithmExecuteModel.class));
    }

    @Test
    void calculateFordFulkersonMaxFlow_shouldReturnMaxFlowAndSaveExecution() {
        stubValidSession();
        List<Road> roads = sampleFlowNetwork();
        AlgorithmModel fordFulkerson = algorithm(202, "Traffic - Ford-Fulkerson");

        when(roadRepository.findByIdSessionId(SESSION_ID)).thenReturn(roads);
        when(algorithmRepository.findByAlgorithmName("Traffic - Ford-Fulkerson"))
                .thenReturn(Optional.of(fordFulkerson));
        when(algorithmExecuteRepository.findById(new AlgorithmExecuteId(SESSION_ID, 202))).thenReturn(Optional.empty());

        TrafficService.AlgorithmRunResult result = trafficService.calculateFordFulkersonMaxFlow(SESSION_ID);

        assertEquals("Ford-Fulkerson", result.getAlgorithm());
        assertEquals(15, result.getMaxFlow());
        assertTrue(result.getTimeMs() >= 0.0);

        verify(algorithmExecuteRepository).save(algorithmExecutionCaptor.capture());
        AlgorithmExecuteModel execution = algorithmExecutionCaptor.getValue();
        assertEquals(SESSION_ID, execution.getSessionId());
        assertEquals(202, execution.getAlgorithmId());
        assertEquals(15, execution.getMaxFlowResult());
        assertEquals(AlgorithmExecuteOutputResult.SUCCESS, execution.getOutputResult());
    }

    @Test
    void calculateDinicMaxFlow_shouldThrowWhenRoadNetworkMissing() {
        stubValidSession();
        when(roadRepository.findByIdSessionId(SESSION_ID)).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> trafficService.calculateDinicMaxFlow(SESSION_ID)
        );

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Traffic network not found for session " + SESSION_ID, ex.getReason());
    }

    @Test
    void calculateFordFulkersonMaxFlow_shouldThrowWhenSourceOrSinkMissing() {
        stubValidSession();
        when(roadRepository.findByIdSessionId(SESSION_ID)).thenReturn(List.of(
                road(SESSION_ID, "A", "B", 8),
                road(SESSION_ID, "B", "C", 5)
        ));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> trafficService.calculateFordFulkersonMaxFlow(SESSION_ID)
        );

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Traffic network is missing source or sink nodes", ex.getReason());
    }

    @Test
    void submitAnswer_shouldMarkFirstCorrectAnswerAsCorrectAndReturnPlayerName() {
        stubValidSession();
        PlayerModel player = player(PLAYER_ID, PLAYER_NAME);
        when(trafficSimGameRepository.findById(SESSION_ID)).thenReturn(Optional.of(new TrafficSimGame(SESSION_ID, 15)));
        when(responseRepository.findBySessionId(SESSION_ID)).thenReturn(List.of());
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));

        TrafficService.AnswerSubmissionResult result = trafficService.submitAnswer(SESSION_ID, 15);

        assertTrue(result.isCorrect());
        assertEquals(15, result.getCorrectAnswer());
        assertEquals(PLAYER_NAME, result.getPlayerName());

        verify(responseRepository).save(responseCaptor.capture());
        ResponseModel savedResponse = responseCaptor.getValue();
        assertEquals(SESSION_ID, savedResponse.getSessionId());
        assertEquals("15", savedResponse.getResponse());
        assertTrue(savedResponse.getIsCorrect());
    }

    @Test
    void submitAnswer_shouldStoreIncorrectWhenCorrectAnswerWasAlreadySubmitted() {
        stubValidSession();
        PlayerModel player = player(PLAYER_ID, PLAYER_NAME);
        ResponseModel existingCorrect = new ResponseModel();
        existingCorrect.setIsCorrect(true);

        when(trafficSimGameRepository.findById(SESSION_ID)).thenReturn(Optional.of(new TrafficSimGame(SESSION_ID, 15)));
        when(responseRepository.findBySessionId(SESSION_ID)).thenReturn(List.of(existingCorrect));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));

        TrafficService.AnswerSubmissionResult result = trafficService.submitAnswer(SESSION_ID, 15);

        assertFalse(result.isCorrect());
        assertEquals(15, result.getCorrectAnswer());
        assertEquals(PLAYER_NAME, result.getPlayerName());

        verify(responseRepository).save(responseCaptor.capture());
        assertFalse(responseCaptor.getValue().getIsCorrect());
    }

    @Test
    void submitAnswer_shouldStoreIncorrectForWrongAnswer() {
        stubValidSession();
        PlayerModel player = player(PLAYER_ID, PLAYER_NAME);
        when(trafficSimGameRepository.findById(SESSION_ID)).thenReturn(Optional.of(new TrafficSimGame(SESSION_ID, 15)));
        when(responseRepository.findBySessionId(SESSION_ID)).thenReturn(List.of());
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));

        TrafficService.AnswerSubmissionResult result = trafficService.submitAnswer(SESSION_ID, 11);

        assertFalse(result.isCorrect());
        assertEquals(15, result.getCorrectAnswer());
        assertEquals(PLAYER_NAME, result.getPlayerName());

        verify(responseRepository).save(responseCaptor.capture());
        ResponseModel savedResponse = responseCaptor.getValue();
        assertEquals("11", savedResponse.getResponse());
        assertFalse(savedResponse.getIsCorrect());
    }

    @Test
    void submitAnswer_shouldThrowWhenAnswerIsNull() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> trafficService.submitAnswer(SESSION_ID, null)
        );

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("answer must be a non-negative integer", ex.getReason());
    }

    @Test
    void submitAnswer_shouldThrowWhenAnswerIsNegative() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> trafficService.submitAnswer(SESSION_ID, -1)
        );

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("answer must be a non-negative integer", ex.getReason());
    }

    @Test
    void getSavedMaxFlow_shouldReturnStoredValue() {
        stubValidSession();
        when(trafficSimGameRepository.findById(SESSION_ID)).thenReturn(Optional.of(new TrafficSimGame(SESSION_ID, 21)));

        Integer result = trafficService.getSavedMaxFlow(SESSION_ID);

        assertEquals(21, result);
    }

    @Test
    void getSavedMaxFlow_shouldThrowWhenTrafficGameMissing() {
        stubValidSession();
        when(trafficSimGameRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> trafficService.getSavedMaxFlow(SESSION_ID)
        );

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Traffic game result not found", ex.getReason());
    }

    @Test
    void generateTrafficNetwork_shouldThrowWhenSessionIdInvalid() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> trafficService.generateTrafficNetwork(0)
        );

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("sessionId must be a positive integer", ex.getReason());
    }

    @Test
    void generateTrafficNetwork_shouldThrowWhenSessionDoesNotExist() {
        when(gameSessionRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> trafficService.generateTrafficNetwork(99)
        );

        assertEquals(404, ex.getStatusCode().value());
        assertEquals(
                "Game session not found for sessionId 99. Create a Game_Session first, then generate traffic.",
                ex.getReason()
        );
    }

    private GameSessionModel validSession() {
        return new GameSessionModel(SESSION_ID, 3, PLAYER_ID, null);
    }

    private void stubValidSession() {
        when(gameSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(validSession()));
    }

    private PlayerModel player(Integer playerId, String playerName) {
        PlayerModel player = new PlayerModel();
        player.setPlayerId(playerId);
        player.setPlayerName(playerName);
        player.setPlayerEmail(playerName.toLowerCase() + "@example.com");
        player.setPlayerPassword("secret");
        return player;
    }

    private AlgorithmModel algorithm(Integer algorithmId, String algorithmName) {
        AlgorithmModel model = new AlgorithmModel();
        model.setAlgorithmId(algorithmId);
        model.setAlgorithmName(algorithmName);
        model.setGameId(3);
        return model;
    }

    private Road road(Integer sessionId, String from, String to, int capacity) {
        return new Road(new RoadId(sessionId, from, to), capacity);
    }

    private List<Road> expectedSeededRoads(Integer sessionId) {
        Random random = new Random(sessionId.longValue());
        List<Road> roads = new ArrayList<>();
        for (String[] edge : EDGES) {
            roads.add(road(sessionId, edge[0], edge[1], random.nextInt(11) + 5));
        }
        return roads;
    }

    private List<Road> sampleFlowNetwork() {
        return List.of(
                road(SESSION_ID, "A", "B", 10),
                road(SESSION_ID, "A", "C", 5),
                road(SESSION_ID, "B", "C", 4),
                road(SESSION_ID, "B", "T", 8),
                road(SESSION_ID, "C", "T", 7)
        );
    }

    private void assertRoadListEquals(List<Road> expected, List<Road> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Road expectedRoad = expected.get(i);
            Road actualRoad = actual.get(i);
            assertEquals(expectedRoad.getId(), actualRoad.getId());
            assertEquals(expectedRoad.getCapacity(), actualRoad.getCapacity());
        }
    }

    private int calculateExpectedMaxFlow(List<Road> roads) {
        Map<String, Map<String, Integer>> residual = new HashMap<>();

        for (Road road : roads) {
            String from = road.getId().getStartNode();
            String to = road.getId().getEndNode();

            residual.computeIfAbsent(from, key -> new HashMap<>()).put(to, road.getCapacity());
            residual.computeIfAbsent(to, key -> new HashMap<>()).putIfAbsent(from, 0);
        }

        int maxFlow = 0;
        while (true) {
            Map<String, String> parent = new HashMap<>();
            Queue<String> queue = new ArrayDeque<>();
            queue.add("A");
            parent.put("A", null);

            while (!queue.isEmpty() && !parent.containsKey("T")) {
                String current = queue.poll();
                for (Map.Entry<String, Integer> edge : residual.getOrDefault(current, Map.of()).entrySet()) {
                    if (edge.getValue() > 0 && !parent.containsKey(edge.getKey())) {
                        parent.put(edge.getKey(), current);
                        queue.add(edge.getKey());
                    }
                }
            }

            if (!parent.containsKey("T")) {
                return maxFlow;
            }

            int pathFlow = Integer.MAX_VALUE;
            for (String node = "T"; parent.get(node) != null; node = parent.get(node)) {
                String prev = parent.get(node);
                pathFlow = Math.min(pathFlow, residual.get(prev).get(node));
            }

            for (String node = "T"; parent.get(node) != null; node = parent.get(node)) {
                String prev = parent.get(node);
                residual.get(prev).put(node, residual.get(prev).get(node) - pathFlow);
                residual.get(node).put(prev, residual.get(node).getOrDefault(prev, 0) + pathFlow);
            }

            maxFlow += pathFlow;
        }
    }
}
