package pl.edu.mimuw.nesc.problem.issue;

import static pl.edu.mimuw.nesc.problem.issue.Issues.ErrorType;

/**
 * Class that represents an error in the code. It is not named
 * <code>Error</code> to avoid conflict with the Java core class:
 *
 * <code>java.lang.Error</code>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see CautionaryIssue
 */
public abstract class ErroneousIssue extends AbstractIssue {
    /**
     * Initialize this issue.
     *
     * @param code The code for this error obtained by using
     *             {@link ErrorCode#onlyInstance} method.
     */
    protected ErroneousIssue(ErrorCode code) {
        super(code, Kind.ERROR);
    }

    /**
     * A class that represents an error code.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    protected static class ErrorCode extends AbstractCode {
        /**
         * Factory for error codes.
         */
        private static final ErrorCodeFactory factory = new ErrorCodeFactory();

        /**
         * Get the instance of this class.
         *
         * @param errorType Error type that the instance will be returned for.
         * @return The unique and only instance of this class for the given
         *         error type.
         */
        protected static ErrorCode onlyInstance(ErrorType errorType) {
            return factory.getInstance(errorType);
        }

        /**
         * Initializes this error code with given number.
         *
         * @param errorCodeNumber Number of this error code.
         */
        private ErrorCode(int errorCodeNumber) {
            super(errorCodeNumber);
        }

        @Override
        public String asString() {
            return "E" + getCodeNumber();
        }
    }

    /**
     * Factory for error codes.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class ErrorCodeFactory extends CodeFactory<ErrorCode, ErrorType> {

        private ErrorCodeFactory() {
            super(ErrorType.class);
        }

        @Override
        protected ErrorCode newCode(int codeNumber) {
            return new ErrorCode(codeNumber);
        }
    }
}
