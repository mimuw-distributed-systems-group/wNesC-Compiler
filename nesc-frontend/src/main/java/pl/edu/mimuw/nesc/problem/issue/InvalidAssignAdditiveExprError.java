package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.PointerType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;

import static java.lang.String.format;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.*;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.BinaryOp.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidAssignAdditiveExprError extends BinaryExprErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_ASSIGN_ADDITIVE_EXPR);
    public static final Code CODE = _CODE;

    public InvalidAssignAdditiveExprError(Type leftType, Expression leftExpr, BinaryOp op,
            Type rightType, Expression rightExpr) {
        super(_CODE, leftType, leftExpr, op, rightType, rightExpr);
    }

    @Override
    public String generateDescription() {
        if (leftType.isPointerType() && rightType.isIntegerType()) {

            final PointerType ptrType = (PointerType) leftType;
            final Type refType = ptrType.getReferencedType();

            if (!refType.isComplete()) {
                return format("Cannot advance pointer '%s' because it points to incomplete type '%s'",
                              PrettyPrint.expression(leftExpr), refType);
            } else if (!refType.isObjectType()) {
                return format("Cannot advance pointer '%s' because it does not point to an object",
                              PrettyPrint.expression(leftExpr));
            }
        }

        return format("Invalid operands of types '%s' and '%s' for operator %s",
                      leftType, rightType, op);
    }
}
