package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.RID;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TypeQualifierRepetitionWarning extends CautionaryIssue {
    private static final WarningCode _CODE = WarningCode.onlyInstance(Issues.WarningType.TYPE_QUALIFIER_REPETITION);
    public static final Code CODE = _CODE;

    private final RID repeatedQualifier;

    public TypeQualifierRepetitionWarning(RID repeatedQualifier) {
        super(_CODE);
        checkNotNull(repeatedQualifier, "the repeated qualifier cannot be null");
        this.repeatedQualifier = repeatedQualifier;
    }

    @Override
    public final String generateDescription() {
        return format("'%s' type qualifier ignored because it has been already specified; remove it",
                      repeatedQualifier.getName());
    }
}
