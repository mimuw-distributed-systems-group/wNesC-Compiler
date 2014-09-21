package pl.edu.mimuw.nesc.problem.issue;

import static pl.edu.mimuw.nesc.problem.issue.Issues.WarningType;

/**
 * Class that represents a warning. It is named <code>CautionaryIssue</code>
 * instead of <code>Warning</code> to be consistent with the name of
 * <code>ErroneousIssue</code> class.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see ErroneousIssue
 */
public abstract class CautionaryIssue extends AbstractIssue {
    /**
     * Initializes this warning issue.
     *
     * @param warningType Type of this warning.
     */
    protected CautionaryIssue(WarningType warningType) {
        super(new WarningCode(warningType.getCodeNumber()), Kind.WARNING);
    }

    /**
     * Class that represents a warning code.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class WarningCode extends AbstractCode {
        /**
         * Initialize this warning code with given number.
         *
         * @param warningCodeNumber Number of this warning code.
         */
        private WarningCode(int warningCodeNumber) {
            super(warningCodeNumber);
        }

        @Override
        public String asString() {
            return "W" + getCodeNumber();
        }
    }
}
