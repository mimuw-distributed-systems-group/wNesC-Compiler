package pl.edu.mimuw.nesc.problem.issue;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class IncompleteVariableTypeError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INCOMPLETE_VARIABLE_TYPE);
    public static final Code CODE = _CODE;

    private final Optional<String> variableName;
    private final Type actualType;

    public IncompleteVariableTypeError(Optional<String> variableName, Type actualType) {
        super(_CODE);

        checkNotNull(variableName, "variable name cannot be null");
        checkNotNull(actualType, "actual type cannot be null");

        this.variableName = variableName;
        this.actualType = actualType;
    }

    @Override
    public String generateDescription() {
        return   variableName.isPresent()
               ? format("Variable '%s' has incomplete type '%s'",
                        variableName.get(), actualType)
               : format("Variable has incomplete type '%s'",
                         actualType);
    }
}
