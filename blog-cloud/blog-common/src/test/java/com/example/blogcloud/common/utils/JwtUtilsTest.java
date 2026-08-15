package com.example.blogcloud.common.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * JwtUtils 单元测试（与单体工程同一套套路）
 * ============================================================
 * 覆盖：签发/解析往返、篡改 token、过期 token、空值、提取 userId/userName。
 * 纯工具类测试，不依赖任何中间件。
 * ============================================================
 */
class JwtUtilsTest {

    private static final String SECRET = "dVnsmy+SIX6pNptQdeclDSJ26EMSPEIhvZYKBTTug4k=";
    private static final long EXPIRATION = 24 * 60 * 60 * 1000L;

    /** 每个用例前恢复默认配置，避免用例之间相互污染 */
    @BeforeEach
    void resetJwt() {
        JwtUtils.init(SECRET, EXPIRATION);
    }

    private Map<String, Object> buildClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", 1);
        claims.put("name", "zhangsan");
        return claims;
    }

    @Test
    @DisplayName("签发后能解析出原始信息（网关与各服务共用同一密钥）")
    void genJwt_thenParse_shouldReturnOriginalClaims() {
        String token = JwtUtils.genJwt(buildClaims());
        assertNotNull(token);

        Claims claims = JwtUtils.parseJwt(token);
        assertNotNull(claims);
        assertEquals("zhangsan", claims.get("name"));
        assertEquals(1, claims.get("id"));
    }

    @Test
    @DisplayName("篡改 token 解析返回 null")
    void parseJwt_tamperedToken_shouldReturnNull() {
        String token = JwtUtils.genJwt(buildClaims());
        String tampered = token.substring(0, token.length() - 4) + "xxxx";
        assertNull(JwtUtils.parseJwt(tampered));
    }

    @Test
    @DisplayName("过期 token 解析返回 null")
    void parseJwt_expiredToken_shouldReturnNull() {
        // 以负数过期时间签发，token 立即过期
        JwtUtils.init(SECRET, -1000L);
        String token = JwtUtils.genJwt(buildClaims());
        assertNull(JwtUtils.parseJwt(token));
    }

    @Test
    @DisplayName("null / 空字符串解析返回 null")
    void parseJwt_nullOrEmpty_shouldReturnNull() {
        assertNull(JwtUtils.parseJwt(null));
        assertNull(JwtUtils.parseJwt(""));
    }

    @Test
    @DisplayName("从 token 提取用户名")
    void getUserNameFromToken_shouldReturnName() {
        String token = JwtUtils.genJwt(buildClaims());
        assertEquals("zhangsan", JwtUtils.getUserNameFromToken(token));
        assertNull(JwtUtils.getUserNameFromToken(null));
    }

    @Test
    @DisplayName("从 token 提取用户 id")
    void getUserIdFromToken_shouldReturnId() {
        String token = JwtUtils.genJwt(buildClaims());
        assertEquals(1, JwtUtils.getUserIdFromToken(token));
        assertNull(JwtUtils.getUserIdFromToken(null));
    }
}
