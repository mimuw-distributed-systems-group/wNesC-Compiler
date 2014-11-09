package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.problem.issue.IssuesUtils.getOrdinalForm;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidEndpointError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_ENDPOINT);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidEndpointError tooManyArgumentsLists(int listsCount) {
        final String description = format("An endpoint must have at most one list of arguments but %d lists are specified",
                listsCount);
        return new InvalidEndpointError(description);
    }

    public static InvalidEndpointError nestedArgumentsList() {
        final String description = "List of arguments of an endpoint must be located after all identifiers";
        return new InvalidEndpointError(description);
    }

    public static InvalidEndpointError invalidIdentifiersCount(int identifiersCount) {
        final String description = format("An endpoint must consist of exactly one or two identifiers separated by a dot but %d identifiers specified",
                identifiersCount);
        return new InvalidEndpointError(description);
    }

    public static InvalidEndpointError parameterisedComponentRef() {
        final String description = format("List of arguments cannot be applied to the first identifier of a link wires endpoint");
        return new InvalidEndpointError(description);
    }

    public static InvalidEndpointError nonexistentSpecificationEntity(String component, String entityName) {
        final String description = format("Component '%s' does not contain specification element '%s'",
                component, entityName);
        return new InvalidEndpointError(description);
    }

    public static InvalidEndpointError unexpectedArguments(String entityName, int paramsCount) {

        final String fmt = paramsCount == 1
                ? "Specification element '%s' is not parameterised but %d argument is provided; remove it"
                : "Specification element '%s' is not parameterised but %d arguments are provided; remove them";

        final String description = format(fmt, entityName, paramsCount);
        return new InvalidEndpointError(description);
    }

    public static InvalidEndpointError invalidArgumentsCount(String entityName,
            int expectedParamsCount, int providedParamsCount) {

        final String description = format("Specification element '%s' requires %d argument(s) but %s provided",
                entityName, expectedParamsCount, providedParamsCount);
        return new InvalidEndpointError(description);
    }

    public static InvalidEndpointError invalidEndpointArgumentType(String entityName, int paramNum,
            Expression providedExpr, Type providedType) {

        final String description = format("The %s argument '%s' for '%s' has type '%s' but expecting an integer type",
                getOrdinalForm(paramNum), PrettyPrint.expression(providedExpr), entityName, providedType);
        return new InvalidEndpointError(description);
    }

    private InvalidEndpointError(String description) {
        super(_CODE);

        checkNotNull(description, "description cannot be null");
        checkArgument(!description.isEmpty(), "description cannot be empty");

        this.description = description;
    }

    @Override
    public String generateDescription() {
        return description;
    }
}
