package com.xoassets.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 工具：负责生成和解析登录 Token。
 */
@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    /**
     * JWT签名密钥。
     */
    private final SecretKey secretKey;
    /**
     * 访问令牌有效期。
     */
    private final Duration accessExpireDuration;
    /**
     * 刷新令牌有效期。
     */
    private final Duration refreshExpireDuration;

    /**
     * 初始化行情提供方。
     */
    public JwtTokenProvider(
            @Value("${xoassets.jwt.secret}") String secret,
            @Value("${xoassets.jwt.access-expire-minutes}") long accessExpireMinutes,
            @Value("${xoassets.jwt.refresh-expire-minutes}") long refreshExpireMinutes) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpireDuration = Duration.ofMinutes(accessExpireMinutes);
        this.refreshExpireDuration = Duration.ofMinutes(refreshExpireMinutes);
    }

    /**
     * 按用户 ID 和用户名生成访问 Token。
     */
    public String createAccessToken(Long userId, String username) {
        return createToken(userId, username, accessExpireDuration, TOKEN_TYPE_ACCESS);
    }

    /**
     * 按用户 ID 和用户名生成刷新 Token。
     */
    public String createRefreshToken(Long userId, String username) {
        return createToken(userId, username, refreshExpireDuration, TOKEN_TYPE_REFRESH);
    }

    /**
     * 校验访问 Token 并解析登录用户。
     */
    public LoginUser parseAccessToken(String token) {
        return parseToken(token, TOKEN_TYPE_ACCESS);
    }

    /**
     * 校验刷新 Token 并解析登录用户。
     */
    public LoginUser parseRefreshToken(String token) {
        return parseToken(token, TOKEN_TYPE_REFRESH);
    }

    private String createToken(Long userId, String username, Duration expireDuration, String tokenType) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("tokenType", tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expireDuration)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 校验并解析 Token，返回业务层使用的登录用户信息。
     */
    private LoginUser parseToken(String token, String expectedTokenType) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String tokenType = claims.get("tokenType", String.class);
        if (!expectedTokenType.equals(tokenType)) {
            throw new IllegalArgumentException("Token 类型不匹配");
        }
        return new LoginUser(Long.valueOf(claims.getSubject()), claims.get("username", String.class));
    }
}
