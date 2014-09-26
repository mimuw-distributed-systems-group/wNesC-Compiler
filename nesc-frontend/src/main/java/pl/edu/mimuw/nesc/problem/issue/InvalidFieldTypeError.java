package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.type.Type;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidFieldTypeError extends ErroneousIssue {
    private final Type fieldType;

    public InvalidFieldTypeError(Type fieldType) {
        super(Issues.ErrorType.INVALID_FIELD_TYPE);
        checkNotNull(fieldType, "field type cannot be null");
        this.fieldType = fieldType;
    }

    @Override
    public String generateDescription() {
        if (fieldType.isFunctionType()) {
            return format("Cannot declare a field of a function type '%s'", fieldType);
        } else if (!fieldType.isComplete()) {
            return format("Cannot declare a field of an incomplete type '%s'", fieldType);
        }

        return format("Invalid field type '%s'", fieldType);
    }
}
