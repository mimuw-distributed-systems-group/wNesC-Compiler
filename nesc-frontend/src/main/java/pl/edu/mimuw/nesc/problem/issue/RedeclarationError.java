package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class RedeclarationError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.REDECLARATION);
    public static final Code CODE = _CODE;

    private final String name;
    private final RedeclarationKind kind;

    public RedeclarationError(String name, RedeclarationKind kind) {
        super(_CODE);

        checkNotNull(name, "name cannot be null");
        checkNotNull(kind, "redeclared object kind cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");

        this.name = name;
        this.kind = kind;
    }

    @Override
    public String generateDescription() {
        switch (kind) {
            case FIELD:
                return format("Redeclaration of field '%s'", name);
            case LABEL:
                return format("Local label '%s' has been already declared in the current scope", name);
            default:
                return format("Redeclaration of '%s'", name);
        }
    }

    /**
     * Type of the object that has been redeclared.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum RedeclarationKind {
        FIELD,
        LABEL,
        OTHER,
    }
}
