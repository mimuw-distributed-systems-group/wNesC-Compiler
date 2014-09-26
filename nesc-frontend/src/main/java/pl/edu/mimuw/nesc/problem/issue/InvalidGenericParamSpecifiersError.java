package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidGenericParamSpecifiersError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_GENERIC_PARAM_SPECIFIERS);
    public static final Code CODE = _CODE;

    private final InvalidCombinationType combinationType;

    public InvalidGenericParamSpecifiersError(InvalidCombinationType combinationType) {
        super(_CODE);
        checkNotNull(combinationType, "invalid combination type cannot be null");
        this.combinationType = combinationType;
    }

    @Override
    public String generateDescription() {

        switch (combinationType) {
            case TYPEDEF_WITH_OTHER:
                return "Cannot combine 'typedef' with other specifiers in a generic parameter declaration";
            case NORACE_WITH_OTHER:
                return "Cannot use non-type specifiers other than 'norace' in a non-type generic parameter declaration";
            default:
                return "Invalid combination of generic parameter specifiers";
        }

    }

    /**
     * Kind of the invalid specifiers combination.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum InvalidCombinationType {
        TYPEDEF_WITH_OTHER,
        NORACE_WITH_OTHER,
    }
}
