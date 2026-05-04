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

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration-ms:}") String expirationMsValue) {
        validateSecret(secret);
        this.expirationMs = validateExpiration(expirationMsValue);
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String subject) {
        Instant now = Instant.now();

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String expectedSubject) {
        Claims claims = extractAllClaims(token);
        return expectedSubject.equals(claims.getSubject())
                && claims.getExpiration().after(new Date());
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is missing. Set jwt.secret in games-backend/.env before startup.");
        }

        int secretLength = secret.getBytes(StandardCharsets.UTF_8).length;
        if (secretLength < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long.");
        }
    }

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