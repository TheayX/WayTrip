package com.travel.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类 (适配 jjwt 0.12.x)
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.admin-expiration}")
    private long adminExpiration;

    private SecretKey key;

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ADMIN_ID = "adminId";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_USER = "user";
    private static final String TYPE_ADMIN = "admin";

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成用户 Token
     */
    public String generateUserToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TYPE, TYPE_USER)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 生成管理员 Token
     */
    public String generateAdminToken(Long adminId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + adminExpiration);

        return Jwts.builder()
                .claim(CLAIM_ADMIN_ID, adminId)
                .claim(CLAIM_TYPE, TYPE_ADMIN)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 从 Token 获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null || !TYPE_USER.equals(claims.get(CLAIM_TYPE))) {
            return null;
        }
        return claims.get(CLAIM_USER_ID, Long.class);
    }

    /**
     * 从 Token 获取管理员 ID
     */
    public Long getAdminIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null || !TYPE_ADMIN.equals(claims.get(CLAIM_TYPE))) {
            return null;
        }
        return claims.get(CLAIM_ADMIN_ID, Long.class);
    }

    /**
     * 解析 Token
     */
    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 Token 过期时间（秒）
     */
    public long getExpirationSeconds() {
        return expiration / 1000;
    }

    /**
     * 获取管理员 Token 过期时间（秒）
     */
    public long getAdminExpirationSeconds() {
        return adminExpiration / 1000;
    }
}
