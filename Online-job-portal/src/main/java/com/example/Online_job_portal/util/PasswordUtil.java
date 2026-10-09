package com.example.Online_job_portal.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Utility for cryptographically secure password hashing using PBKDF2-HMAC-SHA256
 * with slow key derivation (65,536 iterations) and per-user cryptographic salts.
 * Eliminates plaintext comparisons and defends against dictionary/rainbow-table attacks.
 */
public final class PasswordUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int PBKDF2_KEY_LENGTH = 256;
    private static final String PBKDF2_PREFIX = "pbkdf2:65536:";

    private PasswordUtil() {
    }

    /**
     * Generates a cryptographically strong 16-byte random salt.
     *
     * @return Base64-encoded salt string
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes password using slow PBKDF2WithHmacSHA256 with per-user salt.
     *
     * @param password Raw plaintext password
     * @param salt     Base64-encoded cryptographic salt
     * @return Formatted PBKDF2 hash string or salted hash
     */
    public static String hashPassword(String password, String salt) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (salt == null || salt.isBlank()) {
            salt = generateSalt();
        }
        try {
            byte[] saltBytes;
            try {
                saltBytes = Base64.getDecoder().decode(salt);
            } catch (IllegalArgumentException e) {
                saltBytes = salt.getBytes(StandardCharsets.UTF_8);
            }
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return PBKDF2_PREFIX + salt + ":" + sb;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Fallback to salted SHA-256 if PBKDF2 algorithm is unavailable in runtime
            return hashSha256(password, salt);
        }
    }

    /**
     * Hashes password generating a fresh cryptographic salt.
     */
    public static String hashPassword(String password) {
        return hashPassword(password, generateSalt());
    }

    /**
     * Verifies candidate raw password against stored hash using constant-time comparison.
     * Plaintext matches are strictly rejected.
     *
     * @param rawPassword    Plaintext candidate password
     * @param salt           Stored per-user cryptographic salt
     * @param hashedPassword Stored password hash
     * @return true if candidate password matches stored hash
     */
    public static boolean matches(String rawPassword, String salt, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null || hashedPassword.isBlank()) {
            return false;
        }

        // 1. Verify PBKDF2 hash format
        if (hashedPassword.startsWith(PBKDF2_PREFIX)) {
            String remainder = hashedPassword.substring(PBKDF2_PREFIX.length());
            int sepIndex = remainder.indexOf(':');
            if (sepIndex > 0) {
                String embeddedSalt = remainder.substring(0, sepIndex);
                String expectedHashHex = remainder.substring(sepIndex + 1);
                String computed = hashPassword(rawPassword, embeddedSalt);
                return MessageDigest.isEqual(
                        computed.getBytes(StandardCharsets.UTF_8),
                        hashedPassword.getBytes(StandardCharsets.UTF_8)
                );
            }
        }

        // 2. Migration support: verify legacy salted SHA-256 hash using constant-time comparison
        if (salt != null && !salt.isBlank()) {
            String legacySalted = hashSha256(rawPassword, salt);
            if (MessageDigest.isEqual(
                    legacySalted.toLowerCase().getBytes(StandardCharsets.UTF_8),
                    hashedPassword.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }

        // 3. Migration support: verify legacy unsalted SHA-256 hash using constant-time comparison
        String legacyUnsalted = hashSha256(rawPassword, null);
        return MessageDigest.isEqual(
                legacyUnsalted.toLowerCase().getBytes(StandardCharsets.UTF_8),
                hashedPassword.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Backward-compatible match verification for hashes without separate salt parameter.
     */
    public static boolean matches(String rawPassword, String hashedPassword) {
        return matches(rawPassword, null, hashedPassword);
    }

    private static String hashSha256(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (salt != null && !salt.isBlank()) {
                byte[] saltBytes;
                try {
                    saltBytes = Base64.getDecoder().decode(salt);
                } catch (IllegalArgumentException e) {
                    saltBytes = salt.getBytes(StandardCharsets.UTF_8);
                }
                digest.update(saltBytes);
            }
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
