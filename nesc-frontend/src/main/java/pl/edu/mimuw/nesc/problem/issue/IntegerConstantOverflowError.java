package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.IntegerCstKind;
import pl.edu.mimuw.nesc.ast.IntegerCstSuffix;
import pl.edu.mimuw.nesc.ast.gen.IntegerCst;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.ast.IntegerCstSuffix.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class IntegerConstantOverflowError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INTEGER_CONSTANT_OVERFLOW);
    public static final Code CODE = _CODE;

    private final IntegerCst overflowedCst;

    public IntegerConstantOverflowError(IntegerCst overflowedCst) {
        super(_CODE);
        checkNotNull(overflowedCst, "the overflowed constant cannot be null");
        this.overflowedCst = overflowedCst;
    }

    @Override
    public String generateDescription() {
        final IntegerCstSuffix suffix = overflowedCst.getSuffix();

        if ((suffix == NO_SUFFIX || suffix == SUFFIX_L || suffix == SUFFIX_LL)
                && overflowedCst.getKind() == IntegerCstKind.DECIMAL) {
            return format("'%s' exceeds the maximum value of the largest signed integer type",
                          overflowedCst.getString());
        } else {
            return format("'%s' exceeds the maximum value of the largest unsigned integer type",
                          overflowedCst.getString());
        }
    }
}
