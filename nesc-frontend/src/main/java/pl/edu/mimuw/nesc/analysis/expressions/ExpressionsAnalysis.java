package pl.edu.mimuw.nesc.analysis.expressions;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.ast.IntegerCstKind;
import pl.edu.mimuw.nesc.ast.IntegerCstSuffix;
import pl.edu.mimuw.nesc.ast.NescCallKind;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.type.*;
import pl.edu.mimuw.nesc.declaration.object.*;
import pl.edu.mimuw.nesc.declaration.object.unique.UniqueCountDeclaration;
import pl.edu.mimuw.nesc.declaration.object.unique.UniqueDeclaration;
import pl.edu.mimuw.nesc.declaration.object.unique.UniqueNDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.*;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.type.TypeUtils.*;
import static pl.edu.mimuw.nesc.astwriting.Tokens.*;
import static pl.edu.mimuw.nesc.astwriting.Tokens.BinaryOp.*;
import static pl.edu.mimuw.nesc.astwriting.Tokens.ConstantFun.*;
import static pl.edu.mimuw.nesc.astwriting.Tokens.UnaryOp.*;
import static pl.edu.mimuw.nesc.problem.issue.InvalidParameterTypeError.FunctionKind;
import static pl.edu.mimuw.nesc.problem.issue.InvalidParameterTypeError.ParameterKind;

