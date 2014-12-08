package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidComponentDerefExprError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_COMPONENTDEREF_EXPR);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidComponentDerefExprError invalidObjectKind(String identifier) {
        final String description = format("'%s' is not a component; only enumerators from components can be referenced",
                identifier);
        return new InvalidComponentDerefExprError(description);
    }

    public static InvalidComponentDerefExprError nonexistentConstant(String componentName,
            String constantName) {
        final String description = format("Component '%s' does not contain enumeration constant '%s' in its specification",
                componentName, constantName);
        return new InvalidComponentDerefExprError(description);
    }

    private InvalidComponentDerefExprError(String description) {
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
