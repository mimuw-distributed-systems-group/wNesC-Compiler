package pl.edu.mimuw.nesc.exception;

/**
 * Exception thrown when the operation of loading the ABI fails, e.g. when the
 * XML file does not exist or its contents are invalid.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class ABILoadFailureException extends Exception {
    private static final long serialVersionUID = 243442745572L;

    public ABILoadFailureException(String message) {
        super(message);
    }

    public ABILoadFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