/**
 * Class that is responsible for analysis of expressions.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class ExpressionsAnalysis extends ExceptionVisitor<Optional<ExprData>, Void> {
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
     * ABI of the target platform.
     */
    protected final ABI abi;

    /**
     * Object that will be notified about detected problems.
     */
    protected final ErrorHelper errorHelper;

    protected ExpressionsAnalysis(ABI abi, ErrorHelper errorHelper) {
        this.abi = abi;
        this.errorHelper = errorHelper;
    }

    @Override
    public Optional<ExprData> visitPlus(Plus expr, Void arg) {
        touch(expr);
        final BinaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        // Perform operations
        cr.leftData.superDecay();
        cr.rightData.superDecay();

        final Optional<Type> arithmeticResult = performGeneralizedArithmeticCheck(cr.leftType(),
                cr.rightType());
        Optional<? extends Type> resultType = Optional.absent();
        Optional<? extends CautionaryIssue> warning = Optional.absent();

        // Check types and simultaneously determine the result type
        if (arithmeticResult.isPresent()) {

            resultType = arithmeticResult;

        } else if (cr.leftType().isPointerType() || cr.rightType().isPointerType()) {
            final PointerType ptrType = cr.leftType().isPointerType()
                    ? (PointerType) cr.leftType()
                    : (PointerType) cr.rightType();
            final Type otherType = ptrType == cr.leftType()
                    ? cr.rightType()
                    : cr.leftType();
            final Type referencedType = ptrType.getReferencedType();

            if ((referencedType.isComplete() || referencedType.isVoid())
                    && referencedType.isObjectType()
                    && otherType.isGeneralizedIntegerType()) {

                resultType = Optional.of(ptrType);

                if (referencedType.isVoid()) {
                    warning = Optional.of(new VoidPointerArithmeticsWarning(
                            ptrType == cr.leftType() ? expr.getLeftArgument() : expr.getRightArgument(),
                            ptrType,
                            PLUS
                    ));
                }
            }
        }

        if (!resultType.isPresent()) {
            final ErroneousIssue error = new InvalidPlusExprError(cr.leftType(), expr.getLeftArgument(),
                    cr.rightType(), expr.getRightArgument());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        } else if (warning.isPresent()) {
            errorHelper.warning(expr.getLocation(), expr.getEndLocation(), warning.get());
        }

        final ExprData result = ExprData.builder()
                .type(resultType.get())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(cr.leftData.isNullPointerConstant()
                        && cr.rightData.isNullPointerConstant())
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitMinus(Minus expr, Void arg) {
        touch(expr);
        final BinaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.leftData.superDecay();
        cr.rightData.superDecay();


        final Optional<Type> arithmeticResult = performGeneralizedArithmeticCheck(cr.leftType(),
                cr.rightType());
        Optional<? extends Type> resultType = Optional.absent();

        /* Check if types are correct and simultaneously determine the result
           type */
        if (arithmeticResult.isPresent()) {

            resultType = arithmeticResult;

        } else if (cr.leftType().isPointerType() && cr.rightType().isPointerType()) {

            final PointerType ptrType1 = (PointerType) cr.leftType(),
                              ptrType2 = (PointerType) cr.rightType();
            final Type refType1 = ptrType1.getReferencedType().removeQualifiers(),
                       refType2 = ptrType2.getReferencedType().removeQualifiers();

            if (refType1.isComplete() && refType1.isObjectType() && refType2.isComplete()
                    && refType2.isObjectType() && refType1.isCompatibleWith(refType2)) {
                resultType = Optional.of(TypeUtils.newIntegerType(this.abi.getPtrdiffT()));
            }
        } else if (cr.leftType().isPointerType() && cr.rightType().isGeneralizedIntegerType()) {

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
                .build()
                .spread(expr);

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
        touch(expr);
        final BinaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.rightData.superDecay();
        cr.leftData.decay();

        Optional<? extends ErroneousIssue> error = Optional.absent();

        if (!cr.leftData.isModifiableLvalue()) {
            error = Optional.of(new NotModifiableLvalueError(cr.leftType(), expr.getLeftArgument(),
                    cr.leftData.isLvalue()));
        } else if (!checkAssignment(cr.leftType(), cr.rightType())) {
            error = Optional.of(new InvalidSimpleAssignExprError(cr.leftType(),
                    expr.getLeftArgument(), cr.rightType(), expr.getRightArgument()));
        }

        if (error.isPresent()) {
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error.get());
        } else if (cr.leftType().isPointerType() && cr.rightType().isGeneralizedIntegerType()
                && !cr.rightData.isNullPointerConstant()) {

            final CautionaryIssue warn = new InvalidPointerAssignmentWarning(cr.leftType(),
                    expr.getLeftArgument(), cr.rightType(), expr.getRightArgument());
            errorHelper.warning(expr.getLocation(), expr.getEndLocation(), warn);
        }

        final ExprData result = ExprData.builder()
                .type(cr.leftType().removeQualifiers())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitPlusAssign(PlusAssign expr, Void arg) {
        return analyzeAssignAdditiveExpr(expr, ASSIGN_PLUS);
    }

    @Override
    public Optional<ExprData> visitMinusAssign(MinusAssign expr, Void arg) {
        return analyzeAssignAdditiveExpr(expr, ASSIGN_MINUS);
    }

    @Override
    public Optional<ExprData> visitTimesAssign(TimesAssign expr, Void arg) {
        return analyzeCompoundAssignExpr(expr, ASSIGN_TIMES);
    }

    @Override
    public Optional<ExprData> visitDivideAssign(DivideAssign expr, Void arg) {
        return analyzeCompoundAssignExpr(expr, ASSIGN_DIVIDE);
    }

    @Override
    public Optional<ExprData> visitModuloAssign(ModuloAssign expr, Void arg) {
        return analyzeCompoundAssignExpr(expr, ASSIGN_MODULO);
    }

    @Override
    public Optional<ExprData> visitLshiftAssign(LshiftAssign expr, Void arg) {
        return analyzeCompoundAssignExpr(expr, ASSIGN_LSHIFT);
    }

    @Override
    public Optional<ExprData> visitRshiftAssign(RshiftAssign expr, Void arg) {
        return analyzeCompoundAssignExpr(expr, ASSIGN_RSHIFT);
    }

    @Override
    public Optional<ExprData> visitBitandAssign(BitandAssign expr, Void arg) {
        return analyzeCompoundAssignExpr(expr, ASSIGN_BITAND);
    }

    @Override
    public Optional<ExprData> visitBitorAssign(BitorAssign expr, Void arg) {
        return analyzeCompoundAssignExpr(expr, ASSIGN_BITOR);
    }

    @Override
    public Optional<ExprData> visitBitxorAssign(BitxorAssign expr, Void arg) {
        return analyzeCompoundAssignExpr(expr, ASSIGN_BITXOR);
    }

    @Override
    public Optional<ExprData> visitDereference(Dereference expr, Void arg) {
        touch(expr);
        final UnaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.argData.superDecay();

        if (!cr.argType().isPointerType()) {
            final ErroneousIssue error = new InvalidDereferenceExprError(cr.argType(),
                    expr.getArgument());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final PointerType argPtrType = (PointerType) cr.argType();
        final Type argRefType = argPtrType.getReferencedType();

        final ExprData result = ExprData.builder()
                .type(argRefType)
                .isLvalue(argRefType.isObjectType())
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitAddressOf(AddressOf expr, Void arg) {
        touch(expr);
        final UnaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        // FIXME add checking if the lvalue has not 'register' specifier
        final boolean correct = cr.argType().isFunctionType()
                || cr.argData.isLvalue() && cr.argType().isObjectType()
                        && !cr.argData.isBitField();

        if (!correct) {
            final ErroneousIssue error = new InvalidAddressOfExprError(cr.argType(),
                    expr.getArgument(), cr.argData.isLvalue(), cr.argData.isBitField(),
                    false);
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(new PointerType(cr.argType()))
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
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
        touch(expr);
        final UnaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.argData.superDecay();

        if (!cr.argType().isGeneralizedIntegerType()) {
            final ErroneousIssue error = new InvalidBitnotExprError(cr.argType(), expr.getArgument());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(cr.argType().promote())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitNot(Not expr, Void arg) {
        touch(expr);
        final UnaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.argData.superDecay();

        if (!cr.argType().isGeneralizedScalarType()) {
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
                .build()
                .spread(expr);

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
        touch(expr);
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitImagpart(Imagpart expr, Void arg) {
        touch(expr);
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitArrayRef(ArrayRef expr, Void arg) {
        touch(expr);

        // Analyze subexpressions
        final Optional<ExprData> oArrayData = expr.getArray().accept(this, null);
        Optional<ExprData> oIndexData = Optional.absent();
        for (Expression indexExpr : expr.getIndex()) {
            oIndexData = indexExpr.accept(this, null);
        }

        // End analysis if important subexpressions are invalid
        if (!oArrayData.isPresent() || !oIndexData.isPresent()) {
            return Optional.absent();
        }

        final ExprData arrayData = oArrayData.get(),
                       indexData = oIndexData.get();

        // Perform operations
        arrayData.superDecay();
        indexData.superDecay();

        // Prepare for further analysis
        final Type arrayType = arrayData.getType(),
                   indexType = indexData.getType();
        Optional<Type> resultType = Optional.absent();

        // Check types and simultaneously determine the type of the result
        if (arrayType.isPointerType() || indexType.isPointerType()) {
            // Distinction between the array and index part is only symbolic
            final PointerType ptrType = arrayType.isPointerType()
                    ? (PointerType) arrayType
                    : (PointerType) indexType;
            final Type otherType = ptrType == arrayType
                    ? indexType
                    : arrayType;
            final Type refType = ptrType.getReferencedType();

            if (refType.isComplete() && refType.isObjectType()
                    && otherType.isGeneralizedIntegerType()) {
                resultType = Optional.of(refType);
            }
        }

        // Report error if the types are invalid
        if (!resultType.isPresent()) {
            final Comma dummyComma = new Comma(expr.getIndex().getFirst().getLocation(), expr.getIndex());
            final ErroneousIssue error = new InvalidArrayRefExprError(arrayType,
                    expr.getArray(), indexType, dummyComma);
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(resultType.get())
                .isLvalue(true)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitErrorExpr(ErrorExpr expr, Void arg) {
        touch(expr);
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitComma(Comma expr, Void arg) {
        touch(expr);

        /* Analyze all subexpressions and simultaneously determine the data for
           the last one. */
        Optional<ExprData> oLastData = Optional.absent();
        for (Expression subexpr : expr.getExpressions()) {
            oLastData = subexpr.accept(this, null);
        }

        if (!oLastData.isPresent()) {
            return Optional.absent();
        }

        final ExprData lastData = oLastData.get();
        lastData.superDecay();

        final ExprData result = ExprData.builder()
                .type(lastData.getType())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitLabelAddress(LabelAddress expr, Void arg) {
        touch(expr);
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitConditional(Conditional expr, Void arg) {
        touch(expr);

        // Analyze all three subexpressions
        final Optional<ExprData> oCondData = expr.getCondition().accept(this, null);
        final Optional<ExprData> oOnTrueData = expr.getOnTrueExp().isPresent()
                ? expr.getOnTrueExp().get().accept(this, null)
                : oCondData;
        final Optional<ExprData> oOnFalseData = expr.getOnFalseExp().accept(this, null);

        // End analysis if one of the expressions is not valid
        if (!oCondData.isPresent() || !oOnTrueData.isPresent() || !oOnFalseData.isPresent()) {
            return Optional.absent();
        }

        final ExprData condData = oCondData.get(),
                       trueData = oOnTrueData.get(),
                       falseData = oOnFalseData.get();

        condData.superDecay();
        trueData.superDecay();
        falseData.superDecay();

        final Optional<? extends Type> resultType = resolveTypeOfConditional(condData.getType(),
                trueData.getType(), falseData.getType());

        final boolean ptrWarn = trueData.getType().isPointerType() && falseData.getType().isIntegerType()
                        && !falseData.isNullPointerConstant()
                || trueData.getType().isIntegerType() && falseData.getType().isPointerType()
                        && !trueData.isNullPointerConstant();

        final Expression trueExpr = expr.getOnTrueExp().isPresent()
                ? expr.getOnTrueExp().get()
                : expr.getCondition();

        // Report detected issues
        if (!resultType.isPresent()) {
            final ErroneousIssue error = new InvalidConditionalExprError(condData.getType(),
                    expr.getCondition(), trueData.getType(), trueExpr, falseData.getType(),
                    expr.getOnFalseExp());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        } else if (ptrWarn) {
            final CautionaryIssue warn = new InvalidPointerConditionalWarning(trueData.getType(),
                    trueExpr, falseData.getType(), expr.getOnFalseExp());
            errorHelper.warning(expr.getLocation(), expr.getEndLocation(), warn);
        }

        final ExprData result = ExprData.builder()
                .type(resultType.get())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    /**
     * Check types of a conditional expression and simultaneously determine the
     * type of the result.
     *
     * @return Result type if types of subexpressions are correct. Otherwise,
     *         the type is absent.
     */
    private Optional<? extends Type> resolveTypeOfConditional(Type condType, Type trueType,
            Type falseType) {

        final Optional<Type> arithmeticResult = performGeneralizedArithmeticCheck(trueType,
                falseType);

        if (!condType.isGeneralizedScalarType()) {
            return Optional.absent();
        } else if (arithmeticResult.isPresent()) {

            return arithmeticResult;

        } else if (trueType.isFieldTagType() && falseType.isFieldTagType()
                && trueType.isCompatibleWith(falseType)) {

            return Optional.of(trueType);

        } else if (trueType.isVoid() && falseType.isVoid()) {

            return Optional.of(trueType);

        } else if (trueType.isPointerType() && falseType.isPointerType()) {

            final PointerType truePtrType = (PointerType) trueType,
                              falsePtrType = (PointerType) falseType;
            final Type trueRefType = truePtrType.getReferencedType(),
                       falseRefType = falsePtrType.getReferencedType();
            final Type trueUnqualRefType = trueRefType.removeQualifiers(),
                       falseUnqualRefType = falseRefType.removeQualifiers();

            if (trueUnqualRefType.isCompatibleWith(falseUnqualRefType)) {
                final Type newRefType = trueRefType.addQualifiers(falseRefType);
                return Optional.of(new PointerType(newRefType));
            } else if (trueRefType.isVoid() && falseRefType.isObjectType()) {
                return Optional.of(new PointerType(trueRefType.addQualifiers(falseRefType)));
            } else if (trueRefType.isObjectType() && falseRefType.isVoid()) {
                return Optional.of(new PointerType(falseRefType.addQualifiers(trueRefType)));
            }
        } else if (trueType.isPointerType() && falseType.isGeneralizedIntegerType()) {
            return Optional.of(trueType);
        } else if (trueType.isGeneralizedIntegerType() && falseType.isPointerType()) {
            return Optional.of(falseType);
        }

        return Optional.absent();
    }

    @Override
    public abstract Optional<ExprData> visitIdentifier(Identifier identifier, Void arg);

    @Override
    public Optional<ExprData> visitCompoundExpr(CompoundExpr expr, Void arg) {
        touch(expr);
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitIntegerCst(IntegerCst expr, Void arg) {
        touch(expr);
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
                if (possibleType.getRange(this.abi).contains(value.get())) {
                    type = Optional.of(possibleType);
                    break;
                }
            }
        }

        // Emit error if the constant has no type and return absent value
        if (type.isPresent()) {
            return Optional.of(dataBuilder.type(type.get()).build().spread(expr));
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

        return Optional.of(dataBuilder.build().spread(expr));
    }

    @Override
    public Optional<ExprData> visitCharacterCst(CharacterCst expr, Void arg) {
        final Optional<Character> charValue = expr.getValue();

        final ExprData result = ExprData.builder()
                .type(new IntType())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(charValue.isPresent() && charValue.get() == '\0')
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitStringCst(StringCst expr, Void arg) {
        final ExprData result = ExprData.builder()
                .type(new ArrayType(new CharType(),
                        Optional.of(AstUtils.newIntegerConstant(expr.getString().length() + 1))))
                .isLvalue(true)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitStringAst(StringAst expr, Void arg) {
        analyzeSubexpressions(expr.getStrings(), false);

        // Compute the length of the entire string
        int length = 1;
        for (StringCst cst : expr.getStrings()) {
            length += cst.getString().length();
        }

        final ExprData result = ExprData.builder()
                .type(new ArrayType(new CharType(),
                        Optional.of(AstUtils.newIntegerConstant(length))))
                .isLvalue(true)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitFunctionCall(FunctionCall expr, Void arg) {
        touch(expr);

        // FIXME analysis of __builtin_va_arg(arguments, vaArgCall)
        if (expr.getFunction() == null) {
            return Optional.absent();
        }

        switch (expr.getCallKind()) {
            case NORMAL_CALL:
                return analyzeNormalCall(expr);
            case POST_TASK:
                return analyzePostTask(expr);
            case COMMAND_CALL:
                return analyzeNescCall(expr, false);
            case EVENT_SIGNAL:
                return analyzeNescCall(expr, true);
            default:
                throw new RuntimeException("unexpected call kind '" + expr.getCallKind() + "'");
        }
    }

    /**
     * Check types of expressions in a function call and simultaneously
     * determine the type of the result.
     */
    private FunctionCallReport checkFunctionCall(Type funExprType, Expression funExpr,
                List<Optional<ExprData>> argsData, LinkedList<Expression> argsExprs,
                InvalidFunctionCallError.Builder errBuilder) {

        // Check type of the function expression
        if (!funExprType.isPointerType()) {
            return FunctionCallReport.errorWithoutType();
        }
        final PointerType funPtrType = (PointerType) funExprType;
        final Type refType = funPtrType.getReferencedType();
        if (!refType.isFunctionType()) {
            return FunctionCallReport.errorWithoutType();
        }

        // Check the return type
        final FunctionType funType = (FunctionType) refType;
        final Type returnType = funType.getReturnType();
        if ((!returnType.isObjectType() || !returnType.isComplete())
                && !returnType.isVoid()) {
            return FunctionCallReport.errorWithoutType();
        }

        // Check number of parameters
        final boolean varArgs = funType.getVariableArguments();
        final int expectedParamsCount = funType.getArgumentsTypes().size(),
                  actualParamsCount = argsData.size();
        errBuilder.expectedParamsCount(expectedParamsCount)
                .actualParamsCount(actualParamsCount)
                .variableArgumentsFunction(varArgs);
        if ((!varArgs && expectedParamsCount != actualParamsCount)
                || (varArgs && actualParamsCount < expectedParamsCount)) {
            return FunctionCallReport.errorWithoutType();
        }

        if (checkParametersTypes(funExpr, funType.getArgumentsTypes().iterator(),
                argsData.iterator(), argsExprs.iterator(), FunctionKind.NORMAL_FUNCTION,
                ParameterKind.NORMAL_PARAMETER)) {
            return FunctionCallReport.withType(funType.getReturnType());
        }

        return FunctionCallReport.empty();
    }

    /**
     * Check is the types of parameters for a function are correct and
     * simultaneously report detected errors.
     *
     * @return <code>true</code> if and only if all parameters are valid.
     */
    protected boolean checkParametersTypes(Expression funExpr, Iterator<Optional<Type>> expectedTypeIt,
            Iterator<Optional<ExprData>> argDataIt, Iterator<Expression> argExprIt, FunctionKind funKind,
            ParameterKind paramKind) {
        boolean errorOccurred = false;
        int paramNum = 0;

        // Check parameters types
        while (expectedTypeIt.hasNext()) {
            ++paramNum;
            final Optional<Type> expectedType = expectedTypeIt.next();
            final Optional<ExprData> argData = argDataIt.next();
            final Expression argExpr = argExprIt.next();

            if (!expectedType.isPresent() || !argData.isPresent()) {
                continue;
            }

            if (!checkAssignment(expectedType.get().removeQualifiers(), argData.get().getType())) {
                errorOccurred = true;
                final ErroneousIssue error = new InvalidParameterTypeError(funExpr, paramNum, argExpr,
                        expectedType.get().removeQualifiers(), argData.get().getType(), funKind, paramKind);
                errorHelper.error(argExpr.getLocation(), argExpr.getEndLocation(), error);
            }
        }

        return !errorOccurred;
    }

    @Override
    public Optional<ExprData> visitUniqueCall(UniqueCall expr, Void arg) {
        return analyzeConstantFunctionCall(expr, UNIQUE);
    }

    @Override
    public Optional<ExprData> visitUniqueNCall(UniqueNCall expr, Void arg) {
        return analyzeConstantFunctionCall(expr, UNIQUEN);
    }

    @Override
    public Optional<ExprData> visitUniqueCountCall(UniqueCountCall expr, Void arg) {
        return analyzeConstantFunctionCall(expr, UNIQUECOUNT);
    }

    @Override
    public Optional<ExprData> visitFieldRef(FieldRef expr, Void arg) {
        touch(expr);
        final UnaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.argData.decay();

        Optional<Type> resultType = Optional.absent();
        boolean isTypeComplete = false, isFieldPresent = false;
        boolean isBitField = false;

        // Check type and simultaneously determine the type of the result
        if (cr.argType().isFieldTagType()) {
            final FieldTagType<?> tagType = (FieldTagType<?>) cr.argType();

            if (tagType.isComplete()) {
                isTypeComplete = true;
                final FieldTagDeclaration<?> tagDecl = tagType.getDeclaration();
                final Optional<FieldDeclaration> fieldDecl = tagDecl.findField(expr.getFieldName());

                if (fieldDecl.isPresent()) {
                    expr.setDeclaration(fieldDecl.get());
                    isFieldPresent = true;
                    isBitField = fieldDecl.get().isBitField();
                    final Optional<Type> fieldType = fieldDecl.get().getType();

                    if (fieldType.isPresent()) {
                        resultType = Optional.of(fieldType.get().addQualifiers(cr.argType()));
                    } else {
                        // End analysis if the type is specified incorrectly
                        return Optional.absent();
                    }
                }
            }
        }

        if (!resultType.isPresent()) {
            final ErroneousIssue error = new InvalidFieldRefExprError(cr.argType(),
                    expr.getArgument(), expr.getFieldName(), isTypeComplete,
                    isFieldPresent);
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(resultType.get())
                .isLvalue(cr.argData.isLvalue())
                .isBitField(isBitField)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitInterfaceDeref(InterfaceDeref expr, Void arg) {
        touch(expr);
        // FIXME
        return Optional.absent();
    }

    @Override
    public abstract Optional<ExprData> visitComponentDeref(ComponentDeref expr, Void arg);

    @Override
    public Optional<ExprData> visitPreincrement(Preincrement expr, Void arg) {
        return analyzeIncrementExpr(expr, INCREMENT);
    }

    @Override
    public Optional<ExprData> visitPredecrement(Predecrement expr, Void arg) {
        return analyzeIncrementExpr(expr, DECREMENT);
    }

    @Override
    public Optional<ExprData> visitPostincrement(Postincrement expr, Void arg) {
        return analyzeIncrementExpr(expr, INCREMENT);
    }

    @Override
    public Optional<ExprData> visitPostdecrement(Postdecrement expr, Void arg) {
        return analyzeIncrementExpr(expr, DECREMENT);
    }

    @Override
    public Optional<ExprData> visitCast(Cast expr, Void arg) {
        touch(expr);
        final UnaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good || !expr.getAsttype().getType().isPresent()) {
            return Optional.absent();
        }

        final Type destType = expr.getAsttype().getType().get();
        cr.argData.superDecay();

        final boolean correct = (destType.isVoid() || destType.isGeneralizedScalarType())
                && (!cr.argType().isFloatingType() || !destType.isPointerType())
                && (!cr.argType().isPointerType() || !destType.isFloatingType())
                && (!cr.argType().isUnknownArithmeticType() || cr.argType().isUnknownIntegerType()
                    || !destType.isPointerType())
                && (!cr.argType().isPointerType() || !destType.isUnknownArithmeticType()
                    || destType.isUnknownIntegerType())
                && cr.argType().isGeneralizedScalarType();

        if (!correct) {
            final ErroneousIssue error = new InvalidCastExprError(destType, cr.argType(),
                    expr.getArgument());
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error);
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(destType)
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitCastList(CastList expr, Void arg) {
        touch(expr);
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitInitList(InitList expr, Void arg) {
        touch(expr);
        return analyzeInitializer(expr);
    }

    @Override
    public Optional<ExprData> visitInitSpecific(InitSpecific expr, Void arg) {
        touch(expr);
        return analyzeInitializer(expr);
    }

    @Override
    public Optional<ExprData> visitTypeArgument(TypeArgument expr, Void arg) {
        touch(expr);
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitGenericCall(GenericCall expr, Void arg) {
        touch(expr);
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitExtensionExpr(ExtensionExpr expr, Void arg) {
        touch(expr);

        final Optional<ExprData> result = expr.getArgument().accept(this, null);
        if (result.isPresent()) {
            result.get().spread(expr);
        }

        return result;
    }

    @Override
    public Optional<ExprData> visitConjugate(Conjugate expr, Void arg) {
        touch(expr);
        // FIXME
        return Optional.absent();
    }

    /**
     * Analysis for operators <code>&lt;&lt;</code> and <code>&gt;&gt;</code>.
     */
    private Optional<ExprData> analyzeShiftExpr(Binary shiftExpr, BinaryOp op) {
        touch(shiftExpr);
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
                .build()
                .spread(shiftExpr);

        return Optional.of(result);
    }

    /**
     * Analysis for operators <code>sizeof</code> and <code>_Alignof</code>
     * applied to typenames.
     */
    private Optional<ExprData> analyzeTypeQueryExpr(Expression typeQueryExpr,
            Optional<Type> arg, String op) {
        touch(typeQueryExpr);
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
                .type(TypeUtils.newIntegerType(this.abi.getSizeT()))
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(typeQueryExpr);

        return Optional.of(result);
    }

    /**
     * Analysis for operators <code>sizeof</code> and <code>_Alignof</code>
     * applied to expressions.
     */
    private Optional<ExprData> analyzeExprQueryExpr(Unary unary, String op) {
        touch(unary);
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
                .type(TypeUtils.newIntegerType(this.abi.getSizeT()))
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(unary);

        return Optional.of(result);
    }

    /**
     * Analysis for operators <code>*</code>, <code>/</code> and <code>%</code>.
     */
    private Optional<ExprData> analyzeMultiplicativeExpr(Binary expr, BinaryOp op) {
        touch(expr);
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

        final Type resultType = performGeneralizedArithmeticCheck(cr.leftType(),
                cr.rightType()).get();
        final boolean isNullPtrCst = resultType.isIntegerType()
                && (op != TIMES || cr.leftData.isNullPointerConstant() || cr.rightData.isNullPointerConstant())
                && (op != MODULO && op != DIVIDE || cr.leftData.isNullPointerConstant());

        final ExprData result = ExprData.builder()
                .type(resultType)
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(isNullPtrCst)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    /**
     * Analysis for unary operators <code>+</code> and <code>-</code>.
     */
    private Optional<ExprData> analyzeUnaryAdditiveExpr(Unary expr, UnaryOp op) {
        touch(expr);
        final UnaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.argData.superDecay();

        if (!cr.argType().isGeneralizedArithmeticType()) {
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
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    /**
     * Analysis for binary operators <code>&lt;=</code>, <code>&gt;=</code>,
     * <code>&lt;</code> and <code>&gt;</code>.
     */
    private Optional<ExprData> analyzeCompareExpr(Binary expr, BinaryOp op) {
        touch(expr);
        final BinaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.leftData.superDecay();
        cr.rightData.superDecay();

        boolean correct = false;
        if (cr.leftType().isGeneralizedRealType() && cr.rightType().isGeneralizedRealType()) {
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
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    /**
     * Analysis for binary operators <code>==</code> and <code>!=</code>.
     */
    private Optional<ExprData> analyzeEqualityExpr(Binary expr, BinaryOp op) {
        touch(expr);
        final BinaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.leftData.superDecay();
        cr.rightData.superDecay();

        // Consider the cases when the type is correct
        boolean correct = false, missingCastWarning = false;
        if (cr.leftType().isGeneralizedArithmeticType() && cr.rightType().isGeneralizedArithmeticType()) {
            correct = true;
        } else if (cr.leftType().isPointerType() && cr.rightType().isPointerType()) {

            final PointerType leftPtrType = (PointerType) cr.leftType(),
                              rightPtrType = (PointerType) cr.rightType();
            final Type leftRefType = leftPtrType.getReferencedType().removeQualifiers(),
                       rightRefType = rightPtrType.getReferencedType().removeQualifiers();

            correct = leftRefType.isCompatibleWith(rightRefType)
                      || leftRefType.isObjectType() && rightRefType.isObjectType()
                           && (leftRefType.isVoid() || rightRefType.isVoid());
        } else if (cr.leftType().isPointerType() && cr.rightType().isGeneralizedIntegerType()) {
            correct = true;
            missingCastWarning = !cr.rightData.isNullPointerConstant();
        } else if (cr.leftType().isGeneralizedIntegerType() && cr.rightType().isPointerType()) {
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
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    /**
     * Analysis for binary operators <code>&amp;&amp;</code> and <code>||</code>.
     */
    private Optional<ExprData> analyzeBinaryLogicalExpr(Binary expr, BinaryOp op) {
        touch(expr);
        final BinaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.leftData.superDecay();
        cr.rightData.superDecay();

        if (!cr.leftType().isGeneralizedScalarType() || !cr.rightType().isGeneralizedScalarType()) {
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
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    /**
     * Analysis for operators <code>&amp;</code>, <code>|</code> and <code>^</code>.
     */
    private Optional<ExprData> analyzeBinaryBitExpr(Binary expr, BinaryOp op) {
        touch(expr);
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

        final Type resultType = performGeneralizedArithmeticCheck(cr.leftType(),
                cr.rightType()).get();
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
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    /**
     * Analysis for operators <code>+=</code> and <code>-=</code>.
     */
    private Optional<ExprData> analyzeAssignAdditiveExpr(Assignment expr, BinaryOp op) {
        touch(expr);
        final BinaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.rightData.superDecay();
        cr.leftData.decay();

        final boolean typesCorrect = cr.leftType().isGeneralizedArithmeticType()
                && cr.rightType().isGeneralizedArithmeticType()
                || checkPointerAdvance(cr.leftType(), cr.rightType());

        Optional<? extends ErroneousIssue> error = Optional.absent();
        if (!cr.leftData.isModifiableLvalue()) {
            error = Optional.of(new NotModifiableLvalueError(cr.leftType(), expr.getLeftArgument(),
                    cr.leftData.isLvalue()));
        } else if (!typesCorrect) {
            error = Optional.of(new InvalidAssignAdditiveExprError(cr.leftType(),
                    expr.getLeftArgument(), op, cr.rightType(), expr.getRightArgument()));
        }

        if (error.isPresent()) {
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error.get());
            return Optional.absent();
        } else if (cr.leftType().isPointerType()) {
            // Emit a warning if a pointer to void is changed
            if (((PointerType) cr.leftType()).getReferencedType().isVoid()) {
                errorHelper.warning(expr.getLocation(), expr.getEndLocation(),
                        new VoidPointerAdvanceWarning(expr.getLeftArgument(), cr.leftType()));
            }
        }

        final ExprData result = ExprData.builder()
                .type(cr.leftType().removeQualifiers())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    /**
     * Analysis for operators <code>*=</code>, <code>/=</code>, <code>%=</code>,
     * <code>&lt;&lt;=</code>, <code>&gt;&gt;=</code>, <code>&=</code>,
     * <code>|=</code>, <code>^=</code>.
     */
    private Optional<ExprData> analyzeCompoundAssignExpr(Assignment expr, BinaryOp op) {
        touch(expr);
        final BinaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.rightData.superDecay();
        cr.leftData.decay();

        Optional<? extends ErroneousIssue> error = Optional.absent();
        if (!cr.leftData.isModifiableLvalue()) {
            error = Optional.of(new NotModifiableLvalueError(cr.leftType(), expr.getLeftArgument(),
                    cr.leftData.isLvalue()));
        } else {
            boolean correct;
            if (op == ASSIGN_TIMES || op == ASSIGN_DIVIDE || op == ASSIGN_MODULO) {
                correct = checkMultiplicativeOpsTypes(cr.leftType(), cr.rightType(), op);
            } else if (op == ASSIGN_LSHIFT || op == ASSIGN_RSHIFT) {
                correct = checkShiftOpsTypes(cr.leftType(), cr.rightType());
            } else if (op == ASSIGN_BITAND || op == ASSIGN_BITOR || op == ASSIGN_BITXOR) {
                correct = checkBinaryBitOpsTypes(cr.leftType(), cr.rightType());
            } else {
                throw new RuntimeException("invalid compound assignment operator '" + op + "'");
            }

            if (!correct) {
                error = Optional.of(new InvalidCompoundAssignExprError(cr.leftType(),
                        expr.getLeftArgument(), op, cr.rightType(), expr.getRightArgument()));
            }
        }

        if (error.isPresent()) {
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error.get());
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(cr.leftType().removeQualifiers())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    /**
     * Analysis for operators <code>++</code> and <code>--</code> in both
     * versions.
     */
    private Optional<ExprData> analyzeIncrementExpr(Increment expr, UnaryOp op) {
        touch(expr);
        final UnaryExprDataCarrier cr = analyzeSubexpressions(expr);
        if (!cr.good) {
            return Optional.absent();
        }

        cr.argData.decay();

        Optional<? extends ErroneousIssue> error = Optional.absent();

        if (!cr.argData.isModifiableLvalue()) {
            error = Optional.of(new NotModifiableLvalueError(cr.argType(), expr.getArgument(),
                    cr.argData.isLvalue()));
        } else if (!cr.argType().isGeneralizedRealType() && (!cr.argType().isPointerType()
                || !checkPointerAdvance(cr.argType(), new IntType()))) {
            error = Optional.of(new InvalidIncrementExprError(op, cr.argType(), expr.getArgument()));
        }

        if (error.isPresent()) {
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error.get());
            return Optional.absent();
        } else if (cr.argType().isPointerType()) {
            if (((PointerType) cr.argType()).getReferencedType().isVoid()) {
                final VoidPointerAdvanceWarning warn = new VoidPointerAdvanceWarning(expr.getArgument(),
                        cr.argType());
                errorHelper.warning(expr.getLocation(), expr.getEndLocation(), warn);
            }
        }

        final ExprData result = ExprData.builder()
                .type(cr.argType().removeQualifiers())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    /**
     * Analysis for a normal function call, e.g. <code>f(2, "abc")</code>.
     */
    private Optional<ExprData> analyzeNormalCall(FunctionCall expr) {
        // Analyze subexpressions
        final Optional<ExprData> oFunData = expr.getFunction().accept(this, null);
        final ImmutableList<Optional<ExprData>> argsData = analyzeSubexpressions(expr.getArguments(), true);

        // End analysis if the function expression is invalid
        if (!oFunData.isPresent()) {
            return Optional.absent();
        }
        final ExprData funData = oFunData.get();

        // Perform operations
        funData.superDecay();
        // arguments data objects are automatically super decayed earlier

        final InvalidFunctionCallError.Builder errBuilder = InvalidFunctionCallError.builder()
                .funExpr(funData.getType(), expr.getFunction());
        final FunctionCallReport report = checkFunctionCall(funData.getType(), expr.getFunction(),
                argsData, expr.getArguments(), errBuilder);

        if (report.reportError) {
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), errBuilder.build());
            return Optional.absent();
        } else if (!report.returnType.isPresent()) {
            return Optional.absent();
        }

        final ExprData result = ExprData.builder()
                .type(report.returnType.get())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    /**
     * Analysis of a task post expression, e.g. <code>post sendTask()</code>.
     */
    protected abstract Optional<ExprData> analyzePostTask(FunctionCall expr);

    /**
     * Analysis of a command call or an event signal expression, e.g.:
     *
     * <pre>
     * call Send.send(&amp;adcPacket, sizeof adcPacket.data)
     * signal Send.sendDone[msg-&gt;amId](msg, SUCCESS)
     * </pre>
     */
    protected abstract Optional<ExprData> analyzeNescCall(FunctionCall expr, boolean isSignal);

    /**
     * Report error because initializers cannot be used within expressions.
     *
     * @param initializer <code>InitSpecific</code> or <code>InitList</code>
     *                    object.
     * @return Absent object.
     */
    private Optional<ExprData> analyzeInitializer(Expression initializer) {
        errorHelper.error(initializer.getLocation(), initializer.getEndLocation(),
                new InvalidInitializerUsageError());
        return Optional.absent();
    }

    /**
     * Analysis of NesC builtin constant function calls:
     *
     * <pre>
     *     unique("aaa")
     *     uniqueN("bbb", 10)
     *     uniqueCount("aaa")
     * </pre>
     */
    private Optional<ExprData> analyzeConstantFunctionCall(ConstantFunctionCall expr, ConstantFun fun) {
        touch(expr);
        final ImmutableList<Optional<ExprData>> argsDatas = analyzeSubexpressions(expr.getArguments(), true);
        final int providedParamsCount = argsDatas.size();
        final int expectedParamsCount = fun == UNIQUEN
                ? 2
                : 1;
        final Optional<InvalidConstantFunctionCallError> error;

        if (expr.getCallKind() != NescCallKind.NORMAL_CALL) {
            error = Optional.of(InvalidConstantFunctionCallError.invalidCallKind(fun));
        } else if (providedParamsCount != expectedParamsCount) {
            error = Optional.of(InvalidConstantFunctionCallError.invalidParametersCount(fun,
                    providedParamsCount, expectedParamsCount));
        } else {
            // Check the identifier
            final Optional<InvalidConstantFunctionCallError> firstArgError =
                    checkIdentifierForConstantFunction(expr.getArguments().getFirst(), argsDatas.get(0), fun);

            if (firstArgError.isPresent()) {
                error = firstArgError;
            } else if (fun == UNIQUEN) {
                error = checkNumbersCountForUniqueN(expr.getArguments().getLast(), argsDatas.get(1));
            } else {
                error = Optional.absent();
            }
        }

        if (error.isPresent()) {
            errorHelper.error(expr.getLocation(), expr.getEndLocation(), error.get());
            return Optional.absent();
        }

        updateConstantFunctionIdentifier((Identifier) expr.getFunction(), fun);

        final ExprData result = ExprData.builder()
                .type(new UnsignedIntType())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build()
                .spread(expr);

        return Optional.of(result);
    }

    private Optional<InvalidConstantFunctionCallError> checkIdentifierForConstantFunction(Expression firstArg,
                Optional<ExprData> argData, ConstantFun fun) {
        // Check the type of the first parameter

        if (!argData.isPresent()) {
            return Optional.absent();
        }

        final Type providedType = argData.get().getType();

        if (providedType.isPointerType()) {
            final PointerType ptrType = (PointerType) argData.get().getType();
            if (!ptrType.getReferencedType().removeQualifiers().isCompatibleWith(new CharType())) {
                return Optional.of(InvalidConstantFunctionCallError.invalidIdentifierType(fun, providedType));
            }
        } else {
            return Optional.of(InvalidConstantFunctionCallError.invalidIdentifierType(fun, providedType));
        }

        // Check if a constant expression is provided

        if (firstArg instanceof StringAst || firstArg instanceof StringCst) {
            return Optional.absent();
        } else if (firstArg instanceof Identifier) {
            final Identifier identifier = (Identifier) firstArg;
            return identifier.getIsGenericReference()
                    ? Optional.<InvalidConstantFunctionCallError>absent()
                    : Optional.of(InvalidConstantFunctionCallError.nonConstantIdentifier(fun, firstArg));
        } else {
            return Optional.of(InvalidConstantFunctionCallError.nonConstantIdentifier(fun, firstArg));
        }
    }

    private Optional<InvalidConstantFunctionCallError> checkNumbersCountForUniqueN(Expression secondArg,
                Optional<ExprData> argData) {

        // FIXME check if the provided expression is an integer constant expression

        if (!argData.isPresent()) {
            return Optional.absent();
        }

        // Check the type of the parameter

        final Type providedType = argData.get().getType();

        if (!providedType.isIntegerType()) {
            return Optional.of(InvalidConstantFunctionCallError.invalidNumbersCountType(providedType));
        }

        // Check if the provided value is an integer or character constant

        if (!(secondArg instanceof IntegerCst) && !(secondArg instanceof CharacterCst)) {
            return Optional.of(InvalidConstantFunctionCallError.unsupportedIntegerExpression(secondArg));
        }

        return Optional.absent();
    }

    private void updateConstantFunctionIdentifier(Identifier identifier, ConstantFun fun) {
        identifier.setIsGenericReference(false);
        identifier.setUniqueName(Optional.of(fun.toString()));
        identifier.setRefsDeclInThisNescEntity(false);

        final FunctionDeclaration declarationToSet;
        final Type typeToSet;

        switch (fun) {
            case UNIQUE:
                declarationToSet = UniqueDeclaration.getInstance();
                typeToSet = UniqueDeclaration.getInstance().getType().get();
                break;
            case UNIQUEN:
                declarationToSet = UniqueNDeclaration.getInstance();
                typeToSet = UniqueNDeclaration.getInstance().getType().get();
                break;
            case UNIQUECOUNT:
                declarationToSet = UniqueCountDeclaration.getInstance();
                typeToSet = UniqueCountDeclaration.getInstance().getType().get();
                break;
            default:
                throw new RuntimeException("unexpected constant function '" + fun + "'");
        }

        identifier.setDeclaration(declarationToSet);
        identifier.setType(Optional.of(typeToSet));
    }

    /**
     * @return <code>true</code> if and only if both types are correct for the
     *         operands of the given multiplicative operator.
     */
    private boolean checkMultiplicativeOpsTypes(Type leftType, Type rightType, BinaryOp op) {
        switch(op) {
            case TIMES:
            case DIVIDE:
            case ASSIGN_TIMES:
            case ASSIGN_DIVIDE:
                return leftType.isGeneralizedArithmeticType()
                        && rightType.isGeneralizedArithmeticType();

            case MODULO:
            case ASSIGN_MODULO:
                return leftType.isGeneralizedIntegerType()
                        && rightType.isGeneralizedIntegerType();

            default:
                throw new RuntimeException("invalid multiplicative operator '" + op + "'");
        }
    }

    /**
     * @return <code>true</code> if and only if both types are correct for the
     *         operands of a shift operator.
     */
    private boolean checkShiftOpsTypes(Type leftType, Type rightType) {
        return leftType.isGeneralizedIntegerType() && rightType.isGeneralizedIntegerType();
    }

    /**
     * @return <code>true</code> if and only if both types are correct for the
     *         operands of a binary bit operator (other than a shift operator).
     */
    private boolean checkBinaryBitOpsTypes(Type leftType, Type rightType) {
        return leftType.isGeneralizedIntegerType() && rightType.isGeneralizedIntegerType();
    }

    /**
     * <p>Check if an expression with <code>rightType</code> can be assigned to
     * an expression of type <code>leftType</code> without violating any type
     * requirements.</p>
     * <p>However, one condition is relaxed. Left type can be a pointer type and
     * right type can be an integer type and it is considered correct. Though in
     * this case the right operand shall be a null pointer constant.</p>
     *
     * @return <code>true</code> if and only if operands depicted by given
     *         arguments are correct for a simple assignment expression
     *         (<code>=</code>).
     */
    private boolean checkAssignment(Type leftType, Type rightType) {
        boolean correct = false;

        if (leftType.isGeneralizedArithmeticType() && rightType.isGeneralizedArithmeticType()) {
            correct = true;
        } else if (leftType.isFieldTagType()) {
            correct = leftType.removeQualifiers().isCompatibleWith(rightType.removeQualifiers());
        } else if (leftType.isPointerType() && rightType.isPointerType()) {

            final PointerType leftPtrType = (PointerType) leftType,
                              rightPtrType = (PointerType) rightType;
            final Type leftRefType = leftPtrType.getReferencedType(),
                       rightRefType = rightPtrType.getReferencedType();
            final Type leftUnqualRefType = leftRefType.removeQualifiers(),
                       rightUnqualRefType = rightRefType.removeQualifiers();

            correct = leftRefType.hasAllQualifiers(rightRefType)
                    && (leftUnqualRefType.isCompatibleWith(rightUnqualRefType)
                        || leftRefType.isObjectType() && rightRefType.isObjectType()
                            && (leftRefType.isVoid() || rightRefType.isVoid()));
        } else if (leftType.isPointerType() && rightType.isGeneralizedIntegerType()) {
            correct = true;
        } else if (leftType.isUnknownType()) {
            correct = leftType.removeQualifiers().isCompatibleWith(rightType.removeQualifiers());
        }

        return correct;
    }

    /**
     * Check if the given types can be used for a correct pointing advancing.
     * One condition is relaxed. Only pointers to complete types shall be
     * advanced but advancing a pointer to <code>void</code> is considered
     * correct.
     *
     * @return <code>true</code> if and only if the given types can be used for
     *         a correct pointer advancing.
     */
    private boolean checkPointerAdvance(Type leftType, Type rightType) {
        boolean correct = false;

        if (leftType.isPointerType() && rightType.isGeneralizedIntegerType()) {

            final PointerType ptrType = (PointerType) leftType;
            final Type refType = ptrType.getReferencedType();

            correct = (refType.isVoid() || refType.isComplete())
                    && refType.isObjectType();
        }

        return correct;
    }

    /**
     * Check if both types are equivalent to an arithmetic type. It happens if
     * every argument of this method is either an arithmetic type or an unknown
     * arithmetic type.
     *
     * @return The object is present if and only if the check succeeded. If so,
     *         the type is the result of usual arithmetic conversions. They are
     *         simulated if an unknown arithmetic type is given.
     */
    private Optional<Type> performGeneralizedArithmeticCheck(Type leftType, Type rightType) {

        final Type result;

        if (leftType.isArithmetic() && rightType.isArithmetic()) {

            result = doUsualArithmeticConversions((ArithmeticType) leftType,
                    (ArithmeticType) rightType);

        } else if (leftType.isUnknownArithmeticType() && rightType.isUnknownArithmeticType()) {

            result = !leftType.isUnknownIntegerType() || !rightType.isUnknownIntegerType()
                    ? UnknownArithmeticType.unnamed()
                    : UnknownIntegerType.unnamed();

        } else if (leftType.isUnknownArithmeticType() || rightType.isUnknownArithmeticType()) {

            final UnknownArithmeticType unknownArithType = leftType.isUnknownArithmeticType()
                    ? (UnknownArithmeticType) leftType
                    : (UnknownArithmeticType) rightType;
            final Type otherType = unknownArithType == leftType
                    ? rightType
                    : leftType;

            if (otherType.isFloatingType()) {
                result = UnknownArithmeticType.unnamed();
            } else if (otherType.isArithmetic()) {
                result = unknownArithType.removeName();
            } else {
                result = null;
            }
        } else {
            result = null;
        }

        return Optional.fromNullable(result);
    }

    /**
     * Helper method to clarify code.
     *
     * @return Newly created carrier object with information about
     *         subexpressions of given expression.
     * @throws NullPointerException Given argument is null.
     */
    protected BinaryExprDataCarrier analyzeSubexpressions(Binary binary) {
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
    protected UnaryExprDataCarrier analyzeSubexpressions(Unary unary) {
        return new UnaryExprDataCarrier(unary);
    }

    /**
     * All expressions from the given list are analyzed and results are put into
     * the returned list. The returned list has the same size as the given list.
     *
     * @param subexpressions List with expressions to analyze.
     * @param superDecay Value that indicates if {@link ExprData#superDecay()}
     *                   is called for the data of given subexpressions.
     * @return Newly created list with results of analysis of given expressions.
     */
    protected ImmutableList<Optional<ExprData>> analyzeSubexpressions(LinkedList<? extends Expression> subexpressions,
            boolean superDecay) {

        final ImmutableList.Builder<Optional<ExprData>> builder = ImmutableList.builder();

        for (Expression expr : subexpressions) {
            final Optional<ExprData> exprData = expr.accept(this, null);

            if (superDecay && exprData.isPresent()) {
                exprData.get().superDecay();
            }

            builder.add(exprData);
        }

        return builder.build();
    }

    /**
     * <p>Set the fields of an <code>Expression</code> node that are set after
     * the analysis of an expression to values suitable for an invalid
     * expression (if an error happens the values won't be changed but when
     * everything is OK they will be overwritten in {@link ExprData#spread}).
     * </p>
     *
     * @param expr Expression to touch.
     */
    protected void touch(Expression expr) {
        expr.setType(Optional.<Type>absent());
    }

    /**
     * A simple helper class to facilitate analysis of binary expressions.
     * Reference fields are not null if and only if <code>good</code> is
     * <code>true</code>.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    protected final class BinaryExprDataCarrier {
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
    protected final class UnaryExprDataCarrier {
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

    /**
     * Simple helper class for passing the results of analysis of a function
     * call expression.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class FunctionCallReport {
        private final Optional<Type> returnType;
        private final boolean reportError;

        private static FunctionCallReport errorWithoutType() {
            return new FunctionCallReport(Optional.<Type>absent(), true);
        }

        private static FunctionCallReport withType(Type retType) {
            return new FunctionCallReport(Optional.of(retType), false);
        }

        private static FunctionCallReport empty() {
            return new FunctionCallReport(Optional.<Type>absent(), false);
        }

        private FunctionCallReport(Optional<Type> returnType, boolean reportError) {
            this.reportError = reportError;
            this.returnType = returnType;
        }
    }
}
