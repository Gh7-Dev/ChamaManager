/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chamamanager.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Provides password hashing functionality using standard Java libraries.
 *
 * Passwords are hashed with SHA-256 so that plain-text passwords are not
 * stored. This is a straightforward, dependency-free hashing approach.
 *
 * @author gh7
 */
public class PasswordHasher {

    /**
     * Hashes the given plain-text password to a hex string.
     *
     * @param password the plain-text password to hash
     * @return the SHA-256 hex digest of the password
     * @throws IllegalStateException if the SHA-256 algorithm is unavailable on
     * the runtime platform
     */
    public static String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 hashing algorithm is not available.", ex);
        }
    }

    /**
     * Verifies a plain-text password against a stored hash.
     *
     * @param password the plain-text password to check
     * @param hashedPassword the previously stored hash to compare against
     * @return {@code true} if the password matches the stored hash
     */
    public static boolean verify(String password, String hashedPassword) {
        if (hashedPassword == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(password).getBytes(StandardCharsets.UTF_8),
                hashedPassword.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
