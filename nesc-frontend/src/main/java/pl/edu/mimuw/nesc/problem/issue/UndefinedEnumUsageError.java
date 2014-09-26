package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UndefinedEnumUsageError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.UNDEFINED_ENUM_USAGE);
    public static final Code CODE = _CODE;

    private final String tag;

    public UndefinedEnumUsageError(String tag) {
        super(_CODE);

        checkNotNull(tag, "tag cannot be null");
        checkArgument(!tag.isEmpty(), "tag cannot be an empty string");

        this.tag = tag;
    }

    @Override
    public String generateDescription() {
        return format("'%s' is undefined; cannot use an enumeration type with no visible definition", tag);
    }
}
