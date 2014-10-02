package pl.edu.mimuw.nesc.analysis;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.math.BigInteger;
import pl.edu.mimuw.nesc.ast.IntegerCstKind;
import pl.edu.mimuw.nesc.ast.IntegerCstSuffix;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.ast.type.*;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectKind;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.ast.type.TypeUtils.*;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.*;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.BinaryOp.*;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.UnaryOp.*;

/**
 * Class that is responsible for analysis of expressions.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class ExpressionsAnalysis extends ExceptionVisitor<Optional<ExprData>, Void> {
    /**
     * Type that is used as <code>size_t</code> type.
     */
    private static final UnsignedIntegerType TYPE_SIZE_T = new UnsignedLongType();

    /**
     * Type that is used as <code>ptrdiff_t</code> type.
     */
    private static final SignedIntegerType TYPE_PTRDIFF_T = new LongType();

    /**
     * Types of decimal constants for their suffixes.
     */
    private static final ImmutableMap<IntegerCstSuffix, ImmutableList<IntegerType>> DECIMAL_CONSTANTS_TYPES;
    static {
        ImmutableMap.Builder<IntegerCstSuffix, ImmutableList<IntegerType>> builder = ImmutableMap.builder();
        builder.put(IntegerCstSuffix.NO_SUFFIX, ImmutableList.<IntegerType>of(
                new IntType(),
                new LongType(),
                new LongLongType())
        );
        builder.put(IntegerCstSuffix.SUFFIX_U, ImmutableList.<IntegerType>of(
                new UnsignedIntType(),
                new UnsignedLongType(),
                new UnsignedLongLongType())
        );
        builder.put(IntegerCstSuffix.SUFFIX_L, ImmutableList.<IntegerType>of(
                new LongType(),
                new LongLongType()
        ));
        builder.put(IntegerCstSuffix.SUFFIX_UL, ImmutableList.<IntegerType>of(
                new UnsignedLongType(),
                new UnsignedLongLongType()
        ));
        builder.put(IntegerCstSuffix.SUFFIX_LL, ImmutableList.<IntegerType>of(
                new LongLongType()
        ));
        builder.put(IntegerCstSuffix.SUFFIX_ULL, ImmutableList.<IntegerType>of(
                new UnsignedLongLongType()
        ));
        DECIMAL_CONSTANTS_TYPES = Maps.immutableEnumMap(builder.build());
    }

    /**
     * Types of octal and hexadecimal constants for their suffixes.
     */
    private static final ImmutableMap<IntegerCstSuffix, ImmutableList<IntegerType>> OCTAL_AND_HEX_CONSTANTS_TYPES;
    static {
        ImmutableMap.Builder<IntegerCstSuffix, ImmutableList<IntegerType>> builder = ImmutableMap.builder();
        builder.put(IntegerCstSuffix.NO_SUFFIX, ImmutableList.<IntegerType>of(
                new IntType(),
                new UnsignedIntType(),
                new LongType(),
                new UnsignedLongType(),
                new LongLongType(),
                new UnsignedLongLongType()
        ));
        builder.put(IntegerCstSuffix.SUFFIX_U, ImmutableList.<IntegerType>of(
                new UnsignedIntType(),
                new UnsignedLongType(),
                new UnsignedLongLongType())
        );
        builder.put(IntegerCstSuffix.SUFFIX_L, ImmutableList.<IntegerType>of(
                new LongType(),
                new UnsignedLongType(),
                new LongLongType(),
                new UnsignedLongLongType()
        ));
        builder.put(IntegerCstSuffix.SUFFIX_UL, ImmutableList.<IntegerType>of(
                new UnsignedLongType(),
                new UnsignedLongLongType()
        ));
        builder.put(IntegerCstSuffix.SUFFIX_LL, ImmutableList.<IntegerType>of(
                new LongLongType(),
                new UnsignedLongLongType()
        ));
        builder.put(IntegerCstSuffix.SUFFIX_ULL, ImmutableList.<IntegerType>of(
                new UnsignedLongLongType()
        ));
        OCTAL_AND_HEX_CONSTANTS_TYPES = Maps.immutableEnumMap(builder.build());
    }

    /**
     * Environment of the expression that is analyzed.
     */
    private final Environment environment;

    /**
     * Object that will be notified about detected problems.
     */
    private final ErrorHelper errorHelper;

    /**
     * Analyze the given expression and report all detected errors to the given
     * error helper.
     *
     * @param expr Expression to be analyzed.
     * @param environment Environment of the expression.
     * @param errorHelper Object that will be notified about detected problems.
     * @return Data about the given expression. The object is present if and
     *         only if the expression is valid, otherwise it is absent.
     */
    public static Optional<ExprData> analyze(Expression expr, Environment environment,
            ErrorHelper errorHelper) {
        checkNotNull(expr, "expression cannot be null");
        checkNotNull(environment, "environment cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        final ExpressionsAnalysis analysisVisitor = new ExpressionsAnalysis(environment, errorHelper);
        return expr.accept(analysisVisitor, null);
    }

    /**
     * Private constructor to prevent this class from being instantiated.
     *
     * @param environment Environment of the analyzed expression.
     * @param errorHelper Object that will be notified about detected problems.
     */
    private ExpressionsAnalysis(Environment environment, ErrorHelper errorHelper) {
        this.environment = environment;
        this.errorHelper = errorHelper;
    }

    @Override
    public Optional<ExprData> visitPlus(Plus expr, Void arg) {
        final BinaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        // Perform operations
        cr.leftData.superDecay();
        cr.rightData.superDecay();

        // Check types and simultaneously determine the result type
        Optional<? extends Type> resultType = Optional.absent();
        if (cr.leftType().isArithmetic() && cr.rightType().isArithmetic()) {

            final Type afterUAC = doUsualArithmeticConversions((ArithmeticType) cr.leftType(),
                                                               (ArithmeticType) cr.rightType());
            resultType = Optional.of(afterUAC);

        } else if (cr.leftType().isPointerType() || cr.rightType().isPointerType()) {
            final PointerType ptrType = cr.leftType().isPointerType()
                    ? (PointerType) cr.leftType()
                    : (PointerType) cr.rightType();
            final Type otherType = ptrType == cr.leftType()
                    ? cr.rightType()
                    : cr.leftType();
            final Type referencedType = ptrType.getReferencedType();

            if (referencedType.isComplete() && referencedType.isObjectType()
                    && otherType.isIntegerType()) {
                resultType = Optional.of(ptrType);
            }
        }

        if (!resultType.isPresent()) {
            final ErroneousIssue error = new InvalidPlusExprError(cr.leftType(), expr.getLeftArgument(),
                    cr.rightType(), expr.getRightArgument());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(resultType.get())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(cr.leftData.isNullPointerConstant()
                        && cr.rightData.isNullPointerConstant())
                .build();

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitMinus(Minus expr, Void arg) {
        final BinaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.leftData.superDecay();
        cr.rightData.superDecay();

        /* Check if types are correct and simultaneously determine the result
           type */
        Optional<? extends Type> resultType = Optional.absent();
        if (cr.leftType().isArithmetic() && cr.rightType().isArithmetic()) {

            resultType = Optional.of(doUsualArithmeticConversions((ArithmeticType) cr.leftType(),
                    (ArithmeticType) cr.rightType()));

        } else if (cr.leftType().isPointerType() && cr.rightType().isPointerType()) {

            final PointerType ptrType1 = (PointerType) cr.leftType(),
                              ptrType2 = (PointerType) cr.rightType();
            final Type refType1 = ptrType1.getReferencedType().removeQualifiers(),
                       refType2 = ptrType2.getReferencedType().removeQualifiers();

            if (refType1.isComplete() && refType1.isObjectType() && refType2.isComplete()
                    && refType2.isObjectType() && refType1.isCompatibleWith(refType2)) {
                resultType = Optional.of(TYPE_PTRDIFF_T);
            }
        } else if (cr.leftType().isPointerType() && cr.rightType().isIntegerType()) {

            final PointerType ptrType = (PointerType) cr.leftType();
            final Type refType = ptrType.getReferencedType();

            if (refType.isComplete() && refType.isObjectType()) {
                resultType = Optional.of(cr.leftType());
            }
        }

        // If the result type is absent, types of operands are erroneous
        if (!resultType.isPresent()) {
            final ErroneousIssue error = new InvalidMinusExprError(cr.leftType(),
                    expr.getLeftArgument(), cr.rightType(), expr.getRightArgument());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(resultType.get())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(cr.leftData.isNullPointerConstant()
                                  && cr.rightData.isNullPointerConstant())
                .build();

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitTimes(Times expr, Void arg) {
        return analyzeMultiplicativeExpr(expr, TIMES);
    }

    @Override
    public Optional<ExprData> visitDivide(Divide expr, Void arg) {
        return analyzeMultiplicativeExpr(expr, DIVIDE);
    }

    @Override
    public Optional<ExprData> visitModulo(Modulo expr, Void arg) {
        return analyzeMultiplicativeExpr(expr, MODULO);
    }

    @Override
    public Optional<ExprData> visitLshift(Lshift expr, Void arg) {
        return analyzeShiftExpr(expr, LSHIFT);
    }

    @Override
    public Optional<ExprData> visitRshift(Rshift expr, Void arg) {
        return analyzeShiftExpr(expr, RSHIFT);
    }

    @Override
    public Optional<ExprData> visitLeq(Leq expr, Void arg) {
        return analyzeCompareExpr(expr, LEQ);
    }

    @Override
    public Optional<ExprData> visitGeq(Geq expr, Void arg) {
        return analyzeCompareExpr(expr, GEQ);
    }

    @Override
    public Optional<ExprData> visitLt(Lt expr, Void arg) {
        return analyzeCompareExpr(expr, LT);
    }

    @Override
    public Optional<ExprData> visitGt(Gt expr, Void arg) {
        return analyzeCompareExpr(expr, GT);
    }

    @Override
    public Optional<ExprData> visitEq(Eq expr, Void arg) {
        return analyzeEqualityExpr(expr, EQ);
    }

    @Override
    public Optional<ExprData> visitNe(Ne expr, Void arg) {
        return analyzeEqualityExpr(expr, NE);
    }

    @Override
    public Optional<ExprData> visitBitand(Bitand expr, Void arg) {
        return analyzeBinaryBitExpr(expr, BITAND);
    }

    @Override
    public Optional<ExprData> visitBitor(Bitor expr, Void arg) {
        return analyzeBinaryBitExpr(expr, BITOR);
    }

    @Override
    public Optional<ExprData> visitBitxor(Bitxor expr, Void arg) {
        return analyzeBinaryBitExpr(expr, BITXOR);
    }

    @Override
    public Optional<ExprData> visitAndand(Andand expr, Void arg) {
        return analyzeBinaryLogicalExpr(expr, ANDAND);
    }

    @Override
    public Optional<ExprData> visitOror(Oror expr, Void arg) {
        return analyzeBinaryLogicalExpr(expr, OROR);
    }

    @Override
    public Optional<ExprData> visitAssign(Assign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitPlusAssign(PlusAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitMinusAssign(MinusAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitTimesAssign(TimesAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitDivideAssign(DivideAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitModuloAssign(ModuloAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitLshiftAssign(LshiftAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitRshiftAssign(RshiftAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitBitandAssign(BitandAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitBitorAssign(BitorAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitBitxorAssign(BitxorAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitDereference(Dereference expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitAddressOf(AddressOf expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitUnaryPlus(UnaryPlus expr, Void arg) {
        return analyzeUnaryAdditiveExpr(expr, UNARY_PLUS);
    }

    @Override
    public Optional<ExprData> visitUnaryMinus(UnaryMinus expr, Void arg) {
        return analyzeUnaryAdditiveExpr(expr, UNARY_MINUS);
    }

    @Override
    public Optional<ExprData> visitBitnot(Bitnot expr, Void arg) {
        final UnaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.argData.superDecay();

        if (!cr.argType().isIntegerType()) {
            final ErroneousIssue error = new InvalidBitnotExprError(cr.argType(), expr.getArgument());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(cr.argType().promote())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build();

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitNot(Not expr, Void arg) {
        final UnaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.argData.superDecay();

        if (!cr.argType().isScalarType()) {
            final ErroneousIssue error = new InvalidNotExprError(cr.argType(),
                    expr.getArgument());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(new IntType())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build();

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitAlignofType(AlignofType expr, Void arg) {
        return analyzeTypeQueryExpr(expr, expr.getAsttype().getType(), OP_ALIGNOF);
    }

    @Override
    public Optional<ExprData> visitSizeofType(SizeofType expr, Void arg) {
        return analyzeTypeQueryExpr(expr, expr.getAsttype().getType(), OP_SIZEOF);
    }

    @Override
    public Optional<ExprData> visitSizeofExpr(SizeofExpr expr, Void arg) {
        return analyzeExprQueryExpr(expr, OP_SIZEOF);
    }

    @Override
    public Optional<ExprData> visitAlignofExpr(AlignofExpr expr, Void arg) {
        return analyzeExprQueryExpr(expr, OP_ALIGNOF);
    }

    @Override
    public Optional<ExprData> visitRealpart(Realpart expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitImagpart(Imagpart expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitArrayRef(ArrayRef expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitErrorExpr(ErrorExpr expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitComma(Comma expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitLabelAddress(LabelAddress expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitConditional(Conditional expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitIdentifier(Identifier expr, Void arg) {
        final ExprData.Builder dataBuilder = ExprData.builder()
                .isBitField(false)
                .isNullPointerConstant(false);

        final Optional<? extends ObjectDeclaration> objDecl =
                environment.getObjects().get(expr.getName());

        // Emit error if the identifier is undeclared
        if (!objDecl.isPresent()) {
            errorHelper.error(expr.getLocation(), expr.getEndLocation(),
                    new UndeclaredIdentifierError(expr.getName()));
            return Optional.absent();
        }

        // Get necessary information from the declaration object
        final ObjectDeclaration pureObjDecl = objDecl.get();
        final ObjectKind objKind = pureObjDecl.getKind();
        final Optional<Type> objType = pureObjDecl.getType();

        // Emit error if an invalid type of object is referred
        if (objKind != ObjectKind.VARIABLE && objKind != ObjectKind.FUNCTION
                && objKind != ObjectKind.CONSTANT) {
            errorHelper.error(expr.getLocation(), expr.getEndLocation(),
                    new InvalidIdentifierUsageError(expr.getName()));
            return Optional.absent();
        }

        if (objType.isPresent()) {
            final ExprData result = dataBuilder
                    .isLvalue(pureObjDecl.getKind() == ObjectKind.VARIABLE)
                    .type(objType.get())
                    .build();
            return Optional.of(result);
        } else {
            return Optional.absent();
        }
    }

    @Override
    public Optional<ExprData> visitCompoundExpr(CompoundExpr expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitIntegerCst(IntegerCst expr, Void arg) {
        final Optional<BigInteger> value = expr.getValue();
        final boolean isNullPtrCst = value.isPresent()
                && value.get().equals(BigInteger.ZERO);

        final ExprData.Builder dataBuilder = ExprData.builder()
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(isNullPtrCst);

        // Determine type
        Optional<? extends Type> type = Optional.absent();
        if (value.isPresent()) {
            final ImmutableMap<IntegerCstSuffix, ImmutableList<IntegerType>> map =
                              expr.getKind() == IntegerCstKind.DECIMAL
                            ? DECIMAL_CONSTANTS_TYPES
                            : OCTAL_AND_HEX_CONSTANTS_TYPES;
            final ImmutableList<IntegerType> typesList = map.get(expr.getSuffix());

            for (IntegerType possibleType : typesList) {
                if (possibleType.getRange().contains(value.get())) {
                    type = Optional.of(possibleType);
                    break;
                }
            }
        }

        // Emit error if the constant has no type and return absent value
        if (type.isPresent()) {
            return Optional.of(dataBuilder.type(type.get()).build());
        } else {
            errorHelper.error(expr.getLocation(), expr.getEndLocation(),
                    new IntegerConstantOverflowError(expr));
            return Optional.absent();
        }
    }

    @Override
    public Optional<ExprData> visitFloatingCst(FloatingCst expr, Void arg) {
        final ExprData.Builder dataBuilder = ExprData.builder()
                .isBitField(false)
                .isLvalue(false)
                .isNullPointerConstant(false);

        final String literal = expr.getString();
        final char lastCharacter = literal.charAt(literal.length() - 1);

        switch(lastCharacter) {
            case 'l':
            case 'L':
                dataBuilder.type(new LongDoubleType());
                break;
            case 'f':
            case 'F':
                dataBuilder.type(new FloatType());
                break;
            default:
                dataBuilder.type(new DoubleType());
                break;
        }

        return Optional.of(dataBuilder.build());
    }

    @Override
    public Optional<ExprData> visitCharacterCst(CharacterCst expr, Void arg) {
        final Optional<Character> charValue = expr.getValue();

        final ExprData result = ExprData.builder()
                .type(new IntType())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(charValue.isPresent() && charValue.get() == '\0')
                .build();

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitStringCst(StringCst expr, Void arg) {
        final ExprData result = ExprData.builder()
                .type(new ArrayType(new CharType(), true))
                .isLvalue(true)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build();

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitStringAst(StringAst expr, Void arg) {
        final ExprData result = ExprData.builder()
                .type(new ArrayType(new CharType(), true))
                .isLvalue(true)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build();

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitFunctionCall(FunctionCall expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitFieldRef(FieldRef expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitInterfaceDeref(InterfaceDeref expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitComponentDeref(ComponentDeref expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitPreincrement(Preincrement expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitPredecrement(Predecrement expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitPostincrement(Postincrement expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitPostdecrement(Postdecrement expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitCast(Cast expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitCastList(CastList expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitInitList(InitList expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitInitSpecific(InitSpecific expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitTypeArgument(TypeArgument expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitGenericCall(GenericCall expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitExtensionExpr(ExtensionExpr expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitConjugate(Conjugate expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    /**
     * Analysis for operators <code>&lt;&lt;</code> and <code>&gt;&gt;</code>.
     */
    private Optional<ExprData> analyzeShiftExpr(Binary shiftExpr, BinaryOp op) {
        final BinaryExprDataCarrier cr = analyzeSubexpressions(shiftExpr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.leftData.superDecay();
        cr.rightData.superDecay();

        // Check conditions
        if (!checkShiftOpsTypes(cr.leftType(), cr.rightType())) {
            final ErroneousIssue error = new InvalidShiftExprOperandsError(cr.leftType(),
                    shiftExpr.getLeftArgument(), op, cr.rightType(),
                    shiftExpr.getRightArgument());
            errorHelper.error(shiftExpr.getLocation(), shiftExpr.getEndLocation(), error);
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(cr.leftType().promote())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(cr.leftData.isNullPointerConstant()
                                  && cr.rightData.isNullPointerConstant())
                .build();

        return Optional.of(result);
    }

    /**
     * Analysis for operators <code>sizeof</code> and <code>_Alignof</code>
     * applied to typenames.
     */
    private Optional<ExprData> analyzeTypeQueryExpr(Expression typeQueryExpr,
            Optional<Type> arg, String op) {
        if (!arg.isPresent()) {
            return Optional.absent();
        }

        // Check the type
        final Type type = arg.get();
        if (type.isFunctionType() || !type.isComplete()) {
            errorHelper.error(typeQueryExpr.getLocation(), typeQueryExpr.getEndLocation(),
                              new InvalidTypeQueryExprError(type, op));
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(TYPE_SIZE_T)
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build();

        return Optional.of(result);
    }

    /**
     * Analysis for operators <code>sizeof</code> and <code>_Alignof</code>
     * applied to expressions.
     */
    private Optional<ExprData> analyzeExprQueryExpr(Unary unary, String op) {
        final UnaryExprDataCarrier cr = analyzeSubexpressions(unary);
        if (!cr.good) {
            return Optional.absent();
        }

        // Check the argument
        if (cr.argType().isFunctionType() || !cr.argType().isComplete() || cr.argData.isBitField()) {

            final ErroneousIssue error = new InvalidExprQueryExprError(unary.getArgument(),
                    cr.argType(), cr.argData.isBitField(), op);
            errorHelper.error(unary.getLocation(), unary.getEndLocation(), error);
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(TYPE_SIZE_T)
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build();

        return Optional.of(result);
    }

    /**
     * Analysis for operators <code>*</code>, <code>/</code> and <code>%</code>.
     */
    private Optional<ExprData> analyzeMultiplicativeExpr(Binary expr, BinaryOp op) {
        final BinaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.leftData.superDecay();
        cr.rightData.superDecay();

        // Check type
        if (!checkMultiplicativeOpsTypes(cr.leftType(), cr.rightType(), op)) {
            final ErroneousIssue error = new InvalidMultiplicativeExprError(cr.leftType(),
                    expr.getLeftArgument(), op, cr.rightType(), expr.getRightArgument());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final Type resultType = doUsualArithmeticConversions((ArithmeticType) cr.leftType(),
                (ArithmeticType) cr.rightType());
        final boolean isNullPtrCst = resultType.isIntegerType()
                && (op != TIMES || cr.leftData.isNullPointerConstant() || cr.rightData.isNullPointerConstant())
                && (op != MODULO && op != DIVIDE || cr.leftData.isNullPointerConstant());

        final ExprData result = ExprData.builder()
                .type(resultType)
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(isNullPtrCst)
                .build();

        return Optional.of(result);
    }

    /**
     * Analysis for unary operators <code>+</code> and <code>-</code>.
     */
    private Optional<ExprData> analyzeUnaryAdditiveExpr(Unary expr, UnaryOp op) {
        final UnaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.argData.superDecay();

        if (!cr.argType().isArithmetic()) {
            final ErroneousIssue error = new InvalidUnaryAdditiveExprError(op, cr.argType(),
                    expr.getArgument());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(cr.argType().promote())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(cr.argData.isNullPointerConstant())
                .build();

        return Optional.of(result);
    }

    /**
     * Analysis for binary operators <code>&lt;=</code>, <code>&gt;=</code>,
     * <code>&lt;</code> and <code>&gt;</code>.
     */
    private Optional<ExprData> analyzeCompareExpr(Binary expr, BinaryOp op) {
        final BinaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.leftData.superDecay();
        cr.rightData.superDecay();

        boolean correct = false;
        if (cr.leftType().isRealType() && cr.rightType().isRealType()) {
            correct = true;
        } else if (cr.leftType().isPointerType() && cr.rightType().isPointerType()) {

            final PointerType leftPtrType = (PointerType) cr.leftType(),
                              rightPtrType = (PointerType) cr.rightType();
            final Type leftRefType = leftPtrType.getReferencedType().removeQualifiers(),
                       rightRefType = rightPtrType.getReferencedType().removeQualifiers();

            correct = leftRefType.isObjectType() && rightRefType.isObjectType()
                      && leftRefType.isCompatibleWith(rightRefType);
        }

        if (!correct) {
            final ErroneousIssue error = new InvalidCompareExprError(cr.leftType(),
                    expr.getLeftArgument(), op, cr.rightType(), expr.getRightArgument());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(new IntType())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build();

        return Optional.of(result);
    }

    /**
     * Analysis for binary operators <code>==</code> and <code>!=</code>.
     */
    private Optional<ExprData> analyzeEqualityExpr(Binary expr, BinaryOp op) {
        final BinaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.leftData.superDecay();
        cr.rightData.superDecay();

        // Consider the cases when the type is correct
        boolean correct = false, missingCastWarning = false;
        if (cr.leftType().isArithmetic() && cr.rightType().isArithmetic()) {
            correct = true;
        } else if (cr.leftType().isPointerType() && cr.rightType().isPointerType()) {

            final PointerType leftPtrType = (PointerType) cr.leftType(),
                              rightPtrType = (PointerType) cr.rightType();
            final Type leftRefType = leftPtrType.getReferencedType().removeQualifiers(),
                       rightRefType = rightPtrType.getReferencedType().removeQualifiers();

            correct = leftRefType.isCompatibleWith(rightRefType)
                      || leftRefType.isObjectType() && rightRefType.isObjectType()
                           && (leftRefType.isVoid() || rightRefType.isVoid());
        } else if (cr.leftType().isPointerType() && cr.rightType().isIntegerType()) {
            correct = true;
            missingCastWarning = !cr.rightData.isNullPointerConstant();
        } else if (cr.leftType().isIntegerType() && cr.rightType().isPointerType()) {
            correct = true;
            missingCastWarning = !cr.leftData.isNullPointerConstant();
        }

        if (!correct) {
            final ErroneousIssue error = new InvalidEqualityExprError(cr.leftType(),
                    expr.getLeftArgument(), op, cr.rightType(), expr.getRightArgument());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();

        } else if (missingCastWarning) {

            final CautionaryIssue warning = new InvalidPointerComparisonWarning(cr.leftType(),
                    expr.getLeftArgument(), op, cr.rightType(), expr.getRightArgument());
            errorHelper.warning(expr.getLocation(), expr.getEndLocation(), warning);
        }

        final ExprData result = ExprData.builder()
                .type(new IntType())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build();

        return Optional.of(result);
    }

    /**
     * Analysis for binary operators <code>&amp;&amp;</code> and <code>||</code>.
     */
    private Optional<ExprData> analyzeBinaryLogicalExpr(Binary expr, BinaryOp op) {
        final BinaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.leftData.superDecay();
        cr.rightData.superDecay();

        if (!cr.leftType().isScalarType() || !cr.rightType().isScalarType()) {
            final ErroneousIssue error = new InvalidBinaryLogicalExprError(cr.leftType(),
                    expr.getLeftArgument(), op, cr.rightType(), expr.getRightArgument());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(new IntType())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build();

        return Optional.of(result);
    }

    /**
     * Analysis for operators <code>&amp;</code>, <code>|</code> and <code>^</code>.
     */
    private Optional<ExprData> analyzeBinaryBitExpr(Binary expr, BinaryOp op) {
        final BinaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.leftData.superDecay();
        cr.rightData.superDecay();

        if (!checkBinaryBitOpsTypes(cr.leftType(), cr.rightType())) {
            final ErroneousIssue error = new InvalidBinaryBitExprError(cr.leftType(),
                    expr.getLeftArgument(), op, cr.rightType(), expr.getRightArgument());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final Type resultType = doUsualArithmeticConversions((ArithmeticType) cr.leftType(),
                (ArithmeticType) cr.rightType());
        final boolean isNullPtrCst =
                (op != BITAND || cr.leftData.isNullPointerConstant()
                        || cr.rightData.isNullPointerConstant())
                && (op != BITXOR && op != BITOR || cr.leftData.isNullPointerConstant()
                        && cr.rightData.isNullPointerConstant());

        final ExprData result = ExprData.builder()
                .type(resultType)
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(isNullPtrCst)
                .build();

        return Optional.of(result);
    }

    /**
     * @return <code>true</code> if and only if both types are correct for the
     *         operands of the given multiplicative operator.
     */
    private boolean checkMultiplicativeOpsTypes(Type leftType, Type rightType, BinaryOp op) {
        switch(op) {
            case TIMES:
            case DIVIDE:
                return leftType.isArithmetic() && rightType.isArithmetic();

            case MODULO:
                return leftType.isIntegerType() && rightType.isIntegerType();

            default:
                throw new RuntimeException("invalid multiplicative operator '" + op + "'");
        }
    }

    /**
     * @return <code>true</code> if and only if both types are correct for the
     *         operands of a shift operator.
     */
    private boolean checkShiftOpsTypes(Type leftType, Type rightType) {
        return leftType.isIntegerType() && rightType.isIntegerType();
    }

    /**
     * @return <code>true</code> if and only if both types are correct for the
     *         operands of a binary bit operator (other than a shift operator).
     */
    private boolean checkBinaryBitOpsTypes(Type leftType, Type rightType) {
        return leftType.isIntegerType() && rightType.isIntegerType();
    }

    /**
     * Helper method to clarify code.
     *
     * @return Newly created carrier object with information about
     *         subexpressions of given expression.
     * @throws NullPointerException Given argument is null.
     */
    private BinaryExprDataCarrier analyzeSubexpressions(Binary binary) {
        return new BinaryExprDataCarrier(binary);
    }

    /**
     * As {@link ExpressionsAnalysis#analyzeSubexpressions(Binary)} but for
     * unary expressions.
     *
     * @return Newly created carrier object with information about the
     *         subexpression of the given unary expression.
     * @throws NullPointerException Given argument is null.
     */
    private UnaryExprDataCarrier analyzeSubexpressions(Unary unary) {
        return new UnaryExprDataCarrier(unary);
    }

    /**
     * A simple helper class to facilitate analysis of binary expressions.
     * Reference fields are not null if and only if <code>good</code> is
     * <code>true</code>.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class BinaryExprDataCarrier {
        private final ExprData leftData;
        private final ExprData rightData;
        private final boolean good;

        private BinaryExprDataCarrier(Binary expr) {
            checkNotNull(expr, "the binary expression cannot be null");

            final Optional<ExprData> oLeftData =
                    expr.getLeftArgument().accept(ExpressionsAnalysis.this, null);

            final Optional<ExprData> oRightData =
                    expr.getRightArgument().accept(ExpressionsAnalysis.this, null);

            this.good = oLeftData.isPresent() && oRightData.isPresent();

            if (this.good) {
                this.leftData = oLeftData.get();
                this.rightData = oRightData.get();
            } else {
                this.leftData = this.rightData = null;
            }
        }

        private Type leftType() {
            return leftData.getType();
        }

        private Type rightType() {
            return rightData.getType();
        }
    }

    /**
     * Simple helper class for for analysis of subexpressions of unary
     * expressions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class UnaryExprDataCarrier {
        private final ExprData argData;
        private final boolean good;

        private UnaryExprDataCarrier(Unary expr) {
            checkNotNull(expr, "the unary expression cannot be null");

            final Optional<ExprData> oArgData =
                    expr.getArgument().accept(ExpressionsAnalysis.this, null);

            this.good = oArgData.isPresent();
            this.argData = oArgData.orNull();
        }

        private Type argType() {
            return argData.getType();
        }
    }
}
