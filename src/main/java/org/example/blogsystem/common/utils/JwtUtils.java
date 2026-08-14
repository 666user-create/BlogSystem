package org.example.blogsystem.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import java.util.Date;
import javax.crypto.SecretKey;
import java.util.Map;

/**
 * JWT 工具类
 * <p>
 * 用于生成和解析 JWT，包含：
 * 1. 生成携带用户信息的 token
 * 2. 解析 token 得到 Claims
 * 3. 从 token 中提取 userId
 */
@Slf4j
public class JwtUtils {
    // 默认密钥：仅本地开发兜底；生产环境必须通过 blog.jwt.secret 配置（支持环境变量 BLOG_JWT_SECRET 覆盖）
    private static final String DEFAULT_SECRET = "dVnsmy+SIX6pNptQdeclDSJ26EMSPEIhvZYKBTTug4k=";
    // 默认过期时间(单位: 毫秒)，一天
    private static final long DEFAULT_EXPIRATION = 24 * 60 * 60 * 1000L;

    private static volatile String secret = DEFAULT_SECRET;
    private static volatile long expiration = DEFAULT_EXPIRATION;
    private static volatile SecretKey secretKey =
            Keys.hmacShaKeyFor(Decoders.BASE64.decode(DEFAULT_SECRET));

    /**
     * 初始化 JWT 密钥与过期时间（由 JwtConfig 在应用启动时注入）。
     * 密钥必须是 Base64 编码的 HMAC-SHA 密钥。
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
    /**
     * 生成 JWT
     * @param claim 自定义载荷（例如用户 id、用户名等）
     * @return 生成的 JWT 字符串
     */
    public static String genJwt(Map<String, Object> claim){
        //签名算法
        String jwt = Jwts.builder()
                .setClaims(claim) //自定义内容(载荷)
                .setIssuedAt(new Date())// 设置签发时间
                .setExpiration(new Date(System.currentTimeMillis() +
                        expiration)) //设置过期时间
                .signWith(secretKey) //签名算法
                .compact();
        return jwt;
    }
    /**
     * 解析 JWT
     * @param jwt 前端传入的 token 字符串
     * @return 解析成功返回 Claims，失败返回 null
     */
    public static Claims parseJwt(String jwt){
        if(jwt==null||jwt.isEmpty()){
            return null;
        }
        JwtParserBuilder jwtParserBuilder =
                Jwts.parserBuilder().setSigningKey(secretKey);
        Claims claims = null;
        try {
            // 使用密钥解析 token，如果过期或被篡改会抛异常
            claims=jwtParserBuilder.build().parseClaimsJws(jwt).getBody();
        }catch (Exception e){
            log.error("解析JWT失败",e);
        }
        return claims;
    }

    /**
     * 从 token 中获取用户名
     */
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

    /**
     * 从 token 中获取用户 id
     */
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
