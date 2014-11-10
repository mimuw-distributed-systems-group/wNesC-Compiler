package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.problem.issue.IssuesUtils.getProvidedText;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidEqConnectionError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_EQ_CONNECTION);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidEqConnectionError invalidObjectReferenced(String name) {
        final String description = format("'%s' does not denote a component or an external specification element",
                name);
        return new InvalidEqConnectionError(description);
    }

    public static InvalidEqConnectionError expectedExternalSpecElement() {
        final String description = "Equate wires requires at least one endpoint that is an external specification element but no such endpoint specified";
        return new InvalidEqConnectionError(description);
    }

    public static InvalidEqConnectionError externalSpecElementDereference(int identifiersCount) {

        final String description = format("An endpoint that is an external specification element must consist of only one identifier but %d specified",
                identifiersCount);
        return new InvalidEqConnectionError(description);
    }

    public static InvalidEqConnectionError argumentsForComponent(String name) {
        final String description = format("Instance arguments specified for component '%s'; cannot provide arguments for components, remove them",
                name);
        return new InvalidEqConnectionError(description);
    }

    public static InvalidEqConnectionError expectedBothUsedOrProvided(String endpoint1Str,
            boolean provided1, String endpoint2Str, boolean provided2) {

        final String description = format("%s endpoint '%s' is connected with %s endpoint '%s'; connected internal and external elements shall be both used or both provided",
                getProvidedText(provided1, true), endpoint1Str, getProvidedText(provided2, false),
                endpoint2Str);
        return new InvalidEqConnectionError(description);
    }

    public static InvalidEqConnectionError expectedOneUsedAndOneProvided(String endpoint1Str,
            String endpoint2Str, boolean provided) {

        final String description = format("Connected external elements '%s' and '%s' are both %s; one external element shall be provided and the other used",
                endpoint1Str, endpoint2Str, getProvidedText(provided, false));
        return new InvalidEqConnectionError(description);
    }

    private InvalidEqConnectionError(String description) {
        super(_CODE);

        checkNotNull(description, "description of the error cannot be null");
        checkArgument(!description.isEmpty(), "description cannot be an empty string");

        this.description = description;
    }

    @Override
    public String generateDescription() {
        return description;
    }
}
