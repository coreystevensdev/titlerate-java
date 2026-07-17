package dev.coreystevens.titlerate.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final String issuer;
    private final long expiryHours;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.issuer:titlerate-api}") String issuer,
        @Value("${jwt.expiration-hours:24}") long expiryHours
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.expiryHours = expiryHours;
    }

    public String generateToken(String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(email)
            .claim("role", role)
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expiryHours, ChronoUnit.HOURS)))
            .signWith(signingKey)
            .compact();
    }

    /** Returns null if the token is invalid or expired. */
    public String extractEmail(String token) {
        Claims claims = parseClaims(token);
        return claims == null ? null : claims.getSubject();
    }

    /** Returns null if the token is invalid, expired, or carries no role claim. */
    public String extractRole(String token) {
        Claims claims = parseClaims(token);
        return claims == null ? null : claims.get("role", String.class);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
