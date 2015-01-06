package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidCombineAttributeUsageError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_COMBINE_ATTRIBUTE_USAGE);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidCombineAttributeUsageError designatorsUsed() {
        final String description = "Designators cannot be used to initialize NesC attribute '@combine'";
        return new InvalidCombineAttributeUsageError(description);
    }

    public static InvalidCombineAttributeUsageError invalidParamsCount(int actualParamsCount,
            int expectedParamsCount) {
        final String description = format("NesC attribute '@combine' expects exactly %d parameter(s) but %d %s provided",
                expectedParamsCount, actualParamsCount, IssuesUtils.getToBeConjugation(actualParamsCount));
        return new InvalidCombineAttributeUsageError(description);
    }

    public static InvalidCombineAttributeUsageError stringLiteralExpected(Expression providedExpression) {
        final String description = format("'%s' used as the parameter for NesC attribute '@combine' but a string literal expected",
                ASTWriter.writeToString(providedExpression));
        return new InvalidCombineAttributeUsageError(description);
    }

    public static InvalidCombineAttributeUsageError emptyStringLiteralUsed() {
        final String description = "Empty string cannot be the parameter for NesC attribute '@combine'";
        return new InvalidCombineAttributeUsageError(description);
    }

    public static InvalidCombineAttributeUsageError usedMoreThanOnce() {
        final String description = "NesC attribute '@combine' cannot be specified more then once for a single type definition";
        return new InvalidCombineAttributeUsageError(description);
    }

    public static InvalidCombineAttributeUsageError appliedNotToTypedef() {
        final String description = "NesC attribute '@combine' cannot be used in this context; only type definitions can be annotated by it";
        return new InvalidCombineAttributeUsageError(description);
    }

    private InvalidCombineAttributeUsageError(String description) {
        super(_CODE);
        checkNotNull(description, "description cannot be null");
        checkArgument(!description.isEmpty(), "description cannot be an empty string");
        this.description = description;
    }

    @Override
    public String generateDescription() {
        return description;
    }
}
