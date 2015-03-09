package pl.edu.mimuw.nesc.optimization;

/**
 * <p>Exception that is thrown when the task optimization cannot be completed
 * due to unexpected wiring. The exact problem is explained in the exception
 * message.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnexpectedWiringException extends Exception {
    UnexpectedWiringException(String message) {
        super(message);
    }
}
