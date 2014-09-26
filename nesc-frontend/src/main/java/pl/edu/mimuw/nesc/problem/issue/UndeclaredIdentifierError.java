package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UndeclaredIdentifierError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.UNDECLARED_IDENTIFIER);
    public static final Code CODE = _CODE;

    private final String identifier;

    public UndeclaredIdentifierError(String identifier) {
        super(_CODE);
        checkNotNull(identifier, "the identifier cannot be null");
        this.identifier = identifier;
    }

    @Override
    public String generateDescription() {
        return format("Usage of undeclared identifier '%s'", identifier);
    }
}
