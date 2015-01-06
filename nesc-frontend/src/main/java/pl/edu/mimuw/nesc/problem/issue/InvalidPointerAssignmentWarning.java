package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static java.lang.String.format;
import static pl.edu.mimuw.nesc.astwriting.Tokens.BinaryOp.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidPointerAssignmentWarning extends BinaryExprCautionaryIssue{
    private static final WarningCode _CODE = WarningCode.onlyInstance(Issues.WarningType.INVALID_POINTER_ASSIGNMENT);
    public static final Code CODE = _CODE;

    public InvalidPointerAssignmentWarning(Type leftType, Expression leftExpr,
            Type rightType, Expression rightExpr) {
        super(_CODE, leftType, leftExpr, ASSIGN, rightType, rightExpr);
    }

    @Override
    public String generateDescription() {
        if (leftType.isPointerType() && rightType.isGeneralizedIntegerType()) {
            return format("'%s' of integer type '%s' assigned to pointer '%s' without a cast",
                         ASTWriter.writeToString(rightExpr), rightType,
                         ASTWriter.writeToString(leftExpr));
        }

        return format("Invalid assignment of '%s' of type '%s' to pointer '%s'",
                ASTWriter.writeToString(rightExpr), rightType,
                ASTWriter.writeToString(leftExpr));
    }
}
