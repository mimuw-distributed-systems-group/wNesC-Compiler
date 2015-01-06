package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.astbuilding.Declarations;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidTaskDeclarationError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_TASK_DECLARATION);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidTaskDeclarationError invalidDeclarationScope() {
        final String description = "Tasks can only be declared inside implementation scopes of modules";
        return new InvalidTaskDeclarationError(description);
    }

    public static InvalidTaskDeclarationError invalidDefinitionScope() {
        final String description = "Tasks can only be defined in implementation scopes of modules";
        return new InvalidTaskDeclarationError(description);
    }

    public static InvalidTaskDeclarationError invalidType(String name, Type actualType) {
        final String description = format("Tasks must have type '%s' but '%s' is of type '%s'",
                                          Declarations.TYPE_TASK, name, actualType);
        return new InvalidTaskDeclarationError(description);
    }

    public static InvalidTaskDeclarationError instanceParametersPresent(int paramsCount) {
        final String description = format("Tasks do not take instance parameters but %d instance parameter(s) declared",
                                          paramsCount);
        return new InvalidTaskDeclarationError(description);
    }

    private InvalidTaskDeclarationError(String description) {
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
