package pl.edu.mimuw.nesc.problem.issue;

import java.util.Set;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.FunctionType;
import pl.edu.mimuw.nesc.ast.type.InterfaceType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.problem.issue.IssuesUtils.getCompactTypeText;
import static pl.edu.mimuw.nesc.problem.issue.IssuesUtils.getInterfaceEntityText;
import static pl.edu.mimuw.nesc.problem.issue.IssuesUtils.getOrdinalForm;
import static pl.edu.mimuw.nesc.problem.issue.IssuesUtils.getProvidedText;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidConnectionError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_CONNECTION);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidConnectionError entitiesKindMismatch(InterfaceEntity.Kind kind,
            String bareEntityName, String ifaceEntityName) {

        final String description = format("Cannot connect bare %s '%s' and interface '%s'; expecting two bare commands or events or two interfaces",
                getInterfaceEntityText(kind, false), bareEntityName, ifaceEntityName);
        return new InvalidConnectionError(description);
    }

    public static InvalidConnectionError bareEntitiesKindMismatch(String entityName1, InterfaceEntity.Kind kind1,
            String entityName2, InterfaceEntity.Kind kind2) {

        final String description = format("Cannot connect bare %s '%s' and bare %s '%s'; expecting two bare commands or two bare events",
                getInterfaceEntityText(kind1, false), entityName1, getInterfaceEntityText(kind2, false), entityName2);
        return new InvalidConnectionError(description);
    }

    public static InvalidConnectionError invalidBareFunctionTypes(InterfaceEntity.Kind kind, String entityName1,
            FunctionType type1, String entityName2, FunctionType type2) {

        final String kindText = getInterfaceEntityText(kind, false);
        final String description = format("Types of connected %ss '%s' ('%s') and '%s' ('%s') differ; expecting compatible types",
                kindText, entityName1, type1, entityName2, type2);
        return new InvalidConnectionError(description);
    }

    public static InvalidConnectionError invalidInterfaceTypes(String entityName1,
            InterfaceType type1, String entityName2, InterfaceType type2) {

        final String description = format("Types of connected interfaces '%s' ('%s') and '%s' ('%s') differ; expecting compatible types",
                entityName1, getCompactTypeText(type1), entityName2, getCompactTypeText(type2));
        return new InvalidConnectionError(description);
    }

    public static InvalidConnectionError invalidDirection(String from, boolean isProvidedFrom,
            String to, boolean isProvidedTo) {

        final String fmt;
        final String explanation = "the arrow shall lead from a used to a provided element";

        if (isProvidedFrom && isProvidedTo) {
            fmt = "Cannot connect two provided elements '%s' and '%s'; %s";
        } else if (!isProvidedFrom && !isProvidedTo) {
            fmt = "Cannot connect two used elements '%s' and '%s'; %s";
        } else {
            fmt = "Cannot connect provided element '%s' to used element '%s'; %s";
        }

        final String description = format(fmt, from, to, explanation);

        return new InvalidConnectionError(description);
    }

    public static InvalidConnectionError parameterisedEndpointMismatch(String entityName1,
            String entityName2, boolean isParameterised1) {

        final String parameterisedEndpoint, nonParameterisedEndpoint;

        if (isParameterised1) {
            parameterisedEndpoint = entityName1;
            nonParameterisedEndpoint = entityName2;
        } else {
            parameterisedEndpoint = entityName2;
            nonParameterisedEndpoint = entityName1;
        }

        final String description = format("Cannot connect parameterised endpoint '%s' and a non-parameterised endpoint '%s'; either both endpoints shall be parameterised or none",
                parameterisedEndpoint, nonParameterisedEndpoint);
        return new InvalidConnectionError(description);
    }

    public static InvalidConnectionError parameterisedEndpointCountMismatch(String entityName1,
            int instanceParamsCount1, String entityName2, int instanceParamsCount2) {

        final String description = format("Cannot connect parameterised endpoints '%s' with %d instance parameter(s) and '%s' – with %d",
                entityName1, instanceParamsCount1, entityName2, instanceParamsCount2);
        return new InvalidConnectionError(description);
    }

    public static InvalidConnectionError parameterisedEndpointTypeMismatch(String entityName1,
            Type type1, String entityName2, Type type2, int paramNum) {

        final String description = format("Types of the %s instance parameter in endpoints '%s' ('%s') and '%s' ('%s') differ",
                getOrdinalForm(paramNum), entityName1, type1, entityName2, type2);
        return new InvalidConnectionError(description);
    }

    public static InvalidConnectionError implicitConnectionNoMatch(String endpointName,
            String componentName) {

        final String description = format("No specification element from component '%s' can be used to made an implicit connection with endpoint '%s'",
                componentName, endpointName);
        return new InvalidConnectionError(description);
    }

    public static InvalidConnectionError implicitConnectionManyMatches(String endpointName,
            Set<String> matchedElementsNames) {

        final StringBuilder builder = new StringBuilder();
        builder.append(format("Too many specification elements match with '%s': ", endpointName));

        boolean first = true;
        for (String elementName : matchedElementsNames) {
            if (!first) {
                builder.append(", '");
            } else {
                builder.append("'");
            }
            first = false;
            builder.append(elementName);
            builder.append("'");
        }

        return new InvalidConnectionError(builder.toString());
    }

    public static InvalidConnectionError expectedBothProvidedOrUsed(String endpoint1Str,
            boolean isProvided1, String endpoint2Str, boolean isProvided2) {

        final String description = format("%s endpoint '%s' is connected with %s endpoint '%s'; expecting two provided or two used elements",
                getProvidedText(isProvided1, true), endpoint1Str, getProvidedText(isProvided2, false), endpoint2Str);
        return new InvalidConnectionError(description);
    }

    private InvalidConnectionError(String description) {
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
