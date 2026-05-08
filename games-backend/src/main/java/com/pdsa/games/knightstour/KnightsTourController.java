package com.pdsa.games.knightstour;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/knights-tour")
@CrossOrigin
@Tag(name = "Knights Tour", description = "Start a round, submit answers, request hints, and fetch the correct answer")
public class KnightsTourController {

    private final KnightsTourService service;

    public KnightsTourController(KnightsTourService service) {
        this.service = service;
    }

    /**
     * Start a new Knights Tour round for the authenticated player.
     *
     * @param request round start request payload
     * @param authentication authenticated user context
     * @return response containing the generated round details
     */
    @PostMapping("/start")
    public ResponseEntity<KnightsTourModel.StartResponse> startRound(
            @RequestBody KnightsTourModel.StartRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.startRound(request, getAuthenticatedEmail(authentication)));
    }

    /**
     * Submit a player's Knight move sequence for validation.
     *
     * @param request answer submission payload
     * @param authentication authenticated user context
     * @return response describing correctness and validation details
     */
    @PostMapping("/submit")
    public ResponseEntity<KnightsTourModel.AnswerResponse> submitAnswer(
            @RequestBody KnightsTourModel.AnswerRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.submitAnswer(request, getAuthenticatedEmail(authentication)));
    }

    /**
     * Request a hint for the next legal knight move in the current session.
     *
     * @param request hint request payload
     * @param authentication authenticated user context
     * @return hint response containing suggested next moves
     */
    @PostMapping("/hint")
    public ResponseEntity<KnightsTourModel.HintResponse> getHint(
            @RequestBody KnightsTourModel.HintRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.getHint(request, getAuthenticatedEmail(authentication)));
    }

    /**
     * Retrieve the correct saved Knight's Tour answer for a session.
     *
     * @param sessionId session identifier
     * @param authentication authenticated user context
     * @return correct answer response with saved moves
     */
    @GetMapping("/{sessionId}/correct-answer")
    public ResponseEntity<KnightsTourModel.CorrectAnswerResponse> getCorrectAnswer(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.getCorrectAnswer(sessionId, getAuthenticatedEmail(authentication)));
    }

    /**
     * Extract the authenticated user's email from the Spring Security authentication object.
     *
     * @param authentication authentication context
     * @return authenticated email address
     */
    private String getAuthenticatedEmail(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "User is not authenticated.");
        }

        return authentication.getName();
    }
}