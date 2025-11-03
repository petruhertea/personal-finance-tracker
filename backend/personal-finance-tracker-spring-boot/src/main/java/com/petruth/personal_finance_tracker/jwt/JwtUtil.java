// JwtUtil.java - Updated with shorter access token validity
package com.petruth.personal_finance_tracker.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class JwtUtil {
    @Value("${jwt.secret.key}")
    private String secret;

    // Access token: 15 minutes (short-lived)
    private static final long ACCESS_TOKEN_VALIDITY = TimeUnit.MINUTES.toMillis(15);

    public String generateToken(String username, Long userId){
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusMillis(ACCESS_TOKEN_VALIDITY)))
                .signWith(generateKey())
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(generateKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractClaims(token).getSubject();
        return (username.equals(extractedUsername) && !isTokenExpired(token));
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    private SecretKey generateKey(){
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(decodedKey);
    }
}