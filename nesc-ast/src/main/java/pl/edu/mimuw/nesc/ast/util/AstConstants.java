package pl.edu.mimuw.nesc.ast.util;

import static com.google.common.base.Preconditions.*;

/**
 * Class that contains useful constants.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class AstConstants {
    /**
     * Parentheses.
     */
    public static final String LPAREN = "(";
    public static final String RPAREN = ")";
    public static final String LBRACK = "[";
    public static final String RBRACK = "]";

    /**
     * Letter unary operators.
     */
    public static final String OP_ALIGNOF = "_Alignof";
    public static final String OP_SIZEOF = "sizeof";
    public static final String OP_REALPART = "__real";
    public static final String OP_IMAGPART = "__imag";

    /**
     * Call kinds keywords.
     */
    public static final String CALL_COMMAND = "call";
    public static final String CALL_EVENT = "signal";
    public static final String CALL_TASK = "post";

    /**
     * Other symbols.
     */
    public static final String APOSTROPHE = "'";
    public static final String QUOTATION_MARK = "\"";
    public static final String SPACE = " ";

    /**
     * Enumerated type that represents a binary operator.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum BinaryOp {
        PLUS("+"),
        MINUS("-"),
        TIMES("*"),
        DIVIDE("/"),
        MODULO("%"),
        LSHIFT("<<"),
        RSHIFT(">>"),
        LEQ("<="),
        GEQ(">="),
        LT("<"),
        GT(">"),
        EQ("=="),
        NE("!="),
        BITAND("&"),
        BITOR("|"),
        BITXOR("^"),
        ANDAND("&&"),
        OROR("||"),
        ASSIGN("="),
        ASSIGN_LSHIFT("<<="),
        ASSIGN_PLUS("+="),
        ASSIGN_BITXOR("^="),
        ASSIGN_MINUS("-="),
        ASSIGN_RSHIFT(">>="),
        ASSIGN_TIMES("*="),
        ASSIGN_BITAND("&="),
        ASSIGN_DIVIDE("/="),
        ASSIGN_BITOR("|="),
        ASSIGN_MODULO("%=");

        private final String textualRepresentation;

        private BinaryOp(String textualRepresentation) {
            checkNotNull(textualRepresentation, "textual representation of a binary operator cannot be null");
            checkArgument(!textualRepresentation.isEmpty(), "textual representation of an unary operator cannot be empty");

            this.textualRepresentation = textualRepresentation;
        }

        @Override
        public String toString() {
            return textualRepresentation;
        }
    }

    /**
     * Enumerated type that represents an unary operator.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum UnaryOp {
        UNARY_PLUS("+"),
        UNARY_MINUS("-"),
        DEREFERENCE("*"),
        ADDRESSOF("&"),
        BITNOT("~"),
        NOT("!"),
        DOT("."),
        INCREMENT("++"),
        DECREMENT("--"),
        LABELADDRESS("&&");

        private final String textualRepresentation;

        private UnaryOp(String textualRepresentation) {
            checkNotNull(textualRepresentation, "textual representation of an unary operator cannot be null");
            checkArgument(!textualRepresentation.isEmpty(), "textual representation of an unary operator cannot be empty");

            this.textualRepresentation = textualRepresentation;
        }

        @Override
        public String toString() {
            return textualRepresentation;
        }
    }

    /**
     * Enumerated type that represents a NesC compile-time constant function.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum ConstantFun {
        UNIQUE("unique"),
        UNIQUEN("uniqueN"),
        UNIQUECOUNT("uniqueCount"),
        ;

        private final String textualRepresentation;

        private ConstantFun(String textualRepresentation) {
            checkNotNull(textualRepresentation, "textual representation of a NesC constant function cannot be null");
            checkArgument(!textualRepresentation.isEmpty(), "textual representation of a NesC constant function cannot an empty string");

            this.textualRepresentation = textualRepresentation;
        }

        @Override
        public String toString() {
            return textualRepresentation;
        }
    }

    /**
     * Private constructor to prevent instantiating this class.
     */
    private AstConstants() {
    }
}
