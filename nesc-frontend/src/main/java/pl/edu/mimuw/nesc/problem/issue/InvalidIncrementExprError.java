package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.PointerType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;

import static java.lang.String.format;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidIncrementExprError extends UnaryExprErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_INCREMENT_EXPR);
    public static final Code CODE = _CODE;

    public InvalidIncrementExprError(UnaryOp op, Type argType, Expression argExpr) {
        super(_CODE, op, argType, argExpr);
    }

    @Override
    public String generateDescription() {
        if (argType.isPointerType()) {

            final PointerType ptrType = (PointerType) argType;
            final Type refType = ptrType.getReferencedType();

            if (!refType.isComplete()) {
                return format("Cannot advance pointer '%s' because it points to incomplete type '%s'",
                              PrettyPrint.expression(argExpr), refType);
            } else if (!refType.isObjectType()) {
                return format("Cannot advance pointer '%s' because it does not point to an object",
                        PrettyPrint.expression(argExpr));
            }
        }

        return format("Invalid operand '%s' of type '%s' for operator %s",
                      PrettyPrint.expression(argExpr), argType, op);
    }
}
