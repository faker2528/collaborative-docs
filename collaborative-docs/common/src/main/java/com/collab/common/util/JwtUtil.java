package com.collab.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
public class JwtUtil {

    /**
     * 默认密钥（生产环境应从配置中心获取）
     */
    private static final String DEFAULT_SECRET = "collaborative-docs-jwt-secret-key-must-be-at-least-256-bits";
    
    /**
     * Token有效期（默认24小时）
     */
    private static final long DEFAULT_EXPIRATION = 24 * 60 * 60 * 1000L;

    /**
     * 生成Token
     */
    public static String generateToken(Long userId, String username) {
        return generateToken(userId, username, DEFAULT_SECRET, DEFAULT_EXPIRATION);
    }

    /**
     * 生成Token
     */
    public static String generateToken(Long userId, String username, String secret, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 解析Token
     */
    public static Claims parseToken(String token) {
        return parseToken(token, DEFAULT_SECRET);
    }

    /**
     * 解析Token
     */
    public static Claims parseToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证Token是否有效
     */
    public static boolean validateToken(String token) {
        return validateToken(token, DEFAULT_SECRET);
    }

    /**
     * 验证Token是否有效
     */
    public static boolean validateToken(String token, String secret) {
        try {
            parseToken(token, secret);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Token无效: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 从Token中获取用户ID
     */
    public static Long getUserId(String token) {
        return getUserId(token, DEFAULT_SECRET);
    }

    /**
     * 从Token中获取用户ID
     */
    public static Long getUserId(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中获取用户名
     */
    public static String getUsername(String token) {
        return getUsername(token, DEFAULT_SECRET);
    }

    /**
     * 从Token中获取用户名
     */
    public static String getUsername(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return claims.get("username", String.class);
    }

    /**
     * 判断Token是否过期
     */
    public static boolean isTokenExpired(String token) {
        return isTokenExpired(token, DEFAULT_SECRET);
    }

    /**
     * 判断Token是否过期
     */
    public static boolean isTokenExpired(String token, String secret) {
        try {
            Claims claims = parseToken(token, secret);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * 获取Token过期时间
     */
    public static Date getExpiration(String token) {
        return getExpiration(token, DEFAULT_SECRET);
    }

    /**
     * 获取Token过期时间
     */
    public static Date getExpiration(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return claims.getExpiration();
    }
}
