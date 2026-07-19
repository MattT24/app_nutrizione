package it.nutrizionista.restnutrizionista.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/** Utilità per generare/validare token JWT HS256. */
@Component
public class JwtUtils {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expirationMs}")
    private long jwtExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateJwtToken(String subject, Map<String, Object> claims) {
        return generateJwtToken(subject, claims,
                Instant.ofEpochMilli(System.currentTimeMillis() + jwtExpirationMs));
    }

    /** Token con scadenza assoluta, usato per demo e impersonazioni brevi. */
    public String generateJwtToken(String subject, Map<String, Object> claims, Instant expiresAt) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateJwtToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public SecretKey getKey() {
        return key;
    }
}
