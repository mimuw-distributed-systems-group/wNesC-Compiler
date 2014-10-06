package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.PointerType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidFieldRefExprError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_FIELDREF_EXPR);
    public static final Code CODE = _CODE;

    private final Expression tagExpr;
    private final Type tagType;
    private final String fieldName;
    private final boolean isTypeComplete, isFieldPresent;

    public InvalidFieldRefExprError(Type tagType, Expression tagExpr, String fieldName,
            boolean isTypeComplete, boolean isFieldPresent) {
        super(_CODE);

        checkNotNull(tagExpr, "expression cannot be null");
        checkNotNull(tagType, "type cannot be null");
        checkNotNull(fieldName, "name of the accessed field cannot be null");
        checkArgument(!fieldName.isEmpty(), "name of the field cannot be an empty string");

        this.tagExpr = tagExpr;
        this.tagType = tagType;
        this.fieldName = fieldName;
        this.isTypeComplete = isTypeComplete;
        this.isFieldPresent = isFieldPresent;
    }

    @Override
    public String generateDescription() {
        if (!tagType.isFieldTagType()) {
            return format("'%s' has type '%s' but expecting a structure or union because of field reference",
                          PrettyPrint.expression(tagExpr), tagType);
        } else if (!isTypeComplete) {
            return format("Cannot refer to a field of incomplete type '%s' which is the type of expression '%s'",
                          tagType, PrettyPrint.expression(tagExpr));
        } else if (!isFieldPresent) {
            return format("Field '%s' does not exist in '%s' which is the type of expression '%s'",
                    fieldName, tagType, PrettyPrint.expression(tagExpr));
        }

        return format("Invalid reference of field '%s'", fieldName);
    }
}
