package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InstantiationCycleError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INSTANTIATION_CYCLE);
    public static final Code CODE = _CODE;

    private final String description;

    /**
     * The constructor allows setting the message directly because it is already
     * generated in the exception class 'CyclePresentException'.
     *
     * @param description Description of the problem with cycle.
     */
    public InstantiationCycleError(String description) {
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
