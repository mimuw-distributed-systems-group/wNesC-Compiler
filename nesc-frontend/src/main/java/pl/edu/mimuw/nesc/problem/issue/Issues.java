package pl.edu.mimuw.nesc.problem.issue;

/**
 * A class that is intended to provide a summary of all concrete issues and
 * allow easy management of their codes.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Issues {
    /**
     * Private constructor to prevent this class from being instantiated.
     */
    private Issues() {
    }

    /**
     * Enumeration type with all errors.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum ErrorType {
        INCOMPLETE_PARAMETER_TYPE(1),
        INCOMPLETE_VARIABLE_TYPE(2),
        INVALID_ARRAY_ELEMENT_TYPE(3),
        INVALID_FUNCTION_RETURN_TYPE(4),
        NO_TYPE_SPECIFIERS(5),
        TYPE_SPECIFIER_REPETITION(6),
        TYPE_SPECIFIERS_MIX_ERROR(7),
        UNDECLARED_IDENTIFIER(8),
        INVALID_IDENTIFIER_TYPE(9);

        private final int codeNumber;

        private ErrorType(int codeNumber) {
            this.codeNumber = codeNumber;
        }

        public int getCodeNumber() {
            return codeNumber;
        }
    }

    /**
     * Enumeration type with all warnings.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum WarningType {
        TYPE_QUALIFIER_REPETITION(1),
        INVALID_RESTRICT_USAGE(2),
        SUPERFLUOUS_SPECIFIERS(3);

        private final int codeNumber;

        private WarningType(int codeNumber) {
            this.codeNumber = codeNumber;
        }

        public int getCodeNumber() {
            return codeNumber;
        }
    }
}
