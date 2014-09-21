package pl.edu.mimuw.nesc.problem.issue;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidRestrictUsageWarning extends CautionaryIssue {
    public InvalidRestrictUsageWarning() {
        super(Issues.WarningType.INVALID_RESTRICT_USAGE);
    }

    @Override
    public String generateDescription() {
        return "'restrict' type qualifier ignored because it cannot be applied to a non-pointer type; remove it";
    }
}
