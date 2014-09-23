package pl.edu.mimuw.nesc.problem.issue;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class AttributeUsageAsTypeError extends ErroneousIssue {
    public AttributeUsageAsTypeError() {
        super(Issues.ErrorType.ATTRIBUTE_USAGE_AS_TYPE);
    }

    @Override
    public String generateDescription() {
        return "Cannot use an attribute definition as a type";
    }
}
