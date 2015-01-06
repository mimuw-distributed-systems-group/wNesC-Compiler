package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static java.lang.String.format;
import static pl.edu.mimuw.nesc.astwriting.Tokens.UnaryOp.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidDereferenceExprError extends UnaryExprErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_DEREFERENCE_EXPR);
    public static final Code CODE = _CODE;

    public InvalidDereferenceExprError(Type argType, Expression argExpr) {
        super(_CODE, DEREFERENCE, argType, argExpr);
    }

    @Override
    public String generateDescription() {
        if (!argType.isPointerType()) {
            return format("Operand '%s' of operator %s has type '%s' but expecting a pointer type",
                          ASTWriter.writeToString(argExpr), op, argType);
        }

        return format("Invalid operand '%s' of type '%s' for operator %s",
                      ASTWriter.writeToString(argExpr), argType, op);
    }
}
