package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;

import static java.lang.String.format;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.UnaryOp.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidNotExprError extends UnaryExprErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_NOT_EXPR);
    public static final Code CODE = _CODE;

    public InvalidNotExprError(Type argType, Expression argExpr) {
        super(_CODE, NOT, argType, argExpr);
    }

    @Override
    public String generateDescription() {
        if (!argType.isGeneralizedScalarType()) {
            return format("Operand '%s' of unary operator %s has type '%s' but expecting a scalar type",
                          PrettyPrint.expression(argExpr), op, argType);
        }

        return format("Invalid operand '%s' for unary operator %s",
                      PrettyPrint.expression(argExpr), op);
    }
}
