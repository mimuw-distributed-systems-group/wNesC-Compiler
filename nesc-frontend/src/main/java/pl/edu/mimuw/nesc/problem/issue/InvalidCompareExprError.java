package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.type.PointerType;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.astwriting.Tokens.*;
import static pl.edu.mimuw.nesc.astwriting.Tokens.BinaryOp.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidCompareExprError extends BinaryExprErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_COMPARE_EXPR);
    public static final Code CODE = _CODE;

    public InvalidCompareExprError(Type leftType, Expression leftExpr, BinaryOp op,
            Type rightType, Expression rightExpr) {
        super(_CODE, leftType, leftExpr, op, rightType, rightExpr);
        checkArgument(op == LEQ || op == GEQ || op == LT || op == GT,
                "invalid binary compare operator");
    }

    @Override
    public String generateDescription() {
        if (leftType.isPointerType() && rightType.isPointerType()) {

            final PointerType leftPtrType = (PointerType) leftType,
                              rightPtrType = (PointerType) rightType;
            final Type leftRefType = leftPtrType.getReferencedType().removeQualifiers(),
                       rightRefType = rightPtrType.getReferencedType().removeQualifiers();

            if (!leftRefType.isObjectType()) {
                return format("Left operand '%s' of operator %s is not a pointer to an object",
                              ASTWriter.writeToString(leftExpr), op);
            } else if (!rightRefType.isObjectType()) {
                return format("Right operand '%s' of operator %s is not a pointer to an object",
                              ASTWriter.writeToString(rightExpr), op);
            } else if (!leftRefType.isCompatibleWith(rightRefType)) {
                return format("Operands '%s' and '%s' of operator %s are pointers to incompatible types '%s' and '%s'",
                              ASTWriter.writeToString(leftExpr), ASTWriter.writeToString(rightExpr),
                              op, leftPtrType.getReferencedType(), rightPtrType.getReferencedType());
            }
        }

        return format("Invalid operands with types '%s' and '%s' for operator %s",
                      leftType, rightType, op);
    }
}
