package com.example.agent_rnd.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.util.Date;

public class JwtProvider {

    private static final String SECRET_KEY =
            "agent-rnd-secret-key-agent-rnd-secret-key"; // 32byte 이상

    private static final long EXPIRATION_TIME = 1000 * 60 * 60; // 1시간

    // JWT 생성
    public static String createToken(Long userId, String email, String role) {

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis() + EXPIRATION_TIME)
                )
                .signWith(
                        Keys.hmacShaKeyFor(SECRET_KEY.getBytes()),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }

    // JWT 검증 + userId 추출
    public static Long validateAndGetUserId(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(
                            Keys.hmacShaKeyFor(SECRET_KEY.getBytes())
                    )
                    .build()
                    .parseClaimsJws(token);

            return Long.parseLong(claims.getBody().getSubject());

        } catch (JwtException | IllegalArgumentException e) {
            return null; // 유효하지 않은 토큰
        }
    }
}

