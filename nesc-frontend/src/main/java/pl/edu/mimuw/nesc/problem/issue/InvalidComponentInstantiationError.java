package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidComponentInstantiationError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_COMPONENT_INSTANTIATION);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidComponentInstantiationError abstractMismatch(String componentName,
            boolean isAbstractComponent) {

        final String fmt = isAbstractComponent
                ? "'%s' is a generic component and must be instantiated; use 'new' keyword and parentheses"
                : "'%s' is not generic and cannot be instantiated; remove 'new' keyword and parentheses";
        final String description = format(fmt, componentName);

        return new InvalidComponentInstantiationError(description);
    }

    public static InvalidComponentInstantiationError missingComponentParams(String componentName) {
        final String description = format("Component '%s' is marked as generic but the list of its generic parameters is absent; correct the definition of '%s'",
                componentName, componentName);
        return new InvalidComponentInstantiationError(description);
    }

    public static InvalidComponentInstantiationError invalidProvidedParamsCount(String componentName,
            String instanceName, int expectedParamsCount, int providedParamsCount) {
        final String verb = providedParamsCount <= 1
                ? "is"
                : "are";

        final String description = format("Component '%s' requires %d generic parameter(s) but %d %s provided for its reference '%s'",
                componentName, expectedParamsCount, providedParamsCount, verb, instanceName);

        return new InvalidComponentInstantiationError(description);
    }

    private InvalidComponentInstantiationError(String description) {
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
