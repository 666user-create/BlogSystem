package com.example.blogcloud.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SecurityUtil 单元测试（与单体工程同一套套路）
 * ============================================================
 * 覆盖：加盐哈希往返校验、错误密码、null 输入、盐随机性、存储格式长度。
 * ============================================================
 */
class SecurityUtilTest {

    @Test
    @DisplayName("加密后再校验应通过")
    void encryptThenVerify_shouldPass() {
        String password = "123456";
        String encrypted = SecurityUtil.encrypt(password);
        assertTrue(SecurityUtil.verify(password, encrypted));
    }

    @Test
    @DisplayName("错误密码校验失败")
    void verify_wrongPassword_shouldFail() {
        String encrypted = SecurityUtil.encrypt("123456");
        assertFalse(SecurityUtil.verify("wrong", encrypted));
    }

    @Test
    @DisplayName("null 输入校验失败（不抛异常）")
    void verify_nullInput_shouldFail() {
        assertFalse(SecurityUtil.verify(null, "whatever"));
        assertFalse(SecurityUtil.verify("123456", null));
    }

    @Test
    @DisplayName("同一密码每次加密结果不同（盐随机）")
    void encrypt_samePassword_shouldProduceDifferentResult() {
        String password = "123456";
        assertNotEquals(SecurityUtil.encrypt(password), SecurityUtil.encrypt(password));
    }

    @Test
    @DisplayName("存储格式为 32 位盐 + 32 位 MD5 = 64 字符")
    void encrypt_resultShouldBe64HexChars() {
        assertEquals(64, SecurityUtil.encrypt("123456").length());
    }
}
