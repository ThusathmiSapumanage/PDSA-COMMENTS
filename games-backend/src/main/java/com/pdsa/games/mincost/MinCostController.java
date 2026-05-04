package com.pdsa.games.mincost;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * MIN COST CONTROLLER:
 * This is the API Gateway for the MinCost game.
 * It's now very clean because error handling is moved to MinCostExceptionHandler,
 * and data logic is in the Service/Repository.
 */
@RestController
@RequestMapping("/api/mincost")
@CrossOrigin(origins = "*")
public class MinCostController {
    
    private final MinCostService minCostService;

    public MinCostController(MinCostService minCostService) {
        this.minCostService = minCostService;
    }

    /**
     * GET HISTORY: Returns charts data for the last 10 games.
     */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        return ResponseEntity.ok(minCostService.getHistory());
    }

    /**
     * POST START: Generates a matrix and solves it.
     * Error handling is delegated to MinCostExceptionHandler.
     */
    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody(required = false) MinCostModel.StartRequest request) {
        // Use default empty request if none provided
        MinCostModel.StartRequest payload = (request == null) 
                ? new MinCostModel.StartRequest() 
                : request;
        
        return ResponseEntity.ok(minCostService.startGame(payload));
    }
}
