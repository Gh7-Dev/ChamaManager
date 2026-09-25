package chamamanager.logic;

import chamamanager.exceptions.InvalidLoginException;
import chamamanager.util.PasswordHasher;

/**
 * Handles authentication-related operations.
 *
 * Validates login credentials and uses {@link InvalidLoginException} to signal
 * failed authentication.
 *
 * @author gh7
 */
public class AuthService {

    /**
     * Validates a username/password pair.
     *
     * @param username the account username
     * @param password the plain-text password to verify
     * @return {@code true} if the credentials are valid
     * @throws InvalidLoginException if the credentials are not valid
     */
    public boolean login(String username, String password)
            throws InvalidLoginException {
        // TODO: credential verification depends on a lookup method not yet
        // defined (pending in the DAO layer); add the account lookup once
        // finalized.
        return false;
    }

    /**
     * Verifies a plain-text password against a stored hash.
     *
     * @param password the plain-text password
     * @param storedHash the stored password hash
     * @return {@code true} if the password matches the stored hash
     */
    public boolean verifyPassword(String password, String storedHash) {
        return PasswordHasher.verify(password, storedHash);
    }
}
