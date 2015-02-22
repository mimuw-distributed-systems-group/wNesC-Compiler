package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class InvalidCallInfoAttributeUsageError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_CALL_INFO_ATTRIBUTE_USAGE);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidCallInfoAttributeUsageError appliedNotToFunction(String attributeName) {
        final String description = format("Attribute @%s() can only be applied to functions", attributeName);
        return new InvalidCallInfoAttributeUsageError(description);
    }

    public static InvalidCallInfoAttributeUsageError parametersPresent(String attributeName,
            int initializersCount) {
        final String initializerText = initializersCount == 1
                ? "an initializer is"
                : "initializers are";
        final String description = format("Attribute @%s() takes an empty initializer list but %s given",
                attributeName, initializerText);
        return new InvalidCallInfoAttributeUsageError(description);
    }

    private InvalidCallInfoAttributeUsageError(String description) {
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
