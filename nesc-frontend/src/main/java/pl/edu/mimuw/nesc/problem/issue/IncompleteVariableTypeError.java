package pl.edu.mimuw.nesc.problem.issue;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.type.Type;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class IncompleteVariableTypeError extends ErroneousIssue {
    private final Optional<String> variableName;
    private final Type actualType;

    public IncompleteVariableTypeError(Optional<String> variableName, Type actualType) {
        super(Issues.ErrorType.INCOMPLETE_VARIABLE_TYPE);

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
