package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidComponentTyperefError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_COMPONENT_TYPEREF);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidComponentTyperefError expectedComponentRef(String name) {
        final String description = format("'%s' is not a component", name);
        return new InvalidComponentTyperefError(description);
    }

    public static InvalidComponentTyperefError nonexistentTypedefReferenced(String componentName,
            String typedefName) {
        final String description = format("Component '%s' does not contain typedef '%s' in its specification",
                componentName, typedefName);
        return new InvalidComponentTyperefError(description);
    }

    private InvalidComponentTyperefError(String description) {
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
