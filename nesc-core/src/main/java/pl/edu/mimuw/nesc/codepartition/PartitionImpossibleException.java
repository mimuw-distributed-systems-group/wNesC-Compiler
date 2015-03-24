package pl.edu.mimuw.nesc.codepartition;

/**
 * <p>Exception thrown when partition of functions is not possible to be done.
 * The message can optionally contain the reason.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class PartitionImpossibleException extends Exception {
    PartitionImpossibleException() {
    }

    PartitionImpossibleException(String message) {
        super(message);
    }

    PartitionImpossibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
