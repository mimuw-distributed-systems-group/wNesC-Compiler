package pl.edu.mimuw.nesc.problem.issue;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.RID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TypeSpecifierRepetitionError extends ErroneousIssue {
    private final Optional<RID> repeatedRid;
    private final int exceededRepetitionsCount;

    public static TypeSpecifierRepetitionError ridRepetition(RID repeatedRid, int exceededRepetitionsCount) {
        checkNotNull(repeatedRid, "repeated RID cannot be null");
        checkArgument(exceededRepetitionsCount >= 1, "exceeded repetitions count must be positive");

        return new TypeSpecifierRepetitionError(Optional.of(repeatedRid), exceededRepetitionsCount);
    }

    public static TypeSpecifierRepetitionError tagRefRepetition(int exceededRepetitionsCount) {
        checkArgument(exceededRepetitionsCount >= 1, "exceeded repetitions count must be positive");
        return new TypeSpecifierRepetitionError(Optional.<RID>absent(), exceededRepetitionsCount);
    }

    private TypeSpecifierRepetitionError(Optional<RID> repeatedRid, int exceededRepetitionsCount) {
        super(Issues.ErrorType.TYPE_SPECIFIER_REPETITION);
        this.repeatedRid = repeatedRid;
        this.exceededRepetitionsCount = exceededRepetitionsCount;
    }

    @Override
    public String generateDescription() {
        if (repeatedRid.isPresent()) {
            return format("Cannot use '%s' type specifier because it has been already used %d time(s)",
                          repeatedRid.get().getName(), exceededRepetitionsCount);
        } else {
            return format("Cannot use a tag type specifier because such specifier has been already used %d time(s)",
                          exceededRepetitionsCount);
        }
    }
}
