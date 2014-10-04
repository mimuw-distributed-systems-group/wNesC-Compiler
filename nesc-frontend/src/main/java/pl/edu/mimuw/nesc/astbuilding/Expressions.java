package pl.edu.mimuw.nesc.astbuilding;

import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;
import pl.edu.mimuw.nesc.ast.*;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.AstUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>
 * Contains a set of methods useful for creating syntax tree nodes during
 * parsing.
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class Expressions {

    private static final ErrorExpr ERROR_EXPRESSION;

    static {
        final Location errorLocation = new Location("", 0, 0);
        ERROR_EXPRESSION = new ErrorExpr(errorLocation);
        ERROR_EXPRESSION.setEndLocation(errorLocation);
    }

    /**
     * Value of the maximum integer literal that is allowed.
     */
    private static final BigInteger MAX_INTEGER_CST_VALUE = BigInteger
            .valueOf(2L).pow(64).subtract(BigInteger.ONE);

    /**
     * Maximum number of characters that is needed to write the maximum integer
     * literal (after truncating unnecessary leading zeroes). It occurs in the
     * octal base: 01777777777777777777777.
     */
    private static final int MAX_INTEGER_CST_CHARS_COUNT = 23;

    /**
     * Map with simple escape sequences as keys and characters associated with
     * them as values.
     */
    private static final ImmutableMap<String, Character> SIMPLE_ESCAPE_SEQUENCES;
    static {
        ImmutableMap.Builder<String, Character> builder = ImmutableMap.builder();
        builder.put("\\'", '\'');
        builder.put("\\\"", '"');
        builder.put("\\?", '?');
        builder.put("\\\\", '\\');
        builder.put("\\a", '\u0007');
        builder.put("\\b", '\b');
        builder.put("\\f", '\f');
        builder.put("\\n", '\n');
        builder.put("\\r", '\r');
        builder.put("\\t", '\t');
        builder.put("\\v", '\u000b');
        SIMPLE_ESCAPE_SEQUENCES = builder.build();
    }

    /**
     * Regular expression to detect an octal escape sequence in a character
     * literal.
     */
    private static final String OCTAL_ESCAPE_SEQUENCE_REGEXP = "\\\\[0-7]{1,3}";

    /**
     * Regular expression to detect a hexadecimal escape sequence in a character
     * literal.
     */
    private static final String HEXADECIMAL_ESCAPE_SEQUENCE_REGEXP = "\\\\x[0-9a-fA-F]+";

    public static ErrorExpr makeErrorExpr() {
        return ERROR_EXPRESSION;
    }

    public static Type defaultConversionForAssignment(Expression e) {
        // TODO
        return null;
    }

    public static Comma makeComma(Location startLocation, LinkedList<Expression> expressions) {
        final Comma result = new Comma(startLocation, expressions);
        final Optional<Location> endLocation = AstUtils.getEndLocation(expressions);
        result.setEndLocation(endLocation.get());
        return result;
    }

    public static SizeofType makeSizeofType(Location startLocation, Location endLocation, AstType astType) {
        final SizeofType result = new SizeofType(startLocation, astType);
        result.setEndLocation(endLocation);
        return result;
    }

    public static AlignofType makeAlignofType(Location startLocation, Location endLocation, AstType astType) {
        final AlignofType result = new AlignofType(startLocation, astType);
        result.setEndLocation(endLocation);
        return result;
    }

    public static LabelAddress makeLabelAddress(Location startLocation, IdLabel idLabel) {
        final LabelAddress result = new LabelAddress(startLocation, idLabel);
        result.setEndLocation(idLabel.getEndLocation());
        return result;
    }

    public static CastList makeCastList(Location startLocation, Location endLocation, AstType astType, Expression exp) {
        CastList result = new CastList(startLocation, astType, exp);
        result.setEndLocation(endLocation);
        return result;
    }

    public static Conditional makeConditional(Expression condition, Optional<Expression> trueExp, Expression falseExp) {
        final Conditional result = new Conditional(condition.getLocation(), condition, trueExp, falseExp);
        result.setEndLocation(falseExp.getEndLocation());
        return result;
    }

    public static Identifier makeIdentifier(Location startLocation, Location endLocation, String id,
                                            boolean maybeImplicit) {
        final Identifier result = new Identifier(startLocation, id);
        result.setEndLocation(endLocation);
        return result;
    }

    public static CompoundExpr makeCompoundExpr(Location startLocation, Location endLocation, Statement statement) {
        final CompoundExpr result = new CompoundExpr(startLocation, statement);
        result.setEndLocation(endLocation);
        return result;
    }

    public static FunctionCall makeFunctionCall(Location location, Location endLocation, Expression function,
                                                LinkedList<Expression> args) {
        final FunctionCall result = new FunctionCall(location, function, args, null, null);
        result.setEndLocation(endLocation);
        return result;
    }

    public static FunctionCall makeVaArg(Location startLocation, Location endLocation, LinkedList<Expression> args,
                                         AstType type) {
        // FIXME: second argument
        final FunctionCall result = new FunctionCall(startLocation, null, args, type, NescCallKind.NORMAL_CALL);
        result.setEndLocation(endLocation);
        return result;
    }

    public static Expression makeArrayRef(Location startLocation, Location endLocation, Expression array,
                                          LinkedList<Expression> index) {
        // NOTICE: ambiguity generic call or array reference
        final ArrayRef result;
        if (false) {
            // FIXME: generic call
        } else {
            // TODO: index to make comma if list size > 1
            result = new ArrayRef(startLocation, array, index);
        }

        result.setEndLocation(endLocation);
        return result;
    }

    public static FieldRef makeFieldRef(Location startLocation, Location endLocation, Expression object, String field) {
        // FIXME: ambiguity: field reference, interface dereference, component dereference?
        final FieldRef result = new FieldRef(startLocation, object, field);
        result.setEndLocation(endLocation);
        return result;
    }

    public static Expression makeOffsetof(Location startLocation, Location endLocation, AstType type,
                                          LinkedList<Word> fields) {
        // TODO: check if type is struct or union
        final Offsetof offsetof = new Offsetof(startLocation, type, fields);
        offsetof.setEndLocation(endLocation);
        return offsetof;
    }

    public static Cast makeCast(Location startLocation, Location endLocation, AstType type, Expression expression) {
        final Cast result = new Cast(startLocation, expression, type);
        result.setEndLocation(endLocation);
        return result;
    }

    /*
     * Unary.
     */

    public static Cast makeCast(Location startLocation, AstType astType, Expression exp) {
        final Cast result = new Cast(startLocation, exp, astType);
        result.setEndLocation(exp.getEndLocation());
        return result;
    }

    public static Dereference makeDereference(Location startLocation, Expression exp) {
        final Dereference result = new Dereference(startLocation, exp);
        result.setEndLocation(exp.getEndLocation());
        return result;
    }

    public static ExtensionExpr makeExtensionExpr(Location startLocation, Expression exp) {
        final ExtensionExpr result = new ExtensionExpr(startLocation, exp);
        result.setEndLocation(exp.getEndLocation());
        return result;
    }

    public static SizeofExpr makeSizeofExpr(Location startLocation, Expression exp) {
        final SizeofExpr result = new SizeofExpr(startLocation, exp);
        result.setEndLocation(exp.getEndLocation());
        return result;
    }

    public static AlignofExpr makeAlignofExpr(Location startLocation, Expression exp) {
        final AlignofExpr result = new AlignofExpr(startLocation, exp);
        result.setEndLocation(exp.getEndLocation());
        return result;
    }

    public static Realpart makeRealpart(Location startLocation, Expression exp) {
        final Realpart result = new Realpart(startLocation, exp);
        result.setEndLocation(exp.getEndLocation());
        return result;
    }

    public static Imagpart makeImagpart(Location startLocation, Expression exp) {
        final Imagpart result = new Imagpart(startLocation, exp);
        result.setEndLocation(exp.getEndLocation());
        return result;
    }

    public static AddressOf makeAddressOf(Location startLocation, Expression exp) {
        final AddressOf result = new AddressOf(startLocation, exp);
        result.setEndLocation(exp.getEndLocation());
        return result;
    }

    public static UnaryMinus makeUnaryMinus(Location startLocation, Expression exp) {
        final UnaryMinus result = new UnaryMinus(startLocation, exp);
        result.setEndLocation(exp.getEndLocation());
        return result;
    }

    public static UnaryPlus makeUnaryPlus(Location startLocation, Expression exp) {
        final UnaryPlus result = new UnaryPlus(startLocation, exp);
        result.setEndLocation(exp.getEndLocation());
        return result;
    }

    public static Bitnot makeBitnot(Location startLocation, Expression exp) {
        // FIXME: conjugate on complex types?
        final Bitnot result = new Bitnot(startLocation, exp);
        result.setEndLocation(exp.getEndLocation());
        return result;
    }

    public static Not makeNot(Location startLocation, Expression exp) {
        final Not result = new Not(startLocation, exp);
        result.setEndLocation(exp.getEndLocation());
        return result;
    }

    /*
     * Unary, increment/decrement
     */

    public static Predecrement makePredecrement(Location startLocation, Expression exp) {
        final Predecrement result = new Predecrement(startLocation, exp);
        result.setEndLocation(exp.getEndLocation());
        return result;
    }

    public static Preincrement makePreincrement(Location startLocation, Expression exp) {
        final Preincrement result = new Preincrement(startLocation, exp);
        result.setEndLocation(exp.getEndLocation());
        return result;
    }

    public static Postdecrement makePostdecrement(Location startLocation, Location endLocation, Expression exp) {
        final Postdecrement result = new Postdecrement(startLocation, exp);
        result.setEndLocation(endLocation);
        return result;
    }

    public static Postincrement makePostincrement(Location startLocation, Location endLocation, Expression exp) {
        final Postincrement result = new Postincrement(startLocation, exp);
        result.setEndLocation(endLocation);
        return result;
    }

    public static Unary makeUnary(Location startLocation, LeftUnaryOperation operation, Expression exp) {
        final Unary result;

        switch (operation) {
            case ADDRESS_OF:
                result = makeAddressOf(startLocation, exp);
                break;
            case UNARY_MINUS:
                result = makeUnaryMinus(startLocation, exp);
                break;
            case UNARY_PLUS:
                result = makeUnaryPlus(startLocation, exp);
                break;
            case PREINCREMENT:
                result = makePreincrement(startLocation, exp);
                break;
            case PREDECREMENT:
                result = makePredecrement(startLocation, exp);
                break;
            case BITNOT:
                result = makeBitnot(startLocation, exp);
                break;
            case NOT:
                result = makeNot(startLocation, exp);
                break;
            case REALPART:
                result = makeRealpart(startLocation, exp);
                break;
            case IMAGPART:
                result = makeImagpart(startLocation, exp);
                break;
            default:
                throw new IllegalArgumentException("unhandled unary operation value " + operation);
        }
        return result;
    }

    /*
     * Binary.
     */

    /*
     * Binary, arithmetic
     */

    public static Plus makePlus(Expression leftExpression, Expression rightExpression) {
        final Plus result = new Plus(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static Minus makeMinus(Expression leftExpression, Expression rightExpression) {
        final Minus result = new Minus(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static Times makeTimes(Expression leftExpression, Expression rightExpression) {
        final Times result = new Times(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static Divide makeDivide(Expression leftExpression, Expression rightExpression) {
        final Divide result = new Divide(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static Modulo makeModulo(Expression leftExpression, Expression rightExpression) {
        final Modulo result = new Modulo(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static Lshift makeLshift(Expression leftExpression, Expression rightExpression) {
        final Lshift result = new Lshift(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static Rshift makeRshift(Expression leftExpression, Expression rightExpression) {
        final Rshift result = new Rshift(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    /*
     * Binary, relational
     */

    public static Leq makeLeq(Expression leftExpression, Expression rightExpression) {
        final Leq result = new Leq(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static Geq makeGeq(Expression leftExpression, Expression rightExpression) {
        final Geq result = new Geq(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static Lt makeLt(Expression leftExpression, Expression rightExpression) {
        final Lt result = new Lt(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static Gt makeGt(Expression leftExpression, Expression rightExpression) {
        final Gt result = new Gt(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static Eq makeEq(Expression leftExpression, Expression rightExpression) {
        final Eq result = new Eq(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static Ne makeNe(Expression leftExpression, Expression rightExpression) {
        final Ne result = new Ne(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    /*
     * Binary, bit
     */

    public static Bitand makeBitand(Expression leftExpression, Expression rightExpression) {
        final Bitand result = new Bitand(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static Bitor makeBitor(Expression leftExpression, Expression rightExpression) {
        final Bitor result = new Bitor(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static Bitxor makeBitxor(Expression leftExpression, Expression rightExpression) {
        final Bitxor result = new Bitxor(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    /*
     * Binary, logical
     */

    public static Andand makeAndand(Expression leftExpression, Expression rightExpression) {
        final Andand result = new Andand(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static Oror makeOror(Expression leftExpression, Expression rightExpression) {
        final Oror result = new Oror(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    /*
     * Binary, assignment
     */

    public static Assign makeAssign(Expression leftExpression, Expression rightExpression) {
        final Assign result = new Assign(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static PlusAssign makePlusAssign(Expression leftExpression, Expression rightExpression) {
        final PlusAssign result = new PlusAssign(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static MinusAssign makeMinusAssign(Expression leftExpression, Expression rightExpression) {
        final MinusAssign result = new MinusAssign(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static TimesAssign makeTimesAssign(Expression leftExpression, Expression rightExpression) {
        final TimesAssign result = new TimesAssign(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static DivideAssign makeDivideAssign(Expression leftExpression, Expression rightExpression) {
        final DivideAssign result = new DivideAssign(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static ModuloAssign makeModuloAssign(Expression leftExpression, Expression rightExpression) {
        final ModuloAssign result = new ModuloAssign(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static LshiftAssign makeLshiftAssign(Expression leftExpression, Expression rightExpression) {
        final LshiftAssign result = new LshiftAssign(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static RshiftAssign makeRshiftAssign(Expression leftExpression, Expression rightExpression) {
        final RshiftAssign result = new RshiftAssign(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static BitandAssign makeBitandAssign(Expression leftExpression, Expression rightExpression) {
        final BitandAssign result = new BitandAssign(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static BitorAssign makeBitorAssign(Expression leftExpression, Expression rightExpression) {
        final BitorAssign result = new BitorAssign(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static BitxorAssign makeBitxorAssign(Expression leftExpression, Expression rightExpression) {
        final BitxorAssign result = new BitxorAssign(leftExpression.getLocation(), leftExpression, rightExpression);
        result.setEndLocation(rightExpression.getEndLocation());
        return result;
    }

    public static CharacterCst makeCharacterCst(String literalStr, Location startLocation, Location endLocation) {
        Optional<Character> value = Optional.absent();

        if (SIMPLE_ESCAPE_SEQUENCES.containsKey(literalStr)) {
            // Simple escape sequence
            value = Optional.of(SIMPLE_ESCAPE_SEQUENCES.get(literalStr));
        } else if (literalStr.matches(OCTAL_ESCAPE_SEQUENCE_REGEXP)) {
            // Octal escape sequence
            final int codePoint = Integer.parseInt(literalStr.substring(1), 8);
            final char[] chars = Character.toChars(codePoint);
            assert chars.length == 1 : "a character literal cannot be represented by a single char value";
            value = Optional.of(chars[0]);
        } else if (literalStr.matches(HEXADECIMAL_ESCAPE_SEQUENCE_REGEXP)) {
            // Hexadecimal escape sequence
            final int codePoint = Integer.parseInt(literalStr.substring(2), 16);
            final char[] chars = Character.toChars(codePoint);
            assert chars.length == 1 : "a character literal cannot be represented by a single char value";
            value = Optional.of(chars[0]);
        } else if (literalStr.length() == 1) {
            value = Optional.of(literalStr.charAt(0));
        }

        // Prepare and return the result
        final CharacterCst result = new CharacterCst(startLocation, literalStr, value);
        result.setEndLocation(endLocation);
        return result;
    }

    public static IntegerCst makeIntegerCst(String value, Location startLocation, Location endLocation) {
        final String withoutRedundantZeroes = removeRedundantLeadingZeros(value);
        final IntegerLiteralDecomposition decomposition = new IntegerLiteralDecomposition(withoutRedundantZeroes);
        final IntegerCstKind kind = determineIntegerCstKind(decomposition.core);
        final IntegerCstSuffix suffix = determineIntegerCstSuffix(decomposition.suffix);

        // Check and determine the value of the constant
        Optional<BigInteger> intValue = Optional.absent();
        if (decomposition.core.length() <= MAX_INTEGER_CST_CHARS_COUNT) {
            final String forParsing =   kind == IntegerCstKind.HEXADECIMAL
                                      ? decomposition.core.substring(2)
                                      : decomposition.core;
            final BigInteger parsed = new BigInteger(forParsing, kind.getRadix());
            if (parsed.compareTo(MAX_INTEGER_CST_VALUE) <= 0) {
                intValue = Optional.of(parsed);
            }
        }

        // Prepare and return the result
        final IntegerCst result = new IntegerCst(startLocation, value, intValue,
                kind, suffix);
        result.setEndLocation(endLocation);
        return result;
    }

    /**
     * Compress all leading zeroes in the given string to exactly one for an
     * octal or decimal literal. For a hexadecimal literal all the leading
     * zeroes after the prefix "0x" or "0X" are compressed.
     *
     * @param value String to transform.
     * @return Given integer literal with all redundant zeroes removed.
     */
    private static String removeRedundantLeadingZeros(String value) {
        // Check if the constant is hexadecimal
        final StringBuilder result = new StringBuilder();
        final Matcher hexMatcher = Pattern.compile("^(0x|0X)(.*)$")
                .matcher(value);
        if (hexMatcher.matches()) {
            result.append(hexMatcher.group(1));
            value = hexMatcher.group(2);
        }

        // Remove redundant zeroes
        int firstIndex = 0;
        for (int i = 0; i < value.length(); ++i) {
            if (value.charAt(i) != '0') {
                if (i > 0) {
                    firstIndex = i - 1;
                }
                break;
            }
        }
        result.append(value.substring(firstIndex));

        return result.toString();
    }

    private static IntegerCstKind determineIntegerCstKind(String withoutRedundantZeroes) {
        if (withoutRedundantZeroes.startsWith("0x") || withoutRedundantZeroes.startsWith("0X")) {
            return IntegerCstKind.HEXADECIMAL;
        } else if (withoutRedundantZeroes.startsWith("0")) {
            return IntegerCstKind.OCTAL;
        } else {
            return IntegerCstKind.DECIMAL;
        }
    }

    private static IntegerCstSuffix determineIntegerCstSuffix(String suffix) {
        // Sort the characters from the suffix
        final char[] characters = suffix.toUpperCase().toCharArray();
        Arrays.sort(characters);
        final String forCheck = String.copyValueOf(characters);

        // Check the suffix
        switch (forCheck) {
            case "LLU":
                return IntegerCstSuffix.SUFFIX_ULL;
            case "LU":
                return IntegerCstSuffix.SUFFIX_UL;
            case "LL":
                return IntegerCstSuffix.SUFFIX_LL;
            case "L":
                return IntegerCstSuffix.SUFFIX_L;
            case "U":
                return IntegerCstSuffix.SUFFIX_U;
            case "":
                return IntegerCstSuffix.NO_SUFFIX;
            default:
                throw new RuntimeException("unexpected integer literal suffix '" + suffix + "'");
        }
    }

    /**
     * Class whose objects are responsible for decomposing an integer literal to
     * its core and suffix. The part of the literal before its suffix is
     * considered its core.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class IntegerLiteralDecomposition {
        private final String core;
        private final String suffix;

        private IntegerLiteralDecomposition(String literal) {
            checkNotNull(literal, "literal cannot be null");

            /* We assume that the given literal is correct - it has been
               previously checked by the lexer. */
            final Matcher literalMatcher = Pattern
                    .compile("^(?<core>(0x|0X)?[0-9A-Fa-f]+)(?<suffix>.*)$")
                    .matcher(literal);
            final boolean matchSuccessful = literalMatcher.matches();
            checkArgument(matchSuccessful, "invalid integer literal '%s'", literal);

            this.core = literalMatcher.group("core");
            this.suffix = literalMatcher.group("suffix");
        }
    }

    private Expressions() {
    }
}

