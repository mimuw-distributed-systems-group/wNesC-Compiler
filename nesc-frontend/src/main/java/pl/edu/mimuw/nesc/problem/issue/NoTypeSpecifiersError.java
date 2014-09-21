package pl.edu.mimuw.nesc.problem.issue;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NoTypeSpecifiersError extends ErroneousIssue {
    public NoTypeSpecifiersError() {
        super(Issues.ErrorType.NO_TYPE_SPECIFIERS);
    }

    @Override
    public String generateDescription() {
        return "Expecting a type specifier";
    }
}
