package chamamanager.exceptions;

/**
 * Thrown when an action is not allowed in the current loan state e.g Approving
 * a non existing loan
 *
 * @author gh7
 */
public class LoanStateException extends Exception {

    public LoanStateException() {
        super();
    }

    public LoanStateException(String message) {
        super(message);
    }

    public LoanStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
