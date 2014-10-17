package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.type.VoidType;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidCastExprError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_CAST_EXPR);
    public static final Code CODE = _CODE;

    private final Type argType;
    private final Expression argExpr;
    private final Type destType;

    public InvalidCastExprError(Type destType, Type argType, Expression argExpr) {
        super(_CODE);

        checkNotNull(destType, "destination type of a cast cannot be null");
        checkNotNull(argType, "type of the cast argument cannot be null");
        checkNotNull(argExpr, "subexpression of a cast expression cannot be null");

        this.argType = argType;
        this.argExpr = argExpr;
        this.destType = destType;
    }

    @Override
    public String generateDescription() {
        if (!destType.isVoid() && !destType.isGeneralizedScalarType()) {
            return format("Cannot cast to type '%s'; expecting '%s' or a scalar type", destType,
                          new VoidType());
        } else if (!argType.isGeneralizedScalarType()) {
            return format("Cannot cast expression '%s' of type '%s'; expecting a scalar type",
                         PrettyPrint.expression(argExpr), argType);
        } else if (argType.isPointerType() && destType.isFloatingType()) {
            return format("Cannot cast expression of a pointer type to a floating type ('%s' to '%s')",
                          argType, destType);
        } else if (argType.isFloatingType() && destType.isPointerType()) {
            return format("Cannot cast expression of a floating type to a pointer type ('%s' to '%s')",
                          argType, destType);
        } else if (argType.isPointerType() && destType.isUnknownArithmeticType()
                && !destType.isUnknownIntegerType()) {

            return format("Cannot cast expression of a pointer type to an unknown arithmetic type ('%s' to '%s')",
                          argType, destType);

        } else if (argType.isUnknownArithmeticType() && !argType.isUnknownIntegerType()
                && destType.isPointerType()) {

            return format("Cannot cast expression of an unknown arithmetic type to a pointer type ('%s' to '%s')",
                          argType, destType);
        }

        return format("Invalid cast of expression '%s' of type '%s' to '%s'",
                      PrettyPrint.expression(argExpr), argType, destType);
    }
}
