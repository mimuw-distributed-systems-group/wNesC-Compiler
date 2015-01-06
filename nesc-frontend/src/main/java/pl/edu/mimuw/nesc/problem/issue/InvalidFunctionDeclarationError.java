package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidFunctionDeclarationError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_FUNCTION_DECLARATION);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidFunctionDeclarationError instanceParametersPresent(String name) {
        final String description = format("Regular function '%s' cannot have instance parameters",
                name);
        return new InvalidFunctionDeclarationError(description);
    }

    public static InvalidFunctionDeclarationError kindMismatch(String name) {
        final String description = format("'%s' has been previously declared with a different storage-class",
                name);
        return new InvalidFunctionDeclarationError(description);
    }

    public static InvalidFunctionDeclarationError typeMismatch(String name, Type previousType, Type actualType) {
        final String description = format("Type '%s' of '%s' differs from its type '%s' in a previous declaration",
                actualType, name, previousType);
        return new InvalidFunctionDeclarationError(description);
    }

    private InvalidFunctionDeclarationError(String description) {
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
