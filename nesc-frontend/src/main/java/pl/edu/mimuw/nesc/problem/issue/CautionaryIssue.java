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
     * @param code Code for this warning obtained by invoking method
     *             {@link WarningCode#onlyInstance}.
     * @throws NullPointerException Given argument is null.
     */
    protected CautionaryIssue(WarningCode code) {
        super(code, Kind.WARNING);
    }

    /**
     * Class that represents a warning code.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    protected static class WarningCode extends AbstractCode {
        /**
         * Factory that creates the bijection between warning code objects and
         * elements of WarningType enumeration.
         */
        private static final WarningCodeFactory factory = new WarningCodeFactory();

        /**
         * Get the one and only instance for the given warning type.
         *
         * @param warningType Type of the warning that the code is to be
         *                    obtained for.
         * @return The one and only instance of the code for given warning type.
         */
        protected static WarningCode onlyInstance(WarningType warningType) {
            return factory.getInstance(warningType);
        }

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

    /**
     * Factory for warning codes.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class WarningCodeFactory extends CodeFactory<WarningCode, WarningType> {

        private WarningCodeFactory() {
            super(WarningType.class);
        }

        @Override
        protected WarningCode newCode(int codeNumber) {
            return new WarningCode(codeNumber);
        }
    }
}
