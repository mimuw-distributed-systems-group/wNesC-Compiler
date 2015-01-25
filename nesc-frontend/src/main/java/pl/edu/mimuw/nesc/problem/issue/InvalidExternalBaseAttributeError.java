package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidExternalBaseAttributeError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_EXTERNAL_BASE_ATTRIBUTE);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidExternalBaseAttributeError parametersMissing(String attributeName) {
        final String description = format("Attribute '%s' requires exactly one parameter but no arguments given",
                attributeName);
        return new InvalidExternalBaseAttributeError(description);
    }

    public static InvalidExternalBaseAttributeError invalidParamsCount(String attributeName,
            int actualCount) {
        final String description = format("Attribute '%s' requires exactly one parameter but %d parameter(s) given",
                attributeName, actualCount);
        return new InvalidExternalBaseAttributeError(description);
    }

    public static InvalidExternalBaseAttributeError identifierExpected(String attributeName,
            Expression expr) {
        final String description = format("Attribute '%s' requires an identifier as parameter but '%s' given",
                attributeName, ASTWriter.writeToString(expr));
        return new InvalidExternalBaseAttributeError(description);
    }

    public static InvalidExternalBaseAttributeError invalidParameterValue(String attributeName,
            String value) {
        final String description = format("'%s' is an invalid parameter for attribute '%s'", value, attributeName);
        return new InvalidExternalBaseAttributeError(description);
    }

    public static InvalidExternalBaseAttributeError tooManyAttributes() {
        final String description ="Too many attributes specifying an external base type; expecting at most one such attribute";
        return new InvalidExternalBaseAttributeError(description);
    }

    public static InvalidExternalBaseAttributeError appliedNotToTypedef() {
        final String description = "An attribute that specifies an external base type cannot be applied to an entity other than a type definition";
        return new InvalidExternalBaseAttributeError(description);
    }

    public static InvalidExternalBaseAttributeError appliedNotToIntegerType() {
        final String description = "An attribute that specifies an external base type cannot be applied to an enumerated type or a type other than an integer type";
        return new InvalidExternalBaseAttributeError(description);
    }

    private InvalidExternalBaseAttributeError(String description) {
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
