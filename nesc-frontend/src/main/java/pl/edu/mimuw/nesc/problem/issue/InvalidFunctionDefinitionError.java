package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.type.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidFunctionDefinitionError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_FUNCTION_DEFINITION);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidFunctionDefinitionError instanceParametersPresent(String name) {
        final String description = format("Regular function '%s' cannot have instance parameters",
                name);
        return new InvalidFunctionDefinitionError(description);
    }

    public static InvalidFunctionDefinitionError kindMismatch(String name) {
        final String description = format("'%s' has been previously declared with a different storage-class",
                name);
        return new InvalidFunctionDefinitionError(description);
    }

    public static InvalidFunctionDefinitionError typeMismatch(String name, Type previousType, Type actualType) {
        final String description = format("Type '%s' of '%s' differs from its type '%s' in a previous declaration",
                actualType, name, previousType);
        return new InvalidFunctionDefinitionError(description);
    }

    private InvalidFunctionDefinitionError(String description) {
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
