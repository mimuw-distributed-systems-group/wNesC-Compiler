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
public final class InvalidCompoundAssignExprError extends BinaryExprErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_COMPOUND_ASSIGN_EXPR);
    public static final Code CODE = _CODE;

    public InvalidCompoundAssignExprError(Type leftType, Expression leftExpr, BinaryOp op, Type rightType, Expression rightExpr) {
        super(_CODE, leftType, leftExpr, op, rightType, rightExpr);

        checkArgument(op == ASSIGN_TIMES || op == ASSIGN_MODULO || op == ASSIGN_DIVIDE
                      || op == ASSIGN_LSHIFT || op == ASSIGN_RSHIFT
                      || op == ASSIGN_BITAND || op == ASSIGN_BITOR || op == ASSIGN_BITXOR,
                      "invalid compound assignment operator '" + op + "'");
    }

    @Override
    public String generateDescription() {

        switch (op) {
            case ASSIGN_TIMES:
            case ASSIGN_DIVIDE:
                if (!leftType.isGeneralizedArithmeticType()) {
                    return format("Left operand '%s' for operator %s has type '%s' but expecting an arithmetic type",
                                  ASTWriter.writeToString(leftExpr), op, leftType);
                } else if (!rightType.isGeneralizedArithmeticType()) {
                    return format("Right operand '%s' for operator %s has type '%s' but expecting an arithmetic type",
                                  ASTWriter.writeToString(rightExpr), op, rightType);
                }
                break;

            default:
                if (!leftType.isGeneralizedIntegerType()) {
                    return format("Left operand '%s' for operator %s has type '%s' but expecting an integer type",
                                  ASTWriter.writeToString(leftExpr), op, leftType);
                } else if (!rightType.isGeneralizedIntegerType()) {
                    return format("Right operand '%s' for operator %s has type '%s' but expecting an integer type",
                                 ASTWriter.writeToString(rightExpr), op, rightType);
                }
        }

        return format("Invalid operands of types '%s' and '%s' for operator %s",
                      leftType, rightType, op);
    }
}
