package com.example.Backend_J2EE.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {

    private static final String PREFIX = "pbkdf2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int DEFAULT_ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BYTES = 32;

    private static final SecureRandom RNG = new SecureRandom();

    private PasswordHasher() {
    }

    /**
     * Returns encoded password as: pbkdf2$iterations$saltBase64$hashBase64
     */
    public static String hash(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword is required");
        }
        byte[] salt = new byte[SALT_BYTES];
        RNG.nextBytes(salt);
        byte[] derived = pbkdf2(rawPassword.toCharArray(), salt, DEFAULT_ITERATIONS, KEY_BYTES);

        return PREFIX
                + "$" + DEFAULT_ITERATIONS
                + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derived);
    }

    /**
     * Supports both:
     * - pbkdf2$... encoded format
     * - legacy/plain text stored passwords (for existing seeded data)
     */
    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (!storedPassword.startsWith(PREFIX + "$")) {
            return slowEquals(rawPassword.getBytes(StandardCharsets.UTF_8), storedPassword.getBytes(StandardCharsets.UTF_8));
        }

        String[] parts = storedPassword.split("\\$");
        if (parts.length != 4) {
            return false;
        }

        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            return false;
        }

        byte[] salt;
        byte[] expected;
        try {
            salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException ex) {
            return false;
        }

        byte[] actual = pbkdf2(rawPassword.toCharArray(), salt, iterations, expected.length);
        return slowEquals(actual, expected);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyBytes) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBytes * 8);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Password hashing failure", ex);
        } finally {
            spec.clearPassword();
        }
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }
}
