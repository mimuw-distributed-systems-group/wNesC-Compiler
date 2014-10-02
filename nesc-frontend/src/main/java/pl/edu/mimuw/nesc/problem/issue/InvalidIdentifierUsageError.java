package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidIdentifierUsageError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_IDENTIFIER_USAGE);
    public static final Code CODE = _CODE;

    private final String identifier;

    public InvalidIdentifierUsageError(String identifier) {
        super(_CODE);
        checkNotNull(identifier, "identifier cannot be null");
        this.identifier = identifier;
    }

    @Override
    public String generateDescription() {
        return format("'%s' does not denote an object or a function", identifier);
    }
}
