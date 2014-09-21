package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.type.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidIdentifierTypeError extends ErroneousIssue {
    private final String identifier;
    private final Type actualType;
    private final Type expectedType;

    public InvalidIdentifierTypeError(String identifier, Type actualType, Type expectedType) {
        super(Issues.ErrorType.INVALID_IDENTIFIER_TYPE);

        checkNotNull(identifier, "identifier cannot be null");
        checkNotNull(actualType, "actual type cannot be null");
        checkNotNull(expectedType, "expected type cannot be null");
        checkArgument(!identifier.isEmpty(), "identifier cannot be an empty string");

        this.identifier = identifier;
        this.actualType = actualType;
        this.expectedType = expectedType;
    }

    @Override
    public String generateDescription() {
        if (expectedType.isTypeDefinition()) {
            return format("Expected a type name but identifier '%s' has type '%s'",
                          identifier, actualType);
        }

        return format("Identifier '%s' has type '%s' but expecting an identifier of type '%s'",
                      identifier, actualType, expectedType);
    }
}
