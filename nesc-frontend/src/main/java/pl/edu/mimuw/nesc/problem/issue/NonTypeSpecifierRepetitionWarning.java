package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.RID;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NonTypeSpecifierRepetitionWarning extends CautionaryIssue {
    private static final WarningCode _CODE = WarningCode.onlyInstance(Issues.WarningType.NON_TYPE_SPECIFIER_REPETITION);
    public static final Code CODE = _CODE;

    private final RID repeatedSpecifier;

    public NonTypeSpecifierRepetitionWarning(RID repeatedSpecifier) {
        super(_CODE);
        checkNotNull(repeatedSpecifier, "repeated specifier cannot be null");
        this.repeatedSpecifier = repeatedSpecifier;
    }

    @Override
    public String generateDescription() {
        return format("'%s' specifier ignored because it has been already used; remove it",
                repeatedSpecifier.getName());
    }
}
