package pl.edu.mimuw.nesc.constexpr;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.constexpr.value.ConstantValue;

/**
 * <p>Interface for evaluation of constant expressions.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface Interpreter {
    /**
     * Evaluate the given constant expression. Information about target ABI
     * should be provided in a way that depends on the implementing class.
     *
     * @param expr Constant expression to evaluate.
     * @return Value of the given constant expression.
     */
    ConstantValue evaluate(Expression expr);
}
