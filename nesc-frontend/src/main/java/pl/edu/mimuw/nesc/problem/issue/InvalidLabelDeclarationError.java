package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidLabelDeclarationError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_LABEL_DECLARATION);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidLabelDeclarationError invalidLocation() {
        final String description = "Labels cannot be placed outside a function";
        return new InvalidLabelDeclarationError(description);
    }

    private InvalidLabelDeclarationError(String description) {
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
