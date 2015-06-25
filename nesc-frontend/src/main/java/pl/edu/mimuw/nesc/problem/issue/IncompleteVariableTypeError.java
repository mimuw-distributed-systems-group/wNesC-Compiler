package pl.edu.mimuw.nesc.problem.issue;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class IncompleteVariableTypeError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INCOMPLETE_VARIABLE_TYPE);
    public static final Code CODE = _CODE;

    private final String description;

    public static IncompleteVariableTypeError variableOfIncompleteType(Optional<String> variableName,
            Type actualType) {
        checkNotNull(variableName, "variable name cannot be null");
        checkNotNull(actualType, "actual type cannot be null");
        final String description = variableName.isPresent()
                ? format("Variable '%s' has incomplete type '%s'", variableName.get(), actualType)
                : format("Variable has incomplete type '%s'", actualType);
        return new IncompleteVariableTypeError(description);
    }

    public static IncompleteVariableTypeError initializedArrayOfUnknownSize(Optional<String> arrayName) {
        checkNotNull(arrayName, "array name cannot be null");
        final String arrayText = arrayName.isPresent()
                ? "array '" + arrayName.get() + "'"
                : "the array";
        final String description = format("Computing the size of an array from initializer list has not been implemented yet, specify the size of %s directly in brackets", arrayText);
        return new IncompleteVariableTypeError(description);
    }

    private IncompleteVariableTypeError(String description) {
        super(_CODE);
        checkNotNull(description, "description cannot be null");
        checkArgument(!description.isEmpty(), "description cannot be an empty string");
        this.description = description;
    }

    @Override
    public String generateDescription() {
        return description;
    }
}
