package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.type.Type;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidFunctionReturnTypeError extends ErroneousIssue {
    private final Type functionReturnType;

    public InvalidFunctionReturnTypeError(Type functionReturnType) {
        super(Issues.ErrorType.INVALID_FUNCTION_RETURN_TYPE);
        checkNotNull(functionReturnType, "function return type cannot be null");
        this.functionReturnType = functionReturnType;
    }

    @Override
    public String generateDescription() {
        if (functionReturnType.isArrayType()) {
            return format("A function cannot return a value of an array type '%s'",
                          functionReturnType);
        } else if (functionReturnType.isFunctionType()) {
            return format("A function cannot return a value of a function type '%s'",
                          functionReturnType);
        }

        return format("Invalid function return type '%s'", functionReturnType);
    }
}
