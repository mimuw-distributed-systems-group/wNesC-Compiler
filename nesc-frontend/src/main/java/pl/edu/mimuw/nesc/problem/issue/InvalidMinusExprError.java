package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.type.PointerType;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static java.lang.String.format;
import static pl.edu.mimuw.nesc.astwriting.Tokens.BinaryOp.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidMinusExprError extends BinaryExprErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_MINUS_EXPR);
    public static final Code CODE = _CODE;

    public InvalidMinusExprError(Type leftType, Expression leftExpr, Type rightType, Expression rightExpr) {
        super(_CODE, leftType, leftExpr, MINUS, rightType, rightExpr);
    }

    @Override
    public String generateDescription() {
        if (leftType.isPointerType() && rightType.isPointerType()) {

            final PointerType leftPtrType = (PointerType) leftType,
                              rightPtrType = (PointerType) rightType;
            final Type leftRefType = leftPtrType.getReferencedType().removeQualifiers(),
                       rightRefType = rightPtrType.getReferencedType().removeQualifiers();

            if (!leftRefType.isComplete()) {
                return format("Left operand '%s' of operator %s is a pointer to incomplete type '%s'",
                              ASTWriter.writeToString(leftExpr), op, leftPtrType.getReferencedType());
            } else if (!leftRefType.isObjectType()) {
                return format("Left operand '%s' of operator %s is not a pointer to an object",
                              ASTWriter.writeToString(leftExpr), op);
            } else if (!rightRefType.isComplete()) {
                return format("Right operand '%s' of operator %s is a pointer to incomplete type '%s'",
                              ASTWriter.writeToString(rightExpr), op, rightPtrType.getReferencedType());
            } else if (!rightRefType.isObjectType()) {
                return format("Right operand '%s' of operator %s is not a pointer to an object",
                              ASTWriter.writeToString(rightExpr), op);
            } else if (!leftRefType.isCompatibleWith(rightRefType)) {
                return format("Operands '%s' and '%s' of operator %s are pointers to incompatible types '%s' and '%s'",
                             ASTWriter.writeToString(leftExpr), ASTWriter.writeToString(rightExpr),
                             op, leftPtrType.getReferencedType(), rightPtrType.getReferencedType());
            }
        } else if (leftType.isPointerType()) {

            if (!rightType.isGeneralizedIntegerType()) {
                return format("Right operand '%s' of operator %s has type '%s' but expecting an integer type as the left operand is a pointer",
                              ASTWriter.writeToString(rightExpr), op, rightType);
            }

            final PointerType leftPtrType = (PointerType) leftType;
            final Type leftRefType = leftPtrType.getReferencedType();

            if (!leftRefType.isComplete()) {
                return format("Left operand '%s' of operator %s is a pointer to incomplete type '%s'",
                              ASTWriter.writeToString(leftExpr), op, leftRefType);
            } else if (!leftRefType.isObjectType()) {
                return format("Left operand '%s' of operator %s is not a pointer to an object",
                              ASTWriter.writeToString(leftExpr), op);
            }
        }

        return format("Invalid operands with types '%s' and '%s' for binary operator %s",
                      leftType, rightType, op);
    }
}
