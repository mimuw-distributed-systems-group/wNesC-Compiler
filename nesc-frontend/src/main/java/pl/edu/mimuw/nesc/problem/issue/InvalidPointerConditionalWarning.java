package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidPointerConditionalWarning extends CautionaryIssue {
    private static final WarningCode _CODE = WarningCode.onlyInstance(Issues.WarningType.INVALID_POINTER_CONDITIONAL);
    public static final Code CODE = _CODE;

    private final Expression trueExpr, falseExpr;
    private final Type trueType, falseType;

    public InvalidPointerConditionalWarning(Type trueType, Expression trueExpr,
            Type falseType, Expression falseExpr) {
        super(_CODE);

        checkNotNull(trueType, "type of the expression if condition is true cannot be null");
        checkNotNull(trueExpr, "expression if condition is true cannot be null");
        checkNotNull(falseType, "type of the expression if condition is false cannot be null");
        checkNotNull(falseExpr, "expression if condition is false cannot be null");

        this.trueExpr = trueExpr;
        this.falseExpr = falseExpr;
        this.trueType = trueType;
        this.falseType = falseType;
    }

    @Override
    public String generateDescription() {
        if (trueType.isPointerType() && falseType.isGeneralizedIntegerType()) {
            return format("A pointer and an integer combined as the middle and the last operand for operator ?: ('%s' and '%s')",
                          trueType, falseType);
        } else if (trueType.isGeneralizedIntegerType() && falseType.isPointerType()) {
            return format("An integer and a pointer combined as the middle and the last operand for operator ?: ('%s' and '%s')",
                          trueType, falseType);
        }

        return format("Suspicious usage of a pointer as an operand for operator ?:");
    }
}
