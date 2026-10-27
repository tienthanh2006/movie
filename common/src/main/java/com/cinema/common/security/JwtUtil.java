package com.cinema.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key secretKey;
    private final long expirationMillis;

    // Mọi service dùng CHUNG một jwt.secret (ít nhất 32 ký tự)
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-minutes:15}") long expirationMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32
                || secret.startsWith("doi-chuoi-bi-mat")) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes and must not be the sample value");
        }
        if (expirationMinutes < 1 || expirationMinutes > 24 * 60) {
            throw new IllegalStateException("jwt.expiration-minutes must be between 1 and 1440");
        }
        this.expirationMillis = expirationMinutes * 60_000L;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(secretKey)
                .compact();
    }

    private Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build()
                .parseClaimsJws(token).getBody();
    }

    public String extractUsername(String token) { return parse(token).getSubject(); }

    public String extractRole(String token) { return parse(token).get("role", String.class); }

    public Long extractUserId(String token) {
        Object value = parse(token).get("userId");
        return value == null ? null : ((Number) value).longValue();
    }

    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
