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
        INVALID_TYPENAME(9),
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
        INVALID_BINARY_BIT_EXPR(34),
        INVALID_DEREFERENCE_EXPR(35),
        INVALID_CAST_EXPR(36),
        INVALID_ADDRESSOF_EXPR(37),
        NOT_MODIFIABLE_LVALUE(38),
        INVALID_SIMPLE_ASSIGN_EXPR(39),
        INVALID_ASSIGN_ADDITIVE_EXPR(40),
        INVALID_COMPOUND_ASSIGN_EXPR(41),
        INVALID_INCREMENT_EXPR(42),
        INVALID_CONDITIONAL_EXPR(43),
        INVALID_ARRAYREF_EXPR(44),
        INVALID_FIELDREF_EXPR(45),
        INVALID_FUNCTION_CALL(46),
        INVALID_PARAMETER_TYPE(47),
        INVALID_POST_TASK_EXPR(48),
        INVALID_INTERFACE_INSTANTIATION(49),
        INVALID_INTERFACE_PARAMETER(50),
        INVALID_NESC_CALL(51),
        INVALID_INTERFACE_ENTITY_DEFINITION(52),
        MISSING_IMPLEMENTATION_ELEMENT(53),
        INVALID_SPECIFIERS_COMBINATION(54),
        INVALID_TASK_DECLARATION(55),
        INVALID_FUNCTION_DECLARATION(56),
        INVALID_ENDPOINT(57),
        INVALID_CONNECTION(58),
        INVALID_RP_CONNECTION(59),
        INVALID_EQ_CONNECTION(60),
        INVALID_COMPONENT_INSTANTIATION(61),
        INVALID_COMPONENT_PARAMETER(62),
        MISSING_WIRING(63),
        INVALID_COMPONENT_TYPEREF(64),
        INVALID_INITIALIZER_USAGE(65),
        INVALID_CONSTANT_FUNCTION_CALL(66),
        INVALID_CATTRIBUTE_USAGE(67),
        INVALID_COMPONENTDEREF_EXPR(68),
        INVALID_BARE_ENTITY_DECLARATION(69),
        INVALID_SPECIFICATION_DECLARATION(70),
        INVALID_SCHEDULER(71),
        INVALID_TASK_INTERFACE(72),
        INVALID_COMBINE_ATTRIBUTE_USAGE(73),
        INVALID_LABEL_DECLARATION(74),
        ;

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
        INVALID_POINTER_COMPARISON(5),
        INVALID_POINTER_ASSIGNMENT(6),
        VOID_POINTER_ADVANCE(7),
        INVALID_POINTER_CONDITIONAL(8),
        VOID_POINTER_ARITHMETICS(9);

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
