package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidBareEntityDeclarationError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_BARE_ENTITY_DECLARATION);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidBareEntityDeclarationError invalidScope() {
        final String description = "Commands and events can only be declared in interfaces or as used or provided in specifications of modules";
        return new InvalidBareEntityDeclarationError(description);
    }

    private InvalidBareEntityDeclarationError(String description) {
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
