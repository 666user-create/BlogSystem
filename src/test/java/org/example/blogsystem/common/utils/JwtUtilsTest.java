package org.example.blogsystem.common.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtUtilsTest {

    private static final String SECRET = "dVnsmy+SIX6pNptQdeclDSJ26EMSPEIhvZYKBTTug4k=";
    private static final long EXPIRATION = 24 * 60 * 60 * 1000L;

    /**
     * 每个用例前恢复默认配置，避免用例之间相互污染
     */
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
    void genJwt_thenParse_shouldReturnOriginalClaims() {
        String token = JwtUtils.genJwt(buildClaims());
        assertNotNull(token);

        Claims claims = JwtUtils.parseJwt(token);
        assertNotNull(claims);
        assertEquals("zhangsan", claims.get("name"));
        assertEquals(1, claims.get("id"));
    }

    @Test
    void parseJwt_tamperedToken_shouldReturnNull() {
        String token = JwtUtils.genJwt(buildClaims());
        // 篡改 token 末尾字符，破坏签名
        String tampered = token.substring(0, token.length() - 4) + "xxxx";
        assertNull(JwtUtils.parseJwt(tampered));
    }

    @Test
    void parseJwt_expiredToken_shouldReturnNull() {
        // 以负数过期时间签发，token 立即过期
        JwtUtils.init(SECRET, -1000L);
        String token = JwtUtils.genJwt(buildClaims());
        assertNull(JwtUtils.parseJwt(token));
    }

    @Test
    void parseJwt_nullOrEmpty_shouldReturnNull() {
        assertNull(JwtUtils.parseJwt(null));
        assertNull(JwtUtils.parseJwt(""));
    }

    @Test
    void getUserNameFromToken_shouldReturnName() {
        String token = JwtUtils.genJwt(buildClaims());
        assertEquals("zhangsan", JwtUtils.getUserNameFromToken(token));
        assertNull(JwtUtils.getUserNameFromToken(null));
    }

    @Test
    void getUserIdFromToken_shouldReturnId() {
        String token = JwtUtils.genJwt(buildClaims());
        assertEquals(1, JwtUtils.getUserIdFromToken(token));
        assertNull(JwtUtils.getUserIdFromToken(null));
    }
}
