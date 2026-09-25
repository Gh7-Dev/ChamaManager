package chamamanager.exceptions;

public class DuplicateContributionException extends Exception {

    public DuplicateContributionException() {
        super();
    }

    public DuplicateContributionException(String message) {
        super(message);
    }

    public DuplicateContributionException(String message, Throwable cause) {
        super(message, cause);
    }
}
