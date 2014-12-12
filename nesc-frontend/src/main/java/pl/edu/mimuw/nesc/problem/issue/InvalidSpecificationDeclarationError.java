package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidSpecificationDeclarationError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_SPECIFICATION_DECLARATION);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidSpecificationDeclarationError expectedBareEntity() {
        final String description = "Only commands, events and interfaces can be declared as used or provided";
        return new InvalidSpecificationDeclarationError(description);
    }

    private InvalidSpecificationDeclarationError(String description) {
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
