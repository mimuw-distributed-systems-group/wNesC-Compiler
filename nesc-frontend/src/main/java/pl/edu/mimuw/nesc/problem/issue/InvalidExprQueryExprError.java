package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidExprQueryExprError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_EXPR_QUERY_EXPR);
    public static final Code CODE = _CODE;

    private final Expression arg;
    private final Type argType;
    private final String op;
    private final boolean isBitField;

    public InvalidExprQueryExprError(Expression opArg, Type argType, boolean isBitField, String op) {
        super(_CODE);

        checkNotNull(opArg, "argument of the operator cannot be null");
        checkNotNull(argType, "type of the argument cannot be null");
        checkNotNull(op, "operator name cannot be null");
        checkArgument(op.equals(OP_SIZEOF) || op.equals(OP_ALIGNOF),
                "invalid expression query operator");

        this.arg = opArg;
        this.argType = argType;
        this.op = op;
        this.isBitField = isBitField;
    }

    @Override
    public String generateDescription() {
        if (isBitField) {
            return format("Operator %s cannot be applied to bit-field designated by expression '%s'",
                          op, PrettyPrint.expression(arg));
        } else if (argType.isFunctionType()) {
            return format("Operator %s cannot be applied to expression '%s' which has function type '%s'.",
                          op, PrettyPrint.expression(arg), argType);
        } else if (!argType.isComplete()) {
            return format("Operator %s cannot be applied to expression '%s' which has incomplete type '%s'",
                          op, PrettyPrint.expression(arg), argType);
        }

        return format("Operator %s cannot be applied to expression '%s'",
                      op, PrettyPrint.expression(arg));
    }
}
