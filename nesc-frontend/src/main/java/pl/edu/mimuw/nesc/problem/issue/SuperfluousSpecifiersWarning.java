package pl.edu.mimuw.nesc.problem.issue;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SuperfluousSpecifiersWarning extends CautionaryIssue {
    public SuperfluousSpecifiersWarning() {
        super(Issues.WarningType.SUPERFLUOUS_SPECIFIERS);
    }

    @Override
    public String generateDescription() {
        return "Specifiers ignored because they cannot be used in this context; remove them";
    }
}
