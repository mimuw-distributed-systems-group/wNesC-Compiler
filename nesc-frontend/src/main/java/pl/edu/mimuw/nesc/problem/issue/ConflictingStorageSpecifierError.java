package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.RID;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ConflictingStorageSpecifierError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.CONFLICTING_STORAGE_SPECIFIER);
    public static final Code CODE = _CODE;

    private final RID conflictingSpecifier;
    private final RID firstSpecifier;

    public ConflictingStorageSpecifierError(RID conflictingSpecifier, RID firstSpecifier) {
        super(_CODE);

        checkNotNull(conflictingSpecifier, "conflicting specifier cannot be null");
        checkNotNull(firstSpecifier, "first specifier cannot be null");

        this.conflictingSpecifier = conflictingSpecifier;
        this.firstSpecifier = firstSpecifier;
    }

    @Override
    public String generateDescription() {
        return format("'%s' specifier cannot be combined with '%s' used earlier",
                      conflictingSpecifier.getName(), firstSpecifier.getName());
    }
}
