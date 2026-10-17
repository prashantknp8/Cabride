package com.cabride.ride_service.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey secretKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token){
        String userId=extractAllClaims(token)
                .get("userId",String.class);

        return UUID.fromString(userId);

    }


    public String extractRole(String token){
        return extractAllClaims(token)
                .get("role", String.class);
    }

    public boolean isTokenValid(String token){
        try{
            extractAllClaims(token);
            return true;
        }
        catch(Exception ex){
            return false;
        }
    }


}
