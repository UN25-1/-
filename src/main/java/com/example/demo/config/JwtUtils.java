package com.example.demo.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类 —— Token生成、校验、解析
 */
@Component
public class JwtUtils {

    private final SecretKey secretKey;

    /** Token有效期：24小时（毫秒） */
    private static final long ACCESS_TOKEN_EXPIRE_MS = 24 * 60 * 60 * 1000;

    /** Refresh Token有效期：7天 */
    private static final long REFRESH_TOKEN_EXPIRE_MS = 7 * 24 * 60 * 60 * 1000;

    public JwtUtils(@Value("${jwt.secret:ThisIsASecretKeyForJWTTokenGenerationMustBe256BitsLong!!}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(Integer userId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_MS))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(Integer userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRE_MS))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析JWT中的所有Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验Token是否有效（不抛出异常即有效）
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从Token中提取用户ID
     */
    public Integer getUserIdFromToken(String token) {
        return Integer.parseInt(parseToken(token).getSubject());
    }

    /**
     * 从Token中提取用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).get("username", String.class);
    }

    /**
     * 从Token中提取角色
     */
    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }
}
