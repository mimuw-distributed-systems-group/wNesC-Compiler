package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;

import static java.lang.String.format;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.UnaryOp.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidAddressOfExprError extends UnaryExprErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_ADDRESSOF_EXPR);
    public static final Code CODE = _CODE;

    private final boolean isLvalue;
    private final boolean isBitField;
    private final boolean inRegister;

    public InvalidAddressOfExprError(Type argType, Expression argExpr, boolean isLvalue,
            boolean isBitField, boolean inRegister) {
        super(_CODE, ADDRESSOF, argType, argExpr);

        this.isLvalue = isLvalue;
        this.isBitField = isBitField;
        this.inRegister = inRegister;
    }

    @Override
    public String generateDescription() {
        if (!argType.isFunctionType() && !isLvalue) {
            return format("Cannot use expression '%s' as the operand for operator %s because it "
                          + "is neither an lvalue nor a function designator",
                          PrettyPrint.expression(argExpr), op);
        } else if (argType.isObjectType() && isLvalue) {
            if (isBitField) {
                return format("Cannot use expression '%s' as the operand for operator %s because it "
                                + "designates a bit-field",
                             PrettyPrint.expression(argExpr), op);
            } else if (inRegister) {
                return format("Cannot use expression '%s' as the operand for operator %s because it "
                        + "designates an object in a register",
                        PrettyPrint.expression(argExpr), op);
            }
        }

        return format("Invalid operand '%s' for operator %s", PrettyPrint.expression(argExpr), op);
    }
}
