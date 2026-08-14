package org.example.blogsystem.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityUtilTest {

    @Test
    void encryptThenVerify_shouldPass() {
        String password = "123456";
        String encrypted = SecurityUtil.encrypt(password);
        assertTrue(SecurityUtil.verify(password, encrypted));
    }

    @Test
    void verify_wrongPassword_shouldFail() {
        String encrypted = SecurityUtil.encrypt("123456");
        assertFalse(SecurityUtil.verify("wrong", encrypted));
    }

    @Test
    void verify_nullInput_shouldFail() {
        assertFalse(SecurityUtil.verify(null, "whatever"));
        assertFalse(SecurityUtil.verify("123456", null));
    }

    @Test
    void encrypt_samePassword_shouldProduceDifferentResult() {
        // 盐是随机生成的：同一密码每次加密结果必须不同
        String password = "123456";
        assertNotEquals(SecurityUtil.encrypt(password), SecurityUtil.encrypt(password));
    }

    @Test
    void encrypt_resultShouldBe64HexChars() {
        // 存储格式 = 32位盐(hex) + 32位MD5(hex)
        assertEquals(64, SecurityUtil.encrypt("123456").length());
    }
}
