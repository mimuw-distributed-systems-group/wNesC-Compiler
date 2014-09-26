package pl.edu.mimuw.nesc.problem.issue;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SuperfluousSpecifiersWarning extends CautionaryIssue {
    private static final WarningCode _CODE = WarningCode.onlyInstance(Issues.WarningType.SUPERFLUOUS_SPECIFIERS);
    public static final Code CODE = _CODE;

    public SuperfluousSpecifiersWarning() {
        super(_CODE);
    }

    @Override
    public String generateDescription() {
        return "Specifiers ignored because they cannot be used in this context; remove them";
    }
}
