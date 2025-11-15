package com.bookverse.bookverse_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Getter
    private SecretKey signKey;

    @PostConstruct
    public void init() {
        signKey = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        JwtParser parser = Jwts.parser().verifyWith(getSignKey()).build();
        Jws<Claims> jws = parser.parseSignedClaims(token);
        return jws.getPayload();
    }

    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Boolean validateToken(String token) {
        if (!isTokenExpired(token)){
            try {
                Jwts.parser().verifyWith(getSignKey()).build().parseSignedClaims(token);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public List<String> extractRoles(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get("roles", List.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}