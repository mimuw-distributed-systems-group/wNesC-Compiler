package pl.edu.mimuw.nesc.problem.issue;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.type.FunctionType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;

import static java.lang.String.format;
import static pl.edu.mimuw.nesc.problem.issue.IssuesUtils.getInterfaceEntityText;
import static pl.edu.mimuw.nesc.problem.issue.IssuesUtils.getOrdinalForm;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidInterfaceEntityDefinitionError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_INTERFACE_ENTITY_DEFINITION);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidInterfaceEntityDefinitionError interfaceExpected(String interfaceName) {
        final String description = format("'%s' is not an instance of an interface", interfaceName);
        return new InvalidInterfaceEntityDefinitionError(description);
    }

    public static InvalidInterfaceEntityDefinitionError nonexistentInterfaceEntity(String interfaceName,
            String instanceName, String entityName) {

        final String description = interfaceName.equals(instanceName)
                ? format("Interface '%s' does not contain command or event '%s'",
                         interfaceName, entityName)
                : format("Interface '%s' instantiated as '%s' does not contain command or event '%s'",
                         interfaceName, instanceName, entityName);
        return new InvalidInterfaceEntityDefinitionError(description);
    }

    public static InvalidInterfaceEntityDefinitionError undeclaredBareEntity(InterfaceEntity.Kind kind,
            String name) {
        final String description = format("Bare %s '%s' has not been declared in the specification",
                                         getInterfaceEntityText(kind, false), name);
        return new InvalidInterfaceEntityDefinitionError(description);
    }

    public static InvalidInterfaceEntityDefinitionError interfaceEntitySpecifierExpected(InterfaceEntity.Kind kind) {
        final String specifier = kind == InterfaceEntity.Kind.COMMAND
                ? "command"
                : "event";

        final String description = format("'%s' expected as the only storage-class specifier",
                specifier);
        return new InvalidInterfaceEntityDefinitionError(description);
    }

    public static InvalidInterfaceEntityDefinitionError providedDefaultMismatch(Optional<String> ifaceName,
            String callableName, InterfaceEntity.Kind kind, boolean isProvided) {

        final String name = getName(ifaceName, callableName);
        final String entity = getInterfaceEntityText(kind, true);

        final String description = isProvided
                ? format("%s '%s' cannot have a default implementation because it is provided",
                         entity, name)
                : format("%s '%s' can only have a default implementation because it is used but 'default' specifier is missing",
                         entity, name);

        return new InvalidInterfaceEntityDefinitionError(description);
    }

    public static InvalidInterfaceEntityDefinitionError invalidScope() {
        final String description = "Commands and events must be defined in implementation scopes of modules";
        return new InvalidInterfaceEntityDefinitionError(description);
    }

    public static InvalidInterfaceEntityDefinitionError invalidType(Optional<String> ifaceName,
            String callableName, InterfaceEntity.Kind kind, Type expectedType, Type actualType,
            Optional<Type> interfaceType) {

        final String entity = getInterfaceEntityText(kind, false);
        final String name = getName(ifaceName, callableName);
        final Optional<String> ifaceTypeStr;

        if (interfaceType.isPresent()) {
            String fullIfaceTypeStr = interfaceType.get().toString();
            ifaceTypeStr = fullIfaceTypeStr.matches("interface .*")
                    ? Optional.of(fullIfaceTypeStr.substring(10))
                    : Optional.of(fullIfaceTypeStr);
        } else {
            ifaceTypeStr = ifaceName;
        }

        final String description = ifaceTypeStr.isPresent()
            ? format("Type '%s' of %s '%s' differs from its declared type '%s' in interface '%s'",
                     actualType, entity, name, expectedType, ifaceTypeStr.get())
            : format("Type '%s' of bare %s '%s' differs from its declared type '%s' in the specification",
                     actualType, entity, name, expectedType);

        return new InvalidInterfaceEntityDefinitionError(description);
    }

    public static InvalidInterfaceEntityDefinitionError instanceParametersExpected(Optional<String> ifaceName,
            String callableName) {

        final String description = ifaceName.isPresent()
                ? format("Interface '%s' is parameterised but instance parameters are not declared",
                         ifaceName.get())
                : format("'%s' is parameterised but instance parameters are not declared",
                         callableName);
        return new InvalidInterfaceEntityDefinitionError(description);
    }

    public static InvalidInterfaceEntityDefinitionError unexpectedInstanceParameters(Optional<String> ifaceName,
            String callableName) {

        final String description = ifaceName.isPresent()
                ? format("Interface '%s' is not parameterised but instance parameters are declared",
                         ifaceName.get())
                : format("'%s' is not parameterised but instance parameters are declared",
                         callableName);
        return new InvalidInterfaceEntityDefinitionError(description);
    }

    public static InvalidInterfaceEntityDefinitionError invalidInstanceParametersCount(Optional<String> ifaceName,
            String callableName, int expectedCount, int actualCount) {

        final String name = getName(ifaceName, callableName);

        final String fmt = actualCount == 1
                ? "'%s' requires %d instance parameter(s) but %d is declared"
                : "'%s' requires %d instance parameter(s) but %d are declared";

        final String description = format(fmt, name, expectedCount, actualCount);
        return new InvalidInterfaceEntityDefinitionError(description);
    }

    public static InvalidInterfaceEntityDefinitionError invalidInstanceParamType(int paramNum, Type expectedType,
            Type actualType) {

        final String description = format("Type '%s' of the %s instance parameter differs from its declared type '%s' in the specification",
                actualType, getOrdinalForm(paramNum), expectedType);

        return new InvalidInterfaceEntityDefinitionError(description);
    }

    private static String getName(Optional<String> ifaceName, String callableName) {
        return ifaceName.isPresent()
            ? format("%s.%s", ifaceName.get(), callableName)
            : callableName;
    }

    private InvalidInterfaceEntityDefinitionError(String description) {
        super(_CODE);
        this.description = description;
    }

    @Override
    public String generateDescription() {
        return description;
    }
}
