package org.example.blogsystem.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class SecurityUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String encrypt(String plainPassword) {
        String salt = randomSalt32();
        String md5 = md5Hex(plainPassword + salt);
        return salt + md5;
    }

    public static boolean verify(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null) {
            return false;
        }
        String plain = plainPassword.trim();
        String stored = storedPassword.trim();

        if (stored.length() == 64) {
            String salt = stored.substring(0, 32);
            String md5 = stored.substring(32);
            return md5.equalsIgnoreCase(md5Hex(plain + salt));
        }

        return stored.equals(plain);
    }

    private static String randomSalt32() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}

