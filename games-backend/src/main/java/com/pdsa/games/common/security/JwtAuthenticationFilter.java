package com.pdsa.games.common.security;

import java.io.IOException;
import java.util.Collections;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.jsonwebtoken.JwtException;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that validates incoming JWT bearer tokens and populates the security context.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Validate the JWT bearer token for each request, skip public auth endpoints, and set the Spring security context.
     *
     * @param request incoming servlet request
     * @param response outgoing servlet response
     * @param filterChain filter chain to continue processing
     * @throws ServletException when servlet processing fails
     * @throws IOException when I/O fails
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // Skip JWT validation only for the public auth endpoints.
        if (isPublicPlayerEndpoint(request, path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        String playerEmail;

        try {
            playerEmail = jwtService.extractSubject(token);
        } catch (JwtException | IllegalArgumentException e) {
            filterChain.doFilter(request, response);
            return;
        }

        if (playerEmail != null &&
                SecurityContextHolder.getContext().getAuthentication() == null &&
                jwtService.isTokenValid(token, playerEmail)) {

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            playerEmail,
                            null,
                            Collections.emptyList());

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determine whether the request should skip JWT validation because it targets a public auth endpoint.
     *
     * @param request incoming servlet request
     * @param path request path
     * @return true if the endpoint is publicly accessible without a token
     */
    private boolean isPublicPlayerEndpoint(HttpServletRequest request, String path) {
        String method = request.getMethod();

        return HttpMethod.POST.matches(method)
                && (path.equals("/api/players") || path.equals("/api/players/login"));
    }
}
