package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.astwriting.Tokens.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidTypeQueryExprError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_TYPE_QUERY_EXPR);
    public static final Code CODE = _CODE;

    private final Type wrongType;
    private final String op;

    public InvalidTypeQueryExprError(Type wrongType, String op) {
        super(_CODE);

        checkNotNull(wrongType, "type cannot be null");
        checkNotNull(op, "type query operator name cannot be null");
        checkArgument(op.equals(OP_SIZEOF) || op.equals(OP_ALIGNOF),
                "invalid type query operator");

        this.wrongType = wrongType;
        this.op = op;
    }

    @Override
    public String generateDescription() {
        if (wrongType.isFunctionType()) {
            return format("Operator %s cannot be applied to function type '%s'",
                          op, wrongType);
        } else if (!wrongType.isComplete()) {
            return format("Operator %s cannot be applied to incomplete type '%s'",
                          op, wrongType);
        }

        return format("Operator %s cannot be applied to type '%s'", op, wrongType);
    }
}
