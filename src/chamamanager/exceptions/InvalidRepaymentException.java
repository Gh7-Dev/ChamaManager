package chamamanager.exceptions;

/**
 * Thrown when a repayment is not allowed(bad amount, overpayment, loan not
 * repayable)
 *
 */
public class InvalidRepaymentException extends Exception {

    public InvalidRepaymentException() {
        super();
    }

    public InvalidRepaymentException(String message) {
        super(message);
    }

    public InvalidRepaymentException(String message, Throwable cause) {
        super(message, cause);
    }

}
