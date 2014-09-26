package pl.edu.mimuw.nesc.problem.issue;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidInstanceParamSpecifiersError extends ErroneousIssue {

    public InvalidInstanceParamSpecifiersError() {
        super(Issues.ErrorType.INVALID_INSTANCE_PARAM_SPECIFIERS);
    }

    @Override
    public String generateDescription() {
        return "Cannot use non-type specifiers other than 'norace' in an instance parameter declaration";
    }
}
