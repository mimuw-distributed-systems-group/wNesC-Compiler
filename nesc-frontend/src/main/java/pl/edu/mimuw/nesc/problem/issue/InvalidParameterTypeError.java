package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

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
    private final FunctionKind funKind;
    private final ParameterKind paramKind;

    public InvalidParameterTypeError(Expression funExpr, int parameterNumber,
            Expression parameterExpr, Type expectedType, Type actualType,
            FunctionKind funKind, ParameterKind paramKind) {
        super(_CODE);

        checkNotNull(funExpr, "function expression cannot be null");
        checkNotNull(parameterExpr, "expression passed as an invalid parameter cannot be null");
        checkNotNull(expectedType, "expected type of an invalid parameter cannot be null");
        checkNotNull(actualType, "actual type of an invalid parameter cannot be null");
        checkNotNull(funKind, "kind of the function cannot be null");
        checkNotNull(paramKind, "parameter kind cannot be null");
        checkArgument(parameterNumber > 0, "number of the parameter must be positive");

        this.funExpr = funExpr;
        this.parameterNumber = parameterNumber;
        this.parameterExpr = parameterExpr;
        this.expectedType = expectedType;
        this.actualType = actualType;
        this.funKind = funKind;
        this.paramKind = paramKind;
    }

    @Override
    public String generateDescription() {
        return format("Cannot use '%s' of type '%s' as the %s %s of type '%s' for %s '%s'",
                ASTWriter.writeToString(parameterExpr), actualType, getOrdinalForm(), paramKind,
                expectedType, funKind, ASTWriter.writeToString(funExpr));
    }

    private String getOrdinalForm() {
        return IssuesUtils.getOrdinalForm(parameterNumber);
    }

    public enum FunctionKind {
        COMMAND("command"),
        EVENT("event"),
        NORMAL_FUNCTION("function"),
        ;

        private final String text;

        private FunctionKind(String text) {
            checkNotNull(text, "textual representation of a function type cannot be null");
            checkArgument(!text.isEmpty(), "textual representation of a function type cannot be an empty string");

            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public enum ParameterKind {
        INSTANCE_PARAMETER("instance parameter"),
        NORMAL_PARAMETER("parameter"),
        ;

        private final String text;

        private ParameterKind(String text) {
            checkNotNull(text, "textual representation of a parameter type cannot be null");
            checkArgument(!text.isEmpty(), "textual representation of a parameter type cannot be null");

            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
