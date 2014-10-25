package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidInterfaceInstantiationError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_INTERFACE_INSTANTIATION);
    public static final Code CODE = _CODE;

    private final String interfaceName;
    private final int definitionParamsCount, providedParamsCount;
    private final InterfaceRefProblemKind kind;

    public InvalidInterfaceInstantiationError(InterfaceRefProblemKind kind, String interfaceName,
                                              int definitionParamsCount, int providedParamsCount) {
        super(_CODE);

        checkNotNull(kind, "kind of the error cannot be null");
        checkNotNull(interfaceName, "interface name cannot be null");
        checkArgument(!interfaceName.isEmpty(), "interface name cannot be an empty string");
        checkArgument(definitionParamsCount >= 0, "parameters count in the definition cannot be negative");
        checkArgument(providedParamsCount >= 0, "the count of provided parameters cannot be negative");

        this.kind = kind;
        this.interfaceName = interfaceName;
        this.definitionParamsCount = definitionParamsCount;
        this.providedParamsCount = providedParamsCount;
    }

    @Override
    public String generateDescription() {

        switch (kind) {
            case UNEXPECTED_PARAMETERS:
                return format("Interface '%s' is not generic but parameters are given", interfaceName);
            case MISSING_PARAMETERS:
                return format("Interface '%s' is generic but no parameters are provided", interfaceName);
            case PARAMETERS_COUNTS_DIFFER:
                final String fmt = providedParamsCount == 1
                        ? "Interface '%s' requires %d generic parameter(s) but %d is given"
                        : "Interface '%s' requires %d generic parameter(s) but %d are given";
                return format(fmt, interfaceName, definitionParamsCount, providedParamsCount);
            default:
                return format("Invalid instantiation of interface '%s'", interfaceName);
        }

    }

    public enum InterfaceRefProblemKind {
        UNEXPECTED_PARAMETERS,
        MISSING_PARAMETERS,
        PARAMETERS_COUNTS_DIFFER,
    }
}
