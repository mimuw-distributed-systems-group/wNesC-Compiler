package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.TypeArgument;
import pl.edu.mimuw.nesc.type.ArrayType;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.problem.issue.IssuesUtils.getOrdinalForm;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidComponentParameterError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_COMPONENT_PARAMETER);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidComponentParameterError declarationMissing(String componentName,
            int paramNum) {
        final String description = format("The %s parameter of component '%s' is declared without a name; correct the definition of '%s'",
                getOrdinalForm(paramNum), componentName, componentName);
        return new InvalidComponentParameterError(description);
    }

    public static InvalidComponentParameterError typeExpected(String componentName,
            int paramNum, Expression providedExpr) {
        final String description = format("The %s parameter of component '%s' is a type argument but expression '%s' provided; replace it with a typename",
                getOrdinalForm(paramNum), componentName, ASTWriter.writeToString(providedExpr));
        return new InvalidComponentParameterError(description);
    }

    public static InvalidComponentParameterError incompleteTypeProvided(String componentName,
            int paramNum, Type providedType) {
        final String description = format("Incomplete type '%s' used for the %s parameter of component '%s' but expecting a complete type",
                providedType, getOrdinalForm(paramNum), componentName);
        return new InvalidComponentParameterError(description);
    }

    public static InvalidComponentParameterError functionTypeProvided(String componentName,
            int paramNum, Type providedType) {
        final String description = format("Function type '%s' used for the %s parameter of component '%s' but expecting non-function type",
                providedType, getOrdinalForm(paramNum), componentName);
        return new InvalidComponentParameterError(description);
    }

    public static InvalidComponentParameterError arrayTypeProvided(String componentName,
            int paramNum, Type providedType) {
        final String description = format("Array type '%s' used for the %s parameter of component '%s' but expecting non-array type",
                providedType, getOrdinalForm(paramNum), componentName);
        return new InvalidComponentParameterError(description);
    }

    public static InvalidComponentParameterError tagDefinitionProvided(String componentName,
            int paramNum) {
        final String description = format("Cannot use tag definition for a parameter of a component as for the %s parameter of component '%s'",
                getOrdinalForm(paramNum), componentName);
        return new InvalidComponentParameterError(description);
    }

    public static InvalidComponentParameterError expectedIntegerType(String componentName,
            int paramNum, Type providedType) {
        final String description = format("The %s parameter of component '%s' must be an integer type but non-integer type '%s' is provided",
                getOrdinalForm(paramNum), componentName, providedType);
        return new InvalidComponentParameterError(description);
    }

    public static InvalidComponentParameterError expectedArithmeticType(String componentName,
            int paramNum, Type providedType) {
        final String description = format("The %s parameter of component '%s' must be an arithmetic type but non-arithmetic type '%s' is provided",
                getOrdinalForm(paramNum), componentName, providedType);
        return new InvalidComponentParameterError(description);
    }

    public static InvalidComponentParameterError unexpectedType(String componentName, int paramNum,
            TypeArgument typeProvided) {

        final String description = typeProvided.getAsttype().getType().isPresent()
                ? format("The %s parameter of component '%s' is a non-type argument but type '%s' provided",
                    getOrdinalForm(paramNum), componentName, typeProvided.getAsttype().getType().get())
                : format("The %s parameter of component '%s' is a non-type argument but a type has been provided",
                    getOrdinalForm(paramNum), componentName);
        return new InvalidComponentParameterError(description);
    }

    public static InvalidComponentParameterError expectedIntegerExpression(String componentName,
            int paramNum, Expression providedExpression, Type providedType) {

        final String description = format("'%s' used for the %s parameter of component '%s' has type '%s' but expecting an integer type",
                ASTWriter.writeToString(providedExpression), getOrdinalForm(paramNum), componentName, providedType);
        return new InvalidComponentParameterError(description);
    }

    public static InvalidComponentParameterError expectedFloatingExpression(String componentName,
            int paramNum, Expression providedExpression, Type providedType) {

        final String description = format("'%s' used for the %s parameter of component '%s' has type '%s' but expecting a floating type",
                ASTWriter.writeToString(providedExpression), getOrdinalForm(paramNum), componentName, providedType);
        return new InvalidComponentParameterError(description);
    }

    public static InvalidComponentParameterError expectedCharArrayType(String componentName,
            int paramNum, Expression providedExpression, Type providedType,
            Iterable<ArrayType> allowedCharArrayTypes) {

        final String description = format("'%s' used for the %s parameter of component '%s' has type '%s' but expecting one of the following types: %s",
                ASTWriter.writeToString(providedExpression), getOrdinalForm(paramNum), componentName,
                providedType, IssuesUtils.getTypesText(allowedCharArrayTypes));
        return new InvalidComponentParameterError(description);
    }

    public static InvalidComponentParameterError invalidDeclaredParameterType(String componentName,
            int paramNum, Type expectedType, Iterable<ArrayType> allowedCharArrayTypes) {

        final String description = format("The %s parameter of component '%s' has declared type '%s' but only arithmetic types and %s (with indeterminate sizes) are allowed; correct the definition of '%s'",
                getOrdinalForm(paramNum), componentName, expectedType, IssuesUtils.getTypesText(allowedCharArrayTypes),
                componentName);
        return new InvalidComponentParameterError(description);
    }

    private InvalidComponentParameterError(String description) {
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
