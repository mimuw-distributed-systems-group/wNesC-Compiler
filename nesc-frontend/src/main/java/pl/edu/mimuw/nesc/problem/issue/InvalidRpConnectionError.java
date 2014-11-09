package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidRpConnectionError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_RP_CONNECTION);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidRpConnectionError componentRefExpected(String name) {
        final String description = format("'%s' is not a component; the first identifier of a link wires endpoint must refer to a component", name);
        return new InvalidRpConnectionError(description);
    }

    public static InvalidRpConnectionError missingSpecificationElement(String component1, String component2) {
        final String description = component1.equals(component2)
            ? format("Components cannot be wired to themselves; expecting at least one specification element (from '%s')",
                     component1)
            : format("Components cannot be wired to themselves; expecting at least one specification element (from '%s' or '%s')",
                     component1, component2);
        return new InvalidRpConnectionError(description);
    }

    private InvalidRpConnectionError(String description) {
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
