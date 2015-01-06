package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.astwriting.Tokens.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidShiftExprOperandsError extends BinaryExprErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_SHIFT_EXPR_OPERANDS);
    public static final Code CODE = _CODE;

    public InvalidShiftExprOperandsError(Type leftType, Expression leftExpr, BinaryOp op,
            Type rightType, Expression rightExpr) {
        super(_CODE, leftType, leftExpr, op, rightType, rightExpr);
        checkArgument(op == BinaryOp.LSHIFT || op == BinaryOp.RSHIFT,
                      "invalid shift operator '%s'", op);
    }

    @Override
    public String generateDescription() {
        if (!leftType.isGeneralizedIntegerType()) {
            return format("Left operand '%s' of operator %s has type '%s' but expecting an integer type",
                    ASTWriter.writeToString(leftExpr), op, leftType);
        } else if (!rightType.isGeneralizedIntegerType()) {
            return format("Right operand '%s' of operator %s has type '%s' but expecting an integer type",
                    ASTWriter.writeToString(rightExpr), op, rightType);
        }

        return format("Invalid operands for operator '%s'", op);
    }
}
