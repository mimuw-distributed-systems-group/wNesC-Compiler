package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidParameterTypeError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_PARAMETER_TYPE);
    public static final Code CODE = _CODE;

    private final int parameterNumber;
    private final Expression funExpr;
    private final Expression parameterExpr;
    private final Type expectedType;
    private final Type actualType;

    public InvalidParameterTypeError(Expression funExpr, int parameterNumber,
            Expression parameterExpr, Type expectedType, Type actualType) {
        super(_CODE);

        checkNotNull(funExpr, "function expression cannot be null");
        checkNotNull(parameterExpr, "expression passed as an invalid parameter cannot be null");
        checkNotNull(expectedType, "expected type of an invalid parameter cannot be null");
        checkNotNull(actualType, "actual type of an invalid parameter cannot be null");
        checkArgument(parameterNumber > 0, "number of the parameter must be positive");

        this.funExpr = funExpr;
        this.parameterNumber = parameterNumber;
        this.parameterExpr = parameterExpr;
        this.expectedType = expectedType;
        this.actualType = actualType;
    }

    @Override
    public String generateDescription() {
        return format("Cannot use '%s' of type '%s' as the %s parameter for function '%s' with declared type '%s'",
                PrettyPrint.expression(parameterExpr), actualType, getOrdinalForm(),
                PrettyPrint.expression(funExpr), expectedType);
    }

    private String getOrdinalForm() {
        switch (parameterNumber) {
            case 1:
                return "1st";
            case 2:
                return "2nd";
            case 3:
                return "3rd";
            default:
                return parameterNumber + "th";
        }
    }
}
