package org.example.blogsystem.common.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.spec.MGF1ParameterSpec;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * 密码加密传输（RSA-OAEP + SHA-256）
 * <p>
 * 前端用服务端公钥加密密码后再提交，抓包只能看到密文；服务端用私钥解密后再做哈希校验。
 * 密钥对在应用启动时生成，重启后更新，前端每次登录前从 /user/rsaPublicKey 获取。
 * 说明：主要是降低"抓包直接看到密码"的风险；链路仍建议 HTTPS（防中间人替换公钥）。
 */
public final class PasswordCipher {

    private static final KeyPair KEY_PAIR;
    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    static {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KEY_PAIR = generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("初始化 RSA 密钥失败", e);
        }
    }

    private PasswordCipher() {
    }

    /** 公钥（Base64，X.509 格式），供前端加密使用 */
    public static String publicKeyBase64() {
        return Base64.getEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded());
    }

    /**
     * 解密前端提交的密码。
     * <p>
     * 若不是合法的 RSA 密文（例如接口测试/脚本直接传明文），原样返回，保证向后兼容。
     */
    public static String decryptOrOriginal(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(value.trim());
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            OAEPParameterSpec spec = new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.DECRYPT_MODE, KEY_PAIR.getPrivate(), spec);
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    /** 取实际密码：优先用加密字段，其次用明文字段（兼容脚本调用） */
    public static String resolvePassword(String plain, String encrypted) {
        if (encrypted != null && !encrypted.isEmpty()) {
            return decryptOrOriginal(encrypted);
        }
        return plain;
    }
}
