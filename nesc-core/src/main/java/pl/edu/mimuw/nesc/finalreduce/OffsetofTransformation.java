package pl.edu.mimuw.nesc.finalreduce;

import java.util.LinkedList;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Cast;
import pl.edu.mimuw.nesc.ast.gen.ExprTransformation;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.Offsetof;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.constexpr.ConstExprInterpreter;
import pl.edu.mimuw.nesc.constexpr.Interpreter;
import pl.edu.mimuw.nesc.constexpr.value.ConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.UnsignedIntegerConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.type.ConstantType;
import pl.edu.mimuw.nesc.type.TypeUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Transformation that causes evaluation of 'offsetof' macro - each usage of
 * 'offsetof' macro is replaced with its value.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class OffsetofTransformation implements ExprTransformation<Void> {
    /**
     * Interpreter of constant expressions used to evaluate values of
     * 'offsetof'.
     */
    private final Interpreter interpreter;

    /**
     * ABI used to determine 'size_t' type.
     */
    private final ABI abi;

    /**
     * Initializes this transformation to use given ABI for computing values of
     * 'offsetof'.
     *
     * @param abi ABI to use.
     */
    public OffsetofTransformation(ABI abi) {
        checkNotNull(abi, "ABI cannot be null");
        this.interpreter = new ConstExprInterpreter(abi);
        this.abi = abi;
    }

    @Override
    public LinkedList<Expression> transform(Expression expr, Void arg) {
        if (expr instanceof Offsetof) {
            // Evaluate offsetof expression
            final ConstantValue value = interpreter.evaluate(expr);
            if (value.getType().getType() != ConstantType.Type.UNSIGNED_INTEGER) {
                throw new RuntimeException("unexpected type of the value of offsetof expression '"
                        + value.getType().getType() + "'");
            }
            final UnsignedIntegerConstantValue valueInt = (UnsignedIntegerConstantValue) value;

            // Prepare the replacement expression
            final Expression constantExpr = AstUtils.newIntegerConstant(valueInt.getValue());
            final Cast finalExpr = new Cast(
                    Location.getDummyLocation(),
                    constantExpr,
                    TypeUtils.newIntegerType(this.abi.getSizeT()).toAstType()
            );
            finalExpr.setParenthesesCount(1);

            return Lists.<Expression>newList(finalExpr);
        } else {
            return Lists.newList(expr);
        }
    }
}
