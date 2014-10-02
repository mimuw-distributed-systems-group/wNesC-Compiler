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
public final class InvalidBinaryBitExprError extends BinaryExprErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_BINARY_BIT_EXPR);
    public static final Code CODE = _CODE;

    public InvalidBinaryBitExprError(Type leftType, Expression leftExpr, BinaryOp op, Type rightType, Expression rightExpr) {
        super(_CODE, leftType, leftExpr, op, rightType, rightExpr);
        checkArgument(op == BITAND || op == BITOR || op == BITXOR,
                "invalid binary bit operator");
    }

    @Override
    public String generateDescription() {
        if (!leftType.isIntegerType()) {
            return format("Left operand '%s' of bitwise operator %s has type '%s' but expecting an integer type",
                          PrettyPrint.expression(leftExpr), op, leftType);
        } else if (!rightType.isIntegerType()) {
            return format("Right operand '%s' of bitwise operator %s has type '%s' but expecting an integer type",
                          PrettyPrint.expression(rightExpr), op, rightType);
        }

        return format("Invalid operands of types '%s' and '%s' for bitwise operator %s",
                      leftType, rightType, op);
    }
}
