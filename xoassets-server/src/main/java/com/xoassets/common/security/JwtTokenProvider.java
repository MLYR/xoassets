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

    private final SecretKey secretKey;
    private final Duration expireDuration;

    public JwtTokenProvider(
            @Value("${xoassets.jwt.secret}") String secret,
            @Value("${xoassets.jwt.expire-minutes}") long expireMinutes) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireDuration = Duration.ofMinutes(expireMinutes);
    }

    /**
     * 按用户 ID 和用户名生成登录 Token。
     */
    public String createToken(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expireDuration)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 校验并解析 Token，返回业务层使用的登录用户信息。
     */
    public LoginUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new LoginUser(Long.valueOf(claims.getSubject()), claims.get("username", String.class));
    }
}
