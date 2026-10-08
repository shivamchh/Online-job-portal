package com.example.Online_job_portal.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility for cryptographically secure password hashing using SHA-256 with per-user salt.
 * Uses SecureRandom for salt generation to prevent dictionary and rainbow table attacks.
 */
public final class PasswordUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
     * Hashes password with SHA-256 algorithm combined with a per-user salt.
     *
     * @param password Raw plaintext password
     * @param salt     Base64-encoded cryptographic salt
     * @return Hexadecimal representation of SHA-256 digest
     */
    public static String hashPassword(String password, String salt) {
        if (salt == null || salt.isBlank()) {
            return hashPassword(password);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] saltBytes;
            try {
                saltBytes = Base64.getDecoder().decode(salt);
            } catch (IllegalArgumentException e) {
                saltBytes = salt.getBytes(StandardCharsets.UTF_8);
            }
            digest.update(saltBytes);
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : hash) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    /**
     * Plain SHA-256 hashing maintained for backwards compatibility.
     *
     * @param password Raw plaintext password
     * @return Hexadecimal representation of SHA-256 digest
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();

            for (byte value : hash) {
                result.append(String.format("%02x", value));
            }

            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    /**
     * Verifies raw candidate password against stored salt and hashed value.
     *
     * @param rawPassword    Plaintext candidate password
     * @param salt           Stored per-user cryptographic salt
     * @param hashedPassword Stored password hash
     * @return true if candidate password matches stored hash
     */
    public static boolean matches(String rawPassword, String salt, String hashedPassword) {
        if (hashedPassword == null) {
            return false;
        }
        if (salt == null || salt.isBlank()) {
            return matches(rawPassword, hashedPassword);
        }
        return hashPassword(rawPassword, salt).equals(hashedPassword);
    }

    /**
     * Backward-compatible match verification for unsalted hashes.
     */
    public static boolean matches(String rawPassword, String hashedPassword) {
        return hashPassword(rawPassword).equals(hashedPassword);
    }
}
