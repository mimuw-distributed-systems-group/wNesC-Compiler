package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnplacedLabelError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.UNPLACED_LABEL);
    public static final Code CODE = _CODE;

    private final String description;

    public static UnplacedLabelError undefinedLocalLabel(String labelName) {
        return new UnplacedLabelError(format("Local label '%s' is declared but not placed inside the block of its declaration",
                labelName));
    }

    public static UnplacedLabelError undeclaredLabel(String labelName) {
        return new UnplacedLabelError(format("Label '%s' is used but not placed", labelName));
    }

    private UnplacedLabelError(String description) {
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
