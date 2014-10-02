package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.*;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.BinaryOp.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidBinaryLogicalExprError extends BinaryExprErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_BINARY_LOGICAL_EXPR);
    public static final Code CODE = _CODE;

    public InvalidBinaryLogicalExprError(Type leftType, Expression leftExpr, BinaryOp op,
            Type rightType, Expression rightExpr) {
        super(_CODE, leftType, leftExpr, op, rightType, rightExpr);
        checkArgument(op == ANDAND || op == OROR, "invalid binary logical operator");
    }

    @Override
    public String generateDescription() {
        if (!leftType.isScalarType()) {
            return format("Left operand '%s' of operator %s has type '%s' but expecting a scalar type",
                          PrettyPrint.expression(leftExpr), op, leftType);
        } else if (!rightType.isScalarType()) {
            return format("Right operand '%s' of operator %s has type '%s' but expecting a scalar type",
                          PrettyPrint.expression(rightExpr), op, rightType);
        }

        return format("Invalid operands of types '%s' and '%s' for operator %s",
                      leftType, rightType, op);
    }
}
