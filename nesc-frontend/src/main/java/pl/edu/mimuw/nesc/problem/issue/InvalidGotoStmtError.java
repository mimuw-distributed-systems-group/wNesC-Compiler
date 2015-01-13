package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidGotoStmtError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_GOTO_STMT);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidGotoStmtError jumpInsideAnotherAtomicStmt() {
        final String description = "Cannot jump to the inside of another atomic statement";
        return new InvalidGotoStmtError(description);
    }

    public static InvalidGotoStmtError jumpToAtomicStmtFromNonatomicArea() {
        final String description = "Cannot jump to the inside of an atomic statement";
        return new InvalidGotoStmtError(description);
    }

    private InvalidGotoStmtError(String description) {
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
