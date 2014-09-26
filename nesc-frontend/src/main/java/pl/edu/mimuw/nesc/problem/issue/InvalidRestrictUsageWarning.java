package pl.edu.mimuw.nesc.problem.issue;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidRestrictUsageWarning extends CautionaryIssue {
    private static final WarningCode _CODE = WarningCode.onlyInstance(Issues.WarningType.INVALID_RESTRICT_USAGE);
    public static final Code CODE = _CODE;

    public InvalidRestrictUsageWarning() {
        super(_CODE);
    }

    @Override
    public String generateDescription() {
        return "'restrict' type qualifier ignored because it cannot be applied to a non-pointer type; remove it";
    }
}
