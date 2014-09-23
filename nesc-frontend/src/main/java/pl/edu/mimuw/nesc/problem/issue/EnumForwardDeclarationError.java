package pl.edu.mimuw.nesc.problem.issue;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class EnumForwardDeclarationError extends ErroneousIssue {
    public EnumForwardDeclarationError() {
        super(Issues.ErrorType.ENUM_FORWARD_DECLARATION);
    }

    @Override
    public String generateDescription() {
        return "Invalid declaration; forward declarations of enumeration types are forbidden in the ISO C standard";
    }
}
