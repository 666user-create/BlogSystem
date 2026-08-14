package org.example.blogsystem.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtUtilsTest {

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
        // 用与 JwtUtils 相同的密钥手动构造一个已过期的 token
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JwtUtils.secret));
        String expiredToken = Jwts.builder()
                .setClaims(buildClaims())
                .setIssuedAt(new Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000)) // 2小时前签发
                .setExpiration(new Date(System.currentTimeMillis() - 60 * 60 * 1000))    // 1小时前已过期
                .signWith(key)
                .compact();
        assertNull(JwtUtils.parseJwt(expiredToken));
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
