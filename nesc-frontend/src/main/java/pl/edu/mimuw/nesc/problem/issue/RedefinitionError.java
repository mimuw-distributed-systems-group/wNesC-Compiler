package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class RedefinitionError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.REDEFINITION);
    public static final Code CODE = _CODE;

    private final String name;
    private final RedefinitionKind kind;

    public RedefinitionError(String name, RedefinitionKind kind) {
        super(_CODE);

        checkNotNull(name, "name cannot be null");
        checkNotNull(kind, "redefinition kind cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");

        this.name = name;
        this.kind = kind;
    }

    @Override
    public String generateDescription() {

        switch (kind) {
            case NESTED_TAG:
                return format("Beginning of nested definition of tag '%s'", name);
            case TAG:
                return format("Tag '%s' has been already defined", name);
            case FUNCTION:
                return format("Function '%s' has been already defined", name);
            default:
                return format("Redefinition of '%s'", name);
        }

    }

    /**
     * Kind of the redefinition.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum RedefinitionKind {
        TAG,
        NESTED_TAG,
        FUNCTION,
        OTHER,
    }
}
