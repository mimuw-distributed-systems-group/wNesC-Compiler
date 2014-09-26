package pl.edu.mimuw.nesc.problem.issue;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class AttributeUsageAsTypeError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.ATTRIBUTE_USAGE_AS_TYPE);
    public static final Code CODE = _CODE;

    public AttributeUsageAsTypeError() {
        super(_CODE);
    }

    @Override
    public String generateDescription() {
        return "Cannot use an attribute definition as a type";
    }
}
