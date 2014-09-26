package pl.edu.mimuw.nesc.problem.issue;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidInstanceParamSpecifiersError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_INSTANCE_PARAM_SPECIFIERS);
    public static final Code CODE = _CODE;

    public InvalidInstanceParamSpecifiersError() {
        super(_CODE);
    }

    @Override
    public String generateDescription() {
        return "Cannot use non-type specifiers other than 'norace' in an instance parameter declaration";
    }
}
