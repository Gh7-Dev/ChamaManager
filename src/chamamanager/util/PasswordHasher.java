package chamamanager.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Provides password hashing using PBKDF2 (PBKDF2WithHmacSHA256).
 *
 * Stored format: iterations:salt_base64:hash_base64 Each password gets its own
 * random salt, and hashing is deliberately slow so that guessing passwords is
 * expensive.
 *
 * @author gh7
 */
public class PasswordHasher {

    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    /**
     * Hashes the given plain-text password with a new random salt.
     *
     * @param password the plain-text password to hash
     * @return a string in the form iterations:salt_base64:hash_base64
     */
    public static String hash(String password) {
        try {
            byte[] salt = new byte[SALT_BYTES];
            new SecureRandom().nextBytes(salt);
            byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS);
            return ITERATIONS + ":"
                    + Base64.getEncoder().encodeToString(salt) + ":"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Password hashing failed.", ex);
        }
    }

    /**
     * Verifies a plain-text password against a stored hash.
     *
     * @param password the plain-text password to check
     * @param hashedPassword the stored value produced by {@link #hash}
     * @return {@code true} if the password matches the stored hash
     */
    public static boolean verify(String password, String hashedPassword) {
        if (password == null || hashedPassword == null) {
            return false;
        }
        try {
            String[] parts = hashedPassword.split(":");
            if (parts.length != 3) {
                return false;
            }
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations)
            throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded();
    }
}
