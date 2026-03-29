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
 * JWT 工具类。
 * <p>
 * 负责生成和解析用户端、管理端 Token，并提供过期时间换算能力。
 */
@Component
public class JwtUtil {

    // 配置项
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.admin-expiration}")
    private long adminExpiration;

    // 运行时密钥
    private SecretKey key;

    // Token 声明字段
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ADMIN_ID = "adminId";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_USER = "user";
    private static final String TYPE_ADMIN = "admin";

    /**
     * 初始化签名密钥。
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成用户端 Token。
     *
     * @param userId 用户 ID
     * @return 用户端 Token
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
     * 生成管理端 Token。
     *
     * @param adminId 管理员 ID
     * @return 管理端 Token
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
     * 从 Token 中解析用户 ID。
     *
     * @param token JWT Token
     * @return 用户 ID；若 Token 非用户端 Token 或解析失败则返回 {@code null}
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null || !TYPE_USER.equals(claims.get(CLAIM_TYPE))) {
            return null;
        }
        return claims.get(CLAIM_USER_ID, Long.class);
    }

    /**
     * 从 Token 中解析管理员 ID。
     *
     * @param token JWT Token
     * @return 管理员 ID；若 Token 非管理端 Token 或解析失败则返回 {@code null}
     */
    public Long getAdminIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null || !TYPE_ADMIN.equals(claims.get(CLAIM_TYPE))) {
            return null;
        }
        return claims.get(CLAIM_ADMIN_ID, Long.class);
    }

    /**
     * 解析并校验 Token，有异常时返回 null。
     *
     * @param token JWT Token
     * @return Token 载荷；解析失败时返回 {@code null}
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
     * 获取用户端 Token 过期时间（秒）。
     *
     * @return 用户端 Token 过期时间（秒）
     */
    public long getExpirationSeconds() {
        return expiration / 1000;
    }

    /**
     * 获取管理端 Token 过期时间（秒）。
     *
     * @return 管理端 Token 过期时间（秒）
     */
    public long getAdminExpirationSeconds() {
        return adminExpiration / 1000;
    }
}
