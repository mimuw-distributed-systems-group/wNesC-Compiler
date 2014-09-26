package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ConflictingTagKindError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.CONFLICTING_TAG_KIND);
    public static final Code CODE = _CODE;

    private final String tag;

    public ConflictingTagKindError(String tag) {
        super(_CODE);

        checkNotNull(tag, "tag cannot be null");
        checkArgument(!tag.isEmpty(), "tag cannot be an empty string");

        this.tag = tag;
    }

    @Override
    public String generateDescription() {
        return format("'%s' has been previously declared as a tag of another type", tag);
    }
}
