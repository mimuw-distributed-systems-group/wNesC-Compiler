package pl.edu.mimuw.nesc.astwriting;

import com.google.common.collect.ImmutableMap;
import pl.edu.mimuw.nesc.ast.NescCallKind;

import static com.google.common.base.Preconditions.*;

/**
 * Class with textual representations of various tokens significant in NesC
 * and C languages.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Tokens {
    /**
     * Parentheses.
     */
    public static final String LPAREN = "(";
    public static final String RPAREN = ")";
    public static final String LBRACK = "[";
    public static final String RBRACK = "]";
    public static final String LBRACE = "{";
    public static final String RBRACE = "}";
    public static final String LANGLE = "<";
    public static final String RANGLE = ">";

    /**
     * Statements keywords.
     */
    public static final String STMT_IF = "if";
    public static final String STMT_ELSE = "else";
    public static final String STMT_FOR = "for";
    public static final String STMT_WHILE = "while";
    public static final String STMT_DO = "do";
    public static final String STMT_GOTO = "goto";
    public static final String STMT_BREAK = "break";
    public static final String STMT_CONTINUE = "continue";
    public static final String STMT_RETURN = "return";
    public static final String STMT_SWITCH = "switch";

    /**
     * Label keywords.
     */
    public static final String LBL_DEFAULT = "default";
    public static final String LBL_CASE = "case";

    /**
     * Letter unary operators.
     */
    public static final String OP_ALIGNOF = "_Alignof";
    public static final String OP_SIZEOF = "sizeof";
    public static final String OP_REALPART = "__real";
    public static final String OP_IMAGPART = "__imag";
    public static final String OP_TYPEOF = "typeof";

    /**
     * Tags keywords.
     */
    public static final String TAG_STRUCT = "struct";
    public static final String TAG_UNION = "union";
    public static final String TAG_ENUM = "enum";
    public static final String TAG_NX_STRUCT = "nx_struct";
    public static final String TAG_NX_UNION = "nx_union";

    /**
     * Whitespace characters.
     */
    public static final String SPACE = " ";
    public static final String TAB = "\t";

    /**
     * NesC-specific tokens.
     */
    public static final String NESC_CALL = "call";
    public static final String NESC_SIGNAL = "signal";
    public static final String NESC_POST = "post";
    public static final String NESC_COMPONENTS = "components";
    public static final String NESC_NEW = "new";
    public static final String NESC_AS = "as";
    public static final String NESC_IMPLEMENTATION = "implementation";
    public static final String NESC_ATOMIC = "atomic";
    public static final String NESC_GENERIC = "generic";
    public static final String NESC_CONFIGURATION = "configuration";
    public static final String NESC_MODULE = "module";
    public static final String NESC_BINARY_COMPONENT = "component";
    public static final String NESC_PROVIDES = "provides";
    public static final String NESC_USES = "uses";
    public static final String NESC_INTERFACE = "interface";
    public static final String NESC_EQUATE_WIRES = "=";
    public static final String NESC_LINK_WIRES = "->";

    /**
     * GCC-specific tokens.
     */
    public static final String GCC_EXTENSION = "__extension__";
    public static final String GCC_ATTRIBUTE = "__attribute__";
    public static final String GCC_BUILTIN_VA_ARG = "__builtin_va_arg";
    public static final String GCC_ASM = "__asm__";

    /**
     * SDCC-specific tokens.
     */
    public static final String SDCC_BANKED = "__banked";

    /**
     * Other symbols.
     */
    public static final String APOSTROPHE = "'";
    public static final String QUOTATION_MARK = "\"";
    public static final String COMMA = ",";
    public static final String COMMA_WITH_SPACE = COMMA + SPACE;
    public static final String COLON = ":";
    public static final String SEMICOLON = ";";
    public static final String AT = "@";
    public static final String ELLIPSIS = "...";
    public static final String ASTERISK = "*";
    public static final String MACRO_OFFSETOF = "offsetof";

    /**
     * Map with call keywords for particular call kinds.
     */
    public static final ImmutableMap<NescCallKind, String> CALL_KEYWORDS = ImmutableMap.of(
            NescCallKind.COMMAND_CALL, NESC_CALL,
            NescCallKind.EVENT_SIGNAL, NESC_SIGNAL,
            NescCallKind.POST_TASK, NESC_POST
    );

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
        ARROW("->"),
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
    private Tokens() {
    }
}
