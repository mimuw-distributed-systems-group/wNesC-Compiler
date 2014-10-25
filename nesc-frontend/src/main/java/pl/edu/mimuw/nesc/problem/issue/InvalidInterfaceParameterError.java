package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.type.UnknownType;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidInterfaceParameterError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_INTERFACE_PARAMETER);
    public static final Code CODE = _CODE;

    private final IfaceParamProblemKind kind;
    private final String interfaceName;
    private final UnknownType definitionType;
    private final Type providedType;
    private final int paramNum;

    public InvalidInterfaceParameterError(IfaceParamProblemKind kind, String interfaceName,
            UnknownType definitionType, Type providedType, int paramNum) {
        super(_CODE);

        checkNotNull(kind, "interface parameter error kind cannot be null");
        checkNotNull(interfaceName, "name of the interface cannot be null");
        checkNotNull(definitionType, "type from the definition cannot be null");
        checkNotNull(providedType, "provided type cannot be null");
        checkArgument(!interfaceName.isEmpty(), "name of the interface cannot be null");
        checkArgument(paramNum > 0, "number of parameter must be positive");

        this.kind = kind;
        this.interfaceName = interfaceName;
        this.definitionType = definitionType;
        this.providedType = providedType;
        this.paramNum = paramNum;
    }

    @Override
    public String generateDescription() {

        switch(kind) {
            case INCOMPLETE_TYPE:
                return format("Incomplete type '%s' used for the %s parameter of interface '%s' but expecting a complete type",
                        providedType, getOrdinalParamNum(), interfaceName);
            case FUNCTION_TYPE:
                return format("Function type '%s' used for the %s parameter of interface '%s' but expecting a non-function type",
                        providedType, getOrdinalParamNum(), interfaceName);
            case ARRAY_TYPE:
                return format("Array type '%s' used for the %s parameter of interface '%s' but expecting a non-array type",
                        providedType, getOrdinalParamNum(), interfaceName);
            case INTEGER_TYPE_EXPECTED:
                return format("%s parameter of interface '%s' must be an integer type but non-integer type '%s' is used",
                        getOrdinalParamNum(), interfaceName, providedType);
            case ARITHMETIC_TYPE_EXPECTED:
                return format("%s parameter of interface '%s' must be an arithmetic type but non-arithmetic type '%s' is used",
                        getOrdinalParamNum(), interfaceName, providedType);
            default:
                return format("%s parameter for interface '%s' is invalid",
                        getOrdinalParamNum(), interfaceName);
        }

    }

    private String getOrdinalParamNum() {
        switch (paramNum) {
            case 1: return "1st";
            case 2: return "2nd";
            case 3: return "3rd";
            default: return paramNum + "th";
        }
    }

    public enum IfaceParamProblemKind {
        INCOMPLETE_TYPE,
        FUNCTION_TYPE,
        ARRAY_TYPE,
        INTEGER_TYPE_EXPECTED,
        ARITHMETIC_TYPE_EXPECTED,
    }
}
