package com.pdsa.games.queenspuzzle;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * REST controller for the Sixteen Queens puzzle endpoints.
 *
 * Exposes round start, submission, status, progress, and cancel operations
 * for authenticated players.
 */
@RestController
@RequestMapping("/sixteen-queens")
@CrossOrigin
@Tag(name = "Sixteen Queens", description = "Generate solutions and submit answers for the 16x16 queens puzzle")
public class QueensController {

    private final QueensService service;

    public QueensController(QueensService service) {
	this.service = service;
    }

    @PostMapping("/start")
    @Operation(
	    summary = "Start a Sixteen Queens round",
	    description = "Generates all valid solutions using sequential and threaded backtracking and stores them. Player is derived from the authenticated user."
    )
    @ApiResponses({
	    @ApiResponse(
		    responseCode = "200",
		    description = "Round started successfully",
		    content = @Content(mediaType = "application/json", schema = @Schema(implementation = QueensModel.StartResponse.class))
	    ),
	    @ApiResponse(responseCode = "400", description = "Invalid start request"),
	    @ApiResponse(responseCode = "401", description = "User is not authenticated"),
	    @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    /**
     * Begins a new Sixteen Queens round by generating solutions and storing
     * session metadata for the authenticated player.
     *
     * @param request start request payload
     * @param authentication authenticated user context
     * @return start response containing session details and timing information
     */
    public ResponseEntity<QueensModel.StartResponse> startRound(
	    @RequestBody QueensModel.StartRequest request,
	    Authentication authentication) {
	return ResponseEntity.ok(service.startRound(request, getAuthenticatedEmail(authentication)));
    }

    @PostMapping("/submit")
    @Operation(
	    summary = "Submit a Sixteen Queens answer",
	    description = "Validates a submitted board and records correct answers for the authenticated player."
    )
    @ApiResponses({
	    @ApiResponse(
		    responseCode = "200",
		    description = "Submission processed successfully",
		    content = @Content(mediaType = "application/json", schema = @Schema(implementation = QueensModel.SubmitResponse.class))
	    ),
	    @ApiResponse(responseCode = "400", description = "Invalid submission"),
	    @ApiResponse(responseCode = "401", description = "User is not authenticated"),
	    @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    /**
     * Validates a submitted queens placement and records discovered solutions.
     *
     * @param request submission payload containing session and queen positions
     * @param authentication authenticated user context
     * @return response indicating whether the submission is correct and discovery status
     */
    public ResponseEntity<QueensModel.SubmitResponse> submitAnswer(
	    @RequestBody QueensModel.SubmitRequest request,
	    Authentication authentication) {
	return ResponseEntity.ok(service.submitAnswer(request, getAuthenticatedEmail(authentication)));
    }

    @GetMapping("/{sessionId}/status")
    @Operation(
	    summary = "Get Sixteen Queens status",
	    description = "Returns the number of discovered solutions for the authenticated player's session."
    )
    @ApiResponses({
	    @ApiResponse(
		    responseCode = "200",
		    description = "Status retrieved successfully",
		    content = @Content(mediaType = "application/json", schema = @Schema(implementation = QueensModel.StatusResponse.class))
	    ),
	    @ApiResponse(responseCode = "400", description = "Invalid request"),
	    @ApiResponse(responseCode = "401", description = "User is not authenticated"),
	    @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    /**
     * Returns the current status of the player's Sixteen Queens session,
     * including total and discovered solution counts.
     *
     * @param sessionId game session identifier
     * @param authentication authenticated user context
     * @return status response for the session
     */
    public ResponseEntity<QueensModel.StatusResponse> getStatus(
	    @PathVariable Long sessionId,
	    Authentication authentication) {
	return ResponseEntity.ok(service.getStatus(sessionId, getAuthenticatedEmail(authentication)));
    }

    @GetMapping("/{sessionId}/progress")
    @Operation(
	    summary = "Poll an async Sixteen Queens job",
	    description = "For 8-queens no-cap runs only. Returns live sequential and threaded solution counts while the solver is executing."
    )
    @ApiResponses({
	    @ApiResponse(
		    responseCode = "200",
		    description = "Progress snapshot returned",
		    content = @Content(mediaType = "application/json", schema = @Schema(implementation = QueensModel.ProgressResponse.class))
	    ),
	    @ApiResponse(responseCode = "401", description = "User is not authenticated"),
	    @ApiResponse(responseCode = "404", description = "No async job tracked for this session")
    })
    /**
     * Polls live progress for an asynchronous full solve job.
     *
     * @param sessionId game session identifier
     * @param authentication authenticated user context
     * @return progress snapshot containing live counts and elapsed times
     */
    public ResponseEntity<QueensModel.ProgressResponse> getProgress(
	    @PathVariable Long sessionId,
	    Authentication authentication) {
	return ResponseEntity.ok(service.getProgress(sessionId, getAuthenticatedEmail(authentication)));
    }

    @PostMapping("/{sessionId}/cancel")
    @Operation(
	    summary = "Cancel an async Sixteen Queens job",
	    description = "Signals the async solvers to stop. Returns 200 if a job was cancelled or already done, 404 otherwise."
    )
    /**
     * Cancels an async solve job for the authenticated player's session.
     *
     * @param sessionId game session identifier
     * @param authentication authenticated user context
     * @return HTTP 200 when cancelled or already complete, 404 when no job exists
     */
    public ResponseEntity<Void> cancel(
	    @PathVariable Long sessionId,
	    Authentication authentication) {
	boolean cancelled = service.cancelJob(sessionId, getAuthenticatedEmail(authentication));
	return cancelled ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * Extracts the authenticated user's email from Spring Security context.
     *
     * @param authentication security authentication object
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
