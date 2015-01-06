package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.astwriting.Tokens.*;
import static pl.edu.mimuw.nesc.astwriting.Tokens.BinaryOp.*;

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
        if (!leftType.isGeneralizedScalarType()) {
            return format("Left operand '%s' of operator %s has type '%s' but expecting a scalar type",
                          ASTWriter.writeToString(leftExpr), op, leftType);
        } else if (!rightType.isGeneralizedScalarType()) {
            return format("Right operand '%s' of operator %s has type '%s' but expecting a scalar type",
                          ASTWriter.writeToString(rightExpr), op, rightType);
        }

        return format("Invalid operands of types '%s' and '%s' for operator %s",
                      leftType, rightType, op);
    }
}
