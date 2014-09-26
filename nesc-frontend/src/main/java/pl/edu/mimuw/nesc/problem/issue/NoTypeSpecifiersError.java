package pl.edu.mimuw.nesc.problem.issue;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NoTypeSpecifiersError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.NO_TYPE_SPECIFIERS);
    public static final Code CODE = _CODE;

    public NoTypeSpecifiersError() {
        super(_CODE);
    }

    @Override
    public String generateDescription() {
        return "Expecting a type specifier";
    }
}
