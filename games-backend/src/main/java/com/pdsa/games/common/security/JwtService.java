package com.pdsa.games.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT utility service for token creation, parsing, and validation.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    /**
     * Construct the JWT service with configuration values from the environment.
     *
     * @param secret signing secret used for token HMAC validation
     * @param expirationMsValue token expiration duration in milliseconds
     */
    public JwtService(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration-ms:}") String expirationMsValue) {
        validateSecret(secret);
        this.expirationMs = validateExpiration(expirationMsValue);
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a new JWT for the given subject.
     *
     * @param subject token subject (player email)
     * @return signed JWT string
     */
    public String generateToken(String subject) {
        Instant now = Instant.now();

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extract the subject (player email) from the JWT.
     *
     * @param token JWT string
     * @return subject embedded in the token
     */
    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Verify that the token belongs to the expected subject and has not expired.
     *
     * @param token JWT string
     * @param expectedSubject expected subject value
     * @return true when the token is valid and subject matches
     */
    public boolean isTokenValid(String token, String expectedSubject) {
        Claims claims = extractAllClaims(token);
        return expectedSubject.equals(claims.getSubject())
                && claims.getExpiration().after(new Date());
    }

    /**
     * Get the configured token expiration value in seconds.
     *
     * @return token expiration duration in seconds
     */
    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    /**
     * Parse the JWT and return all claims.
     *
     * @param token JWT string
     * @return claims extracted from the token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validate that the configured JWT secret is present and has sufficient entropy.
     *
     * @param secret configured secret value
     */
    private void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is missing. Set jwt.secret in games-backend/.env before startup.");
        }

        int secretLength = secret.getBytes(StandardCharsets.UTF_8).length;
        if (secretLength < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long.");
        }
    }

    /**
     * Validate the configured JWT expiration value and parse it to milliseconds.
     *
     * @param expirationMsValue expiration value from configuration
     * @return expiration in milliseconds
     */
    private long validateExpiration(String expirationMsValue) {
        if (expirationMsValue == null || expirationMsValue.isBlank()) {
            throw new IllegalStateException("JWT expiration is missing. Set jwt.expiration-ms in games-backend/.env before startup.");
        }

        try {
            long parsedExpiration = Long.parseLong(expirationMsValue.trim());
            if (parsedExpiration <= 0) {
                throw new IllegalStateException("JWT expiration must be greater than 0.");
            }
            return parsedExpiration;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("JWT expiration must be a valid number.", exception);
        }
    }
}