package pl.edu.mimuw.nesc.problem.issue;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidInitializerUsageError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_INITIALIZER_USAGE);
    public static final Code CODE = _CODE;

    public InvalidInitializerUsageError() {
        super(_CODE);
    }

    @Override
    public String generateDescription() {
        return "An initializer list cannot be used in this context";
    }
}
