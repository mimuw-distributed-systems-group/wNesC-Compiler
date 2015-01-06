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
public final class InvalidPointerComparisonWarning  extends BinaryExprCautionaryIssue {
    private static final WarningCode _CODE = WarningCode.onlyInstance(Issues.WarningType.INVALID_POINTER_COMPARISON);
    public static final Code CODE = _CODE;

    public InvalidPointerComparisonWarning(Type leftType, Expression leftExpr, BinaryOp op,
            Type rightType, Expression rightExpr) {
        super(_CODE, leftType, leftExpr, op, rightType, rightExpr);
        checkArgument(op == NE || op == EQ, "invalid equality operator for warning about comparison");
    }

    @Override
    public String generateDescription() {
        if (leftType.isPointerType() && rightType.isGeneralizedIntegerType()) {

            return format("Pointer '%s' of type '%s' compared to number '%s' of type '%s' with operator %s without a cast",
                          ASTWriter.writeToString(leftExpr), leftType, ASTWriter.writeToString(rightExpr), rightType, op);

        } else if (leftType.isGeneralizedIntegerType() && rightType.isPointerType()) {

            return format("Number '%s' of type '%s' compared to pointer '%s' of type '%s' with operator %s without a cast",
                        ASTWriter.writeToString(leftExpr), leftType, ASTWriter.writeToString(rightExpr), rightType, op);
        }

        return format("Invalid pointer comparison with operator %s", op);
    }
}
