package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.PointerType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidConditionalExprError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_CONDITIONAL_EXPR);
    public static final Code CODE = _CODE;

    private final Type condType, trueType, falseType;
    private final Expression condExpr, trueExpr, falseExpr;

    public InvalidConditionalExprError(Type condType, Expression condExpr, Type trueType,
            Expression trueExpr, Type falseType, Expression falseExpr) {
        super(_CODE);

        checkNotNull(condType, "type of the condition cannot be null");
        checkNotNull(condExpr, "the condition expression cannot be null");
        checkNotNull(trueType, "type of the expression used if condition is true cannot be null");
        checkNotNull(trueExpr, "expression used if condition is true cannot be null");
        checkNotNull(falseType, "type of the expression used if condition is false cannot be null");
        checkNotNull(falseExpr, "expression used if condition is false cannot be null");

        this.condType = condType;
        this.condExpr = condExpr;
        this.trueType = trueType;
        this.trueExpr = trueExpr;
        this.falseType = falseType;
        this.falseExpr = falseExpr;
    }

    @Override
    public String generateDescription() {
        if (!condType.isGeneralizedScalarType()) {
            return format("Condition operand '%s' for operator ?: has type '%s' but expecting a scalar type",
                          PrettyPrint.expression(condExpr), condType);
        } else if (trueType.isFieldTagType() && falseType.isFieldTagType()) {
            return format("Middle and last operand for operator ?: are structures or unions of incompatible types '%s' and '%s'",
                          trueType, falseType);
        } else if (trueType.isVoid() && !falseType.isVoid()) {
            return format("Last operand '%s' for operator ?: has type '%s' but expecting 'void' as the middle operand has such type",
                    PrettyPrint.expression(falseExpr), falseType);
        } else if (!trueType.isVoid() && falseType.isVoid()) {
            return format("Middle operand '%s' for operator ?: has type '%s' but expecting 'void' as the last operand has such type",
                         PrettyPrint.expression(trueExpr), trueType);
        } else if (trueType.isPointerType() && falseType.isPointerType()) {

            final PointerType truePtrType = (PointerType) trueType,
                              falsePtrType = (PointerType) falseType;
            final Type trueRefType = truePtrType.getReferencedType(),
                       falseRefType = falsePtrType.getReferencedType();

            if (trueRefType.isVoid() && !falseRefType.isObjectType()) {
                return format("Last operand '%s' for operator ?: points not to an object and thus cannot be combined with a middle operand of type '%s'",
                        PrettyPrint.expression(falseExpr), trueType);
            } else if (!trueRefType.isObjectType() && falseRefType.isVoid()) {
                return format("Middle operand '%s' for operator ?: points not to an object and thus cannot be combined with a last operand of type '%s'",
                        PrettyPrint.expression(trueExpr), falseType);
            }
        }

        return format("Invalid middle and last operands of types '%s' and '%s' for operator ?:",
                      trueType, falseType);
    }
}
