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
     * @param errorType Type of this error.
     */
    protected ErroneousIssue(ErrorType errorType) {
        super(new ErrorCode(errorType.getCodeNumber()), Kind.ERROR);
    }

    /**
     * A class that represents an error code.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class ErrorCode extends AbstractCode {
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
}
