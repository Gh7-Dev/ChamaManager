package chamamanager.logic;
import chamamanager.dao.MemberDAO;
import chamamanager.model.Member;
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
    private MemberDAO memberDAO = new MemberDAO();

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

    Member member = memberDAO.findByUsername(username);

    if (member == null) {
        throw new InvalidLoginException("Invalid username or password");
    }

    if (!verifyPassword(password, member.getPasswordHash())) {
        throw new InvalidLoginException("Invalid username or password");
    }

    return true;
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
