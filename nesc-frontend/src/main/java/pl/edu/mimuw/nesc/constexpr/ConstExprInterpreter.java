package pl.edu.mimuw.nesc.constexpr;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.constexpr.value.ConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.DoubleConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.FloatConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.UnsignedIntegerConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.factory.ConstantTypeFactory;
import pl.edu.mimuw.nesc.constexpr.value.factory.IntegerConstantFactory;
import pl.edu.mimuw.nesc.constexpr.value.factory.SignedIntegerConstantFactory;
import pl.edu.mimuw.nesc.constexpr.value.factory.UnsignedIntegerConstantFactory;
import pl.edu.mimuw.nesc.constexpr.value.type.ConstantType;
import pl.edu.mimuw.nesc.constexpr.value.type.IntegerConstantType;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.FieldDeclaration;
import pl.edu.mimuw.nesc.type.ArithmeticType;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.type.TypeUtils;
import pl.edu.mimuw.nesc.typelayout.EnumeratedTypeLayoutCalculator;
import pl.edu.mimuw.nesc.typelayout.FieldTagTypeLayoutCalculator;
import pl.edu.mimuw.nesc.typelayout.TypeLayout;
import pl.edu.mimuw.nesc.typelayout.UniversalTypeLayoutCalculator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Implementation of constant expressions evaluation. It is assumed that the
 * evaluated expressions fulfill the following conditions:</p>
 * <ol>
 *     <li>values of integer and character constants are present</li>
 *     <li>all expressions have known types</li>
 * </ol>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class ConstExprInterpreter implements Interpreter {
    /**
     * ABI with information about types.
     */
    private final ABI abi;

    /**
     * Factory used for producing constant types.
     */
    private final ConstantTypeFactory typeFactory;

    /**
     * Object that will actually perform the evaluation of expressions.
     */
    private final InterpreterVisitor interpreter = new InterpreterVisitor();

    /**
     * Creates an interpreter of constant expressions that will used given
     * ABI for types information.
     *
     * @param abi ABI with necessary types information.
     */
    public ConstExprInterpreter(ABI abi) {
        checkNotNull(abi, "ABI cannot be null");
        this.abi = abi;
        this.typeFactory = new ConstantTypeFactory(abi);
    }

    @Override
    public ConstantValue evaluate(Expression expr) {
        checkNotNull(expr, "expression cannot be null");
        return expr.accept(interpreter, null);
    }

    private IntegerConstantFactory getFactoryForType(IntegerConstantType type) {
        switch (type.getType()) {
            case SIGNED_INTEGER:
                return new SignedIntegerConstantFactory(type.getBitsCount());
            case UNSIGNED_INTEGER:
                return new UnsignedIntegerConstantFactory(type.getBitsCount());
            default:
                throw new RuntimeException("unexpected type of a constant integer type '"
                        + type.getType() + "'");
        }
    }

    private IntegerConstantFactory getFactoryForType(Type type,
            Optional<ConstantType.Type> expectedType) {
        final ConstantType constantType = typeFactory.newConstantType(type);

        if (expectedType.isPresent() && constantType.getType() != expectedType.get()) {
            throw new RuntimeException("expected constant type '" + expectedType.get()
                    + "' but got '" + constantType + "'");
        } else if (constantType.getType() != ConstantType.Type.UNSIGNED_INTEGER
                && constantType.getType() != ConstantType.Type.SIGNED_INTEGER) {
            throw new RuntimeException("expected an integer constant type");
        }

        return getFactoryForType((IntegerConstantType) constantType);
    }

    /**
     * Visitor that evaluates visited expressions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class InterpreterVisitor extends ExceptionVisitor<ConstantValue, Void> {
        @Override
        public ConstantValue visitIntegerCst(IntegerCst expr, Void arg) {
            return getFactoryForType(expr.getType().get(), Optional.<ConstantType.Type>absent())
                    .newValue(expr.getValue().get());
        }

        @Override
        public ConstantValue visitCharacterCst(CharacterCst expr, Void arg) {
            return getFactoryForType(expr.getType().get(), Optional.of(ConstantType.Type.SIGNED_INTEGER))
                    .newValue(Character.getNumericValue(expr.getValue().get()));
        }

        @Override
        public ConstantValue visitFloatingCst(FloatingCst expr, Void arg) {
            final ConstantType type = typeFactory.newConstantType(expr.getType().get());
            checkArgument(type.getType() == ConstantType.Type.FLOAT
                    || type.getType() == ConstantType.Type.DOUBLE,
                    "type of a floating constant is not a floating constant type");

            final String literal = expr.getString();
            final boolean containsFloatingSuffix = literal.endsWith("L") || literal.endsWith("l")
                    || literal.endsWith("f") || literal.endsWith("F");
            final String literalToInterpret = containsFloatingSuffix
                    ? literal.substring(0, literal.length() - 1)
                    : literal;

            switch (type.getType()) {
                case FLOAT:
                    return new FloatConstantValue(Float.parseFloat(literalToInterpret));
                case DOUBLE:
                    return new DoubleConstantValue(Double.parseDouble(literalToInterpret));
                default:
                    throw new RuntimeException("unexpected floating constant type kind '"
                            + type.getType() + "'");
            }
        }

        @Override
        public ConstantValue visitIdentifier(Identifier expr, Void arg) {
            if (!(expr.getDeclaration() instanceof ConstantDeclaration)) {
                throw new RuntimeException("identifier that does not refer to an enumeration constant encountered");
            }

            final ConstantDeclaration constant = (ConstantDeclaration) expr.getDeclaration();

            if (!constant.getValue().isPresent()) {
                // Determine the value of the constant as a side effect
                new EnumeratedTypeLayoutCalculator(abi, constant.getOwner().getType(false, false))
                        .calculate();
            }

            return getFactoryForType(constant.getOwner().getCompatibleType(), Optional.<ConstantType.Type>absent())
                    .newValue(constant.getValue().get());
        }

        @Override
        public ConstantValue visitUniqueCall(UniqueCall expr, Void arg) {
            return evaluateConstantFun(expr);
        }

        @Override
        public ConstantValue visitUniqueNCall(UniqueNCall expr, Void arg) {
            return evaluateConstantFun(expr);
        }

        @Override
        public ConstantValue visitUniqueCountCall(UniqueCountCall expr, Void arg) {
            return evaluateConstantFun(expr);
        }

        @Override
        public ConstantValue visitUnaryPlus(UnaryPlus expr, Void arg) {
            return evaluateUnaryExpr(expr, UnaryArithmeticOperation.IDENTITY);
        }

        @Override
        public ConstantValue visitUnaryMinus(UnaryMinus expr, Void arg) {
            return evaluateUnaryExpr(expr, UnaryArithmeticOperation.NEGATION);
        }

        @Override
        public ConstantValue visitBitnot(Bitnot expr, Void arg) {
            return evaluateUnaryExpr(expr, UnaryArithmeticOperation.BITWISE_NOT);
        }

        @Override
        public ConstantValue visitNot(Not expr, Void arg) {
            // Get the type of the result
            final ConstantType resultConstantType = typeFactory.newConstantType(expr.getType().get());

            // Perform the operation
            final boolean resultValue = !expr.getArgument().accept(this, null).logicalValue();
            return UnsignedIntegerConstantValue.getLogicalValue(resultValue).castTo(resultConstantType);
        }

        @Override
        public ConstantValue visitSizeofType(SizeofType expr, Void arg) {
            return evaluateTypeOperator(expr.getAsttype().getType().get(), expr.getType().get(),
                    TypeOperation.SIZE);
        }

        @Override
        public ConstantValue visitSizeofExpr(SizeofExpr expr, Void arg) {
            return evaluateTypeOperator(expr.getArgument().getType().get(), expr.getType().get(),
                    TypeOperation.SIZE);
        }

        @Override
        public ConstantValue visitAlignofType(AlignofType expr, Void arg) {
            return evaluateTypeOperator(expr.getAsttype().getType().get(), expr.getType().get(),
                    TypeOperation.ALIGNMENT);
        }

        @Override
        public ConstantValue visitAlignofExpr(AlignofExpr expr, Void arg) {
            return evaluateTypeOperator(expr.getArgument().getType().get(), expr.getType().get(),
                    TypeOperation.ALIGNMENT);
        }

        @Override
        public ConstantValue visitOffsetof(Offsetof expr, Void arg) {
            int totalOffsetInBits = 0;

            for (FieldIdentifier fieldIdentifier : expr.getFieldlist()) {
                final FieldDeclaration declaration = fieldIdentifier.getDeclaration();

                if (!declaration.hasLayout()) {
                    /* One of side effects of calculating the layout of the
                       whole structure or union is setting the size, alignment
                       and offset of all of its fields. */
                    new FieldTagTypeLayoutCalculator(ConstExprInterpreter.this.abi,
                            declaration.getOwner().getType(false, false)).calculate();
                }

                totalOffsetInBits += declaration.getOffsetInBits();
            }

            return getFactoryForType(expr.getType().get(), Optional.of(ConstantType.Type.UNSIGNED_INTEGER))
                    .newValue(totalOffsetInBits / 8);
        }

        @Override
        public ConstantValue visitCast(Cast expr, Void arg) {
            final ConstantType resultType = typeFactory.newConstantType(expr.getType().get());
            return expr.getArgument().accept(this, null).castTo(resultType);
        }

        @Override
        public ConstantValue visitTimes(Times expr, Void arg) {
            return evaluateBinaryExpr(expr, ArithmeticBinaryOperation.MULTIPLICATION);
        }

        @Override
        public ConstantValue visitDivide(Divide expr, Void arg) {
            return evaluateBinaryExpr(expr, ArithmeticBinaryOperation.DIVISION);
        }

        @Override
        public ConstantValue visitModulo(Modulo expr, Void arg) {
            return evaluateBinaryExpr(expr, ArithmeticBinaryOperation.REMAINDER);
        }

        @Override
        public ConstantValue visitPlus(Plus expr, Void arg) {
            return evaluateBinaryExpr(expr, ArithmeticBinaryOperation.ADDITION);
        }

        @Override
        public ConstantValue visitMinus(Minus expr, Void arg) {
            return evaluateBinaryExpr(expr, ArithmeticBinaryOperation.SUBTRACTION);
        }

        @Override
        public ConstantValue visitLshift(Lshift expr, Void arg) {
            return evaluateShiftExpr(expr, ShiftOperation.LEFT_SHIFT);
        }

        @Override
        public ConstantValue visitRshift(Rshift expr, Void arg) {
            return evaluateShiftExpr(expr, ShiftOperation.RIGHT_SHIFT);
        }

        @Override
        public ConstantValue visitLeq(Leq expr, Void arg) {
            return evaluateBinaryExpr(expr, ArithmeticBinaryOperation.LESS_OR_EQUAL);
        }

        @Override
        public ConstantValue visitGeq(Geq expr, Void arg) {
            return evaluateBinaryExpr(expr, ArithmeticBinaryOperation.GREATER_OR_EQUAL);
        }

        @Override
        public ConstantValue visitLt(Lt expr, Void arg) {
            return evaluateBinaryExpr(expr, ArithmeticBinaryOperation.LESS);
        }

        @Override
        public ConstantValue visitGt(Gt expr, Void arg) {
            return evaluateBinaryExpr(expr, ArithmeticBinaryOperation.GREATER);
        }

        @Override
        public ConstantValue visitEq(Eq expr, Void arg) {
            return evaluateBinaryExpr(expr, ArithmeticBinaryOperation.EQUAL);
        }

        @Override
        public ConstantValue visitNe(Ne expr, Void arg) {
            return evaluateBinaryExpr(expr, ArithmeticBinaryOperation.NOT_EQUAL);
        }

        @Override
        public ConstantValue visitBitand(Bitand expr, Void arg) {
            return evaluateBinaryExpr(expr, ArithmeticBinaryOperation.BITWISE_AND);
        }

        @Override
        public ConstantValue visitBitor(Bitor expr, Void arg) {
            return evaluateBinaryExpr(expr, ArithmeticBinaryOperation.BITWISE_OR);
        }

        @Override
        public ConstantValue visitBitxor(Bitxor expr, Void arg) {
            return evaluateBinaryExpr(expr, ArithmeticBinaryOperation.BITWISE_XOR);
        }

        @Override
        public ConstantValue visitAndand(Andand expr, Void arg) {
            return evaluateLogicalExpr(expr, LogicalBinaryOperation.LOGICAL_AND);
        }

        @Override
        public ConstantValue visitOror(Oror expr, Void arg) {
            return evaluateLogicalExpr(expr, LogicalBinaryOperation.LOGICAL_OR);
        }

        @Override
        public ConstantValue visitConditional(Conditional expr, Void arg) {
            final ConstantValue conditionValue = expr.getCondition().accept(this, null);
            final ConstantType resultConstantType = typeFactory.newConstantType(expr.getType().get());
            final ConstantValue resultValue;

            if (conditionValue.logicalValue()) {
                resultValue = expr.getOnTrueExp().isPresent()
                        ? expr.getOnTrueExp().get().accept(this, null)
                        : conditionValue;
            } else {
                resultValue = expr.getOnFalseExp().accept(this, null);
            }

            return resultValue.castTo(resultConstantType);
        }

        private ConstantValue evaluateBinaryExpr(Binary expr, ArithmeticBinaryOperation operation) {
            return evaluateBinaryExpr(expr.getLeftArgument(), expr.getRightArgument(),
                    expr.getType().get(), operation);
        }

        private ConstantValue evaluateBinaryExpr(Expression lhs, Expression rhs,
                Type resultType, ArithmeticBinaryOperation operation) {
            // Evaluate subexpressions
            ConstantValue valueLhs = lhs.accept(this, null);
            ConstantValue valueRhs = rhs.accept(this, null);

            // Convert to type implied by usual arithmetic conversions
            final ArithmeticType commonType = TypeUtils.doUsualArithmeticConversions(
                    (ArithmeticType) lhs.getType().get(), (ArithmeticType) rhs.getType().get());
            final ConstantType commonConstantType = typeFactory.newConstantType(commonType);
            final ConstantType resultConstantType = typeFactory.newConstantType(resultType);
            valueLhs = valueLhs.castTo(commonConstantType);
            valueRhs = valueRhs.castTo(commonConstantType);

            // Perform the operation
            switch (operation) {
                case ADDITION:
                    return valueLhs.add(valueRhs);
                case SUBTRACTION:
                    return valueLhs.subtract(valueRhs);
                case MULTIPLICATION:
                    return valueLhs.multiply(valueRhs);
                case DIVISION:
                    return valueLhs.divide(valueRhs);
                case REMAINDER:
                    return valueLhs.remainder(valueRhs);
                case BITWISE_AND:
                    return valueLhs.bitwiseAnd(valueRhs);
                case BITWISE_OR:
                    return valueLhs.bitwiseOr(valueRhs);
                case BITWISE_XOR:
                    return valueLhs.bitwiseXor(valueRhs);
                case LESS:
                    return valueLhs.less(valueRhs).castTo(resultConstantType);
                case LESS_OR_EQUAL:
                    return valueLhs.lessOrEqual(valueRhs).castTo(resultConstantType);
                case GREATER:
                    return valueLhs.greater(valueRhs).castTo(resultConstantType);
                case GREATER_OR_EQUAL:
                    return valueLhs.greaterOrEqual(valueRhs).castTo(resultConstantType);
                case EQUAL:
                    return valueLhs.equalTo(valueRhs).castTo(resultConstantType);
                case NOT_EQUAL:
                    return valueLhs.notEqualTo(valueRhs).castTo(resultConstantType);
                default:
                    throw new RuntimeException("unexpected arithmetic binary operation '"
                            + operation + "'");
            }
        }

        private ConstantValue evaluateShiftExpr(Binary expr, ShiftOperation operation) {
            return evaluateShiftExpr(expr.getLeftArgument(), expr.getRightArgument(),
                    expr.getType().get(), operation);
        }

        private ConstantValue evaluateShiftExpr(Expression lhs, Expression rhs, Type resultType,
                    ShiftOperation operation) {
            // Evaluate subexpressions
            ConstantValue valueLhs = lhs.accept(this, null);
            ConstantValue valueRhs = rhs.accept(this, null);

            // Make integer promotions
            final ConstantType leftPromotedType = typeFactory.newConstantType(resultType);
            final ConstantType rightPromotedType = typeFactory.newConstantType(rhs.getType().get().promote());
            valueLhs = valueLhs.castTo(leftPromotedType);
            valueRhs = valueRhs.castTo(rightPromotedType);

            // Perform the operation
            switch (operation) {
                case LEFT_SHIFT:
                    return valueLhs.shiftLeft(valueRhs);
                case RIGHT_SHIFT:
                    return valueLhs.shiftRight(valueRhs);
                default:
                    throw new RuntimeException("unexpected shift operation kind '"
                            + operation + "'");
            }
        }

        private ConstantValue evaluateLogicalExpr(Binary logicalExpr, LogicalBinaryOperation operation) {
            return evaluateLogicalExpr(logicalExpr.getLeftArgument(), logicalExpr.getRightArgument(),
                    logicalExpr.getType().get(), operation);
        }

        private ConstantValue evaluateLogicalExpr(Expression lhs, Expression rhs,
                    Type resultType, LogicalBinaryOperation operation) {
            // Evaluate the first subexpression
            final ConstantValue valueLhs = lhs.accept(this, null);

            // Get the factory for the result type
            final ConstantType resultConstantType = typeFactory.newConstantType(resultType);
            checkArgument(resultConstantType.getType() == ConstantType.Type.SIGNED_INTEGER,
                    "result of logical AND or logical OR expression has not a signed integer type");
            final boolean resultValue;

            /* Determine the result - we use the fact the Java '&&' and '||'
               operators are evaluated lazily. */
            switch (operation) {
                case LOGICAL_AND:
                    resultValue = valueLhs.logicalValue() && rhs.accept(this, null).logicalValue();
                    break;
                case LOGICAL_OR:
                    resultValue = valueLhs.logicalValue() || rhs.accept(this, null).logicalValue();
                    break;
                default:
                    throw new RuntimeException("unexpected logical binary operation '"
                            + operation + "'");
            }

            return UnsignedIntegerConstantValue.getLogicalValue(resultValue).castTo(resultConstantType);
        }

        private ConstantValue evaluateUnaryExpr(Unary expr, UnaryArithmeticOperation operation) {
            return evaluateUnaryExpr(expr.getArgument(), expr.getType().get(),
                    operation);
        }

        private ConstantValue evaluateUnaryExpr(Expression opParam, Type resultType,
                    UnaryArithmeticOperation operation) {
            // Evaluate the subexpression
            ConstantValue paramValue = opParam.accept(this, null);

            // Make the integer promotion
            final ConstantType resultConstantType = typeFactory.newConstantType(resultType);
            paramValue = paramValue.castTo(resultConstantType);

            // Perform the operation
            switch (operation) {
                case IDENTITY:
                    return paramValue;
                case NEGATION:
                    return paramValue.negate();
                case BITWISE_NOT:
                    return paramValue.bitwiseNot();
                default:
                    throw new RuntimeException("unexpected unary arithmetic operation '"
                            + operation + "'");
            }
        }

        private ConstantValue evaluateTypeOperator(Type queriedType, Type resultType,
                    TypeOperation operation) {
            // Prepare for performing the operation
            final TypeLayout layout = new UniversalTypeLayoutCalculator(ConstExprInterpreter.this.abi, queriedType)
                    .calculate();
            final IntegerConstantFactory factory = getFactoryForType(resultType,
                    Optional.of(ConstantType.Type.UNSIGNED_INTEGER));

            // Perform the operation
            switch (operation) {
                case SIZE:
                    return factory.newValue(layout.getSize());
                case ALIGNMENT:
                    return factory.newValue(layout.getAlignment());
                default:
                    throw new RuntimeException("unexpected type operation '"
                            + operation + "'");
            }
        }

        private ConstantValue evaluateConstantFun(ConstantFunctionCall expr) {
            return getFactoryForType(expr.getType().get(), Optional.of(ConstantType.Type.UNSIGNED_INTEGER))
                    .newValue(expr.getValue());
        }
    }

    /**
     * Enumeration type that represents an arithmetic binary operation that
     * involves performing usual arithmetic conversions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private enum ArithmeticBinaryOperation {
        ADDITION,
        SUBTRACTION,
        MULTIPLICATION,
        DIVISION,
        REMAINDER,
        BITWISE_AND,
        BITWISE_XOR,
        BITWISE_OR,
        LESS,
        LESS_OR_EQUAL,
        GREATER,
        GREATER_OR_EQUAL,
        EQUAL,
        NOT_EQUAL,
    }

    /**
     * Enumeration type that represents shift operations. These are handled
     * differently than arithmetic operations because integer promotions of
     * operands are performed instead of usual arithmetic conversions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private enum ShiftOperation {
        LEFT_SHIFT,
        RIGHT_SHIFT,
    }

    /**
     * Logical AND and OR operations. They are performed lazily.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private enum LogicalBinaryOperation {
        LOGICAL_AND, // operator &&
        LOGICAL_OR,  // operator ||
    }

    /**
     * Unary arithmetic operations that involve performing integer promotions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private enum UnaryArithmeticOperation {
        IDENTITY,    // operator +
        NEGATION,    // operator -
        BITWISE_NOT, // operator ~
    }

    /**
     * Operations that allow getting information about types.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private enum TypeOperation {
        SIZE,      // operator sizeof
        ALIGNMENT, // operator _Alignof
    }
}
