package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.type.Type;

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

    public static InvalidTaskDeclarationError invalidDefinitionScope() {
        final String description = "Tasks can only be defined in implementation scopes of modules";
        return new InvalidTaskDeclarationError(description);
    }

    public static InvalidTaskDeclarationError variableArgumentsPresent() {
        final String description = "Tasks cannot be declared as variable arguments functions";
        return new InvalidTaskDeclarationError(description);
    }

    public static InvalidTaskDeclarationError parametersPresent(int paramsCount) {
        final String description = format("Tasks do not take any parameters but %d parameter(s) declared",
                                          paramsCount);
        return new InvalidTaskDeclarationError(description);
    }

    public static InvalidTaskDeclarationError invalidReturnType(Type returnType) {
        final String description = format("Tasks do not return any values but '%s' declared as the return type",
                returnType);
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
