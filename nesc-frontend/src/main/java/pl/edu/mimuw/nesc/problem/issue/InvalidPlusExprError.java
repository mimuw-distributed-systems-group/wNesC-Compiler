package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.PointerType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;

import static pl.edu.mimuw.nesc.ast.util.AstConstants.BinaryOp.*;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidPlusExprError extends BinaryExprErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_PLUS_EXPR);
    public static final Code CODE = _CODE;

    public InvalidPlusExprError(Type leftType, Expression leftExpr, Type rightType,
            Expression rightExpr) {
        super(_CODE, leftType, leftExpr, PLUS, rightType, rightExpr);
    }

    @Override
    public String generateDescription() {
        if (leftType.isPointerType() || rightType.isPointerType()) {
            final PointerType ptrType = leftType.isPointerType()
                    ? (PointerType) leftType
                    : (PointerType) rightType;
            final Type otherType = ptrType == leftType
                    ? rightType
                    : leftType;
            final Expression ptrExpr = ptrType == leftType
                    ? leftExpr
                    : rightExpr;
            final Expression otherExpr = ptrExpr == leftExpr
                    ? rightExpr
                    : leftExpr;

            if (!otherType.isGeneralizedIntegerType()) {
                return format("Operand '%s' of operator %s has type '%s' but expecting an integer type as the other operand is a pointer",
                              PrettyPrint.expression(otherExpr), op, otherType);
            }

            final Type referencedType = ptrType.getReferencedType();

            if (!referencedType.isObjectType()) {
                return format("Operand '%s' of operator %s is not a pointer to an object",
                              PrettyPrint.expression(ptrExpr), op);
            } else if (!referencedType.isComplete()) {
                return format("Operand '%s' of operator %s points to an object of incomplete type '%s'",
                              PrettyPrint.expression(ptrExpr), op, referencedType);
            }
        }

        return format("Invalid operands with types '%s' and '%s' for binary operator %s",
                      leftType, rightType, op);
    }


}
