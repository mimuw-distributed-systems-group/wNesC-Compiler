package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TagRedefinitionError extends ErroneousIssue {
    private final String tag;
    private final boolean nestedRedefinition;

    public TagRedefinitionError(String tag, boolean nested) {
        super(Issues.ErrorType.TAG_REDEFINITION);

        checkNotNull(tag, "tag cannot be null");
        checkArgument(!tag.isEmpty(), "tag cannot be an empty string");

        this.tag = tag;
        this.nestedRedefinition = nested;
    }

    @Override
    public String generateDescription() {
        return   nestedRedefinition
               ? format("Beginning of nested definition of tag '%s'", tag)
               : format("Tag '%s' has been already defined", tag);
    }
}
