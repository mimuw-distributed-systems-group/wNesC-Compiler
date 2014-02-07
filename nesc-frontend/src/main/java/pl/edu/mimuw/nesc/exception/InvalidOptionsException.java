package pl.edu.mimuw.nesc.exception;

/**
 * Represents exception caused by invalid options (e.g. unknown option,
 * invalid number of arguments, etc).
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class InvalidOptionsException extends Exception {

    public InvalidOptionsException(String message) {
        super(message);
    }

    public InvalidOptionsException(String message, Throwable cause) {
        super(message, cause);
    }
}
