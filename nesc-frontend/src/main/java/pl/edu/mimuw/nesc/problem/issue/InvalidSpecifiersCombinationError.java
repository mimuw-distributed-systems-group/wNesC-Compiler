package pl.edu.mimuw.nesc.problem.issue;

import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.analysis.SpecifiersAnalysis.NonTypeSpecifier;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidSpecifiersCombinationError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_SPECIFIERS_COMBINATION);
    public static final Code CODE = _CODE;

    private final ImmutableSet<NonTypeSpecifier> specifiers;

    public InvalidSpecifiersCombinationError(ImmutableSet<NonTypeSpecifier> specifiers) {
        super(_CODE);

        checkNotNull(specifiers, "specifiers set cannot be null");
        checkArgument(!specifiers.isEmpty(), "specifiers set cannot be empty");

        this.specifiers = specifiers;
    }

    @Override
    public String generateDescription() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Specifiers ");

        int specifiersLeft = specifiers.size();

        for (NonTypeSpecifier specifier : specifiers) {
            if (specifiersLeft == specifiers.size()) {
                builder.append("'");
            } else if (specifiersLeft == 1) {
                builder.append(" and '");
            } else {
                builder.append(", '");
            }

            builder.append(specifier.getRid().getName());
            builder.append("'");

            --specifiersLeft;
        }

        builder.append(" cannot be combined");

        return builder.toString();
    }
}
