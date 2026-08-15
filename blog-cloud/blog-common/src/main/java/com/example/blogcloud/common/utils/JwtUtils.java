package com.example.blogcloud.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类(与单体工程一致): 生成 / 解析 token, 提取 userId / userName
 */
@Slf4j
public class JwtUtils {
    // 默认密钥: 仅本地开发兜底; 生产环境必须通过 blog.jwt.secret 配置(支持环境变量 BLOG_JWT_SECRET 覆盖)
    private static final String DEFAULT_SECRET = "dVnsmy+SIX6pNptQdeclDSJ26EMSPEIhvZYKBTTug4k=";
    private static final long DEFAULT_EXPIRATION = 24 * 60 * 60 * 1000L;

    private static volatile String secret = DEFAULT_SECRET;
    private static volatile long expiration = DEFAULT_EXPIRATION;
    private static volatile SecretKey secretKey =
            Keys.hmacShaKeyFor(Decoders.BASE64.decode(DEFAULT_SECRET));

    /**
     * 初始化 JWT 密钥与过期时间(由各服务的 JwtConfig 在启动时调用)。
     * 网关与服务端必须使用同一个密钥, 才能互相解析 token。
     */
    public static void init(String secret, long expiration) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("JWT 密钥不能为空");
        }
        JwtUtils.secret = secret;
        JwtUtils.expiration = expiration;
        JwtUtils.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        log.info("JWT 密钥已初始化, 过期时间: {} ms", expiration);
    }

    /** 生成 JWT, claim 例如 {id:1, name:"admin"} */
    public static String genJwt(Map<String, Object> claim) {
        return Jwts.builder()
                .setClaims(claim)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();
    }

    /** 解析 JWT, 失败返回 null */
    public static Claims parseJwt(String jwt) {
        if (jwt == null || jwt.isEmpty()) {
            return null;
        }
        try {
            return Jwts.parserBuilder().setSigningKey(secretKey)
                    .build().parseClaimsJws(jwt).getBody();
        } catch (Exception e) {
            log.error("解析JWT失败", e);
            return null;
        }
    }

    public static String getUserNameFromToken(String jwtToken) {
        Claims claims = JwtUtils.parseJwt(jwtToken);
        if (claims != null) {
            Object name = claims.get("name");
            if (name instanceof String) {
                return (String) name;
            }
        }
        return null;
    }

    public static Integer getUserIdFromToken(String jwtToken) {
        Claims claims = JwtUtils.parseJwt(jwtToken);
        if (claims != null) {
            Object id = claims.get("id");
            if (id instanceof Integer integerId) {
                return integerId;
            }
            if (id instanceof Number numberId) {
                return numberId.intValue();
            }
            if (id instanceof String stringId) {
                try {
                    return Integer.valueOf(stringId);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
