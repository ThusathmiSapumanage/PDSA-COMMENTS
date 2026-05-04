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

    @PostMapping("/start")
    public ResponseEntity<KnightsTourModel.StartResponse> startRound(
            @RequestBody KnightsTourModel.StartRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.startRound(request, getAuthenticatedEmail(authentication)));
    }

    @PostMapping("/submit")
    public ResponseEntity<KnightsTourModel.AnswerResponse> submitAnswer(
            @RequestBody KnightsTourModel.AnswerRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.submitAnswer(request, getAuthenticatedEmail(authentication)));
    }

    @PostMapping("/hint")
    public ResponseEntity<KnightsTourModel.HintResponse> getHint(
            @RequestBody KnightsTourModel.HintRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.getHint(request, getAuthenticatedEmail(authentication)));
    }

    @GetMapping("/{sessionId}/correct-answer")
    public ResponseEntity<KnightsTourModel.CorrectAnswerResponse> getCorrectAnswer(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.getCorrectAnswer(sessionId, getAuthenticatedEmail(authentication)));
    }

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