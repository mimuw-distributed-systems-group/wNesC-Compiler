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
     * Simple interface for getting the code from an enumeration value that
     * represents an issue.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    interface EnumCode {
        int getCodeNumber();
    }

    /**
     * Enumeration type with all errors.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum ErrorType implements EnumCode {
        INCOMPLETE_PARAMETER_TYPE(1),
        INCOMPLETE_VARIABLE_TYPE(2),
        INVALID_ARRAY_ELEMENT_TYPE(3),
        INVALID_FUNCTION_RETURN_TYPE(4),
        NO_TYPE_SPECIFIERS(5),
        TYPE_SPECIFIER_REPETITION(6),
        TYPE_SPECIFIERS_MIX_ERROR(7),
        UNDECLARED_IDENTIFIER(8),
        INVALID_IDENTIFIER_TYPE(9),
        REDEFINITION(10),
        CONFLICTING_TAG_KIND(11),
        ENUM_FORWARD_DECLARATION(12),
        UNDEFINED_ENUM_USAGE(13),
        ATTRIBUTE_USAGE_AS_TYPE(14),
        INVALID_FIELD_TYPE(15),
        REDECLARATION(16),
        INVALID_GENERIC_PARAM_SPECIFIERS(17),
        INVALID_INSTANCE_PARAM_SPECIFIERS(18),
        CONFLICTING_STORAGE_SPECIFIER(19),
        INTEGER_CONSTANT_OVERFLOW(20),
        INVALID_IDENTIFIER_USAGE(21),
        INVALID_SHIFT_EXPR_OPERANDS(22),
        INVALID_TYPE_QUERY_EXPR(23),
        INVALID_EXPR_QUERY_EXPR(24),
        INVALID_MULTIPLICATIVE_EXPR(25),
        INVALID_PLUS_EXPR(26),
        INVALID_MINUS_EXPR(27),
        INVALID_NOT_EXPR(28),
        INVALID_BITNOT_EXPR(29),
        INVALID_UNARY_ADDITIVE_EXPR(30),
        INVALID_COMPARE_EXPR(31),
        INVALID_EQUALITY_EXPR(32),
        INVALID_BINARY_LOGICAL_EXPR(33),
        INVALID_BINARY_BIT_EXPR(34);

        private final int codeNumber;

        private ErrorType(int codeNumber) {
            this.codeNumber = codeNumber;
        }

        @Override
        public int getCodeNumber() {
            return codeNumber;
        }
    }

    /**
     * Enumeration type with all warnings.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum WarningType implements EnumCode {
        TYPE_QUALIFIER_REPETITION(1),
        INVALID_RESTRICT_USAGE(2),
        SUPERFLUOUS_SPECIFIERS(3),
        NON_TYPE_SPECIFIER_REPETITION(4),
        INVALID_POINTER_COMPARISON(5);

        private final int codeNumber;

        private WarningType(int codeNumber) {
            this.codeNumber = codeNumber;
        }

        @Override
        public int getCodeNumber() {
            return codeNumber;
        }
    }
}
