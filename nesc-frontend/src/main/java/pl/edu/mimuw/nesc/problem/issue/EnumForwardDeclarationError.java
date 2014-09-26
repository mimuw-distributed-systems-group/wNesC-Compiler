package pl.edu.mimuw.nesc.problem.issue;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class EnumForwardDeclarationError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.ENUM_FORWARD_DECLARATION);
    public static final Code CODE = _CODE;

    public EnumForwardDeclarationError() {
        super(_CODE);
    }

    @Override
    public String generateDescription() {
        return "Invalid declaration; forward declarations of enumeration types are forbidden in the ISO C standard";
    }
}
