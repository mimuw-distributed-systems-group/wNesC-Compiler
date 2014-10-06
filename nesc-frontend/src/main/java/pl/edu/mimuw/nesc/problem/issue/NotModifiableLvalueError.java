package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.PointerType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.*;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.BinaryOp.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NotModifiableLvalueError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.NOT_MODIFIABLE_LVALUE);
    public static final Code CODE = _CODE;

    private final Expression expr;
    private final Type type;
    private final boolean isLvalue;

    public NotModifiableLvalueError(Type type, Expression expr, boolean isLvalue) {
        super(_CODE);

        checkNotNull(expr, "expression cannot be null");
        checkNotNull(type, "type of the expression cannot be null");

        this.expr = expr;
        this.type = type;
        this.isLvalue = isLvalue;
    }

    @Override
    public String generateDescription() {
        if (!isLvalue) {
            return format("Cannot assign to '%s' because it is not an lvalue",
                    PrettyPrint.expression(expr));
        } else if (!type.isComplete()) {
            return format("Cannot assign to '%s' because it has incomplete type '%s'",
                    PrettyPrint.expression(expr), type);
        } else if (type.isArrayType()) {
            return format("Cannot assign to '%s' because it has array type '%s'",
                    PrettyPrint.expression(expr), type);
        } else if (!type.isModifiable()) {
            return format("Cannot assign to '%s' because it violates a 'const' qualifier",
                    PrettyPrint.expression(expr));
        }

        return format("Cannot assign to '%s'", PrettyPrint.expression(expr));
    }
}
