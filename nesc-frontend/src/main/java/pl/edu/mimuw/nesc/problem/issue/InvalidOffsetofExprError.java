package pl.edu.mimuw.nesc.problem.issue;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.type.FieldTagType;
import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidOffsetofExprError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_OFFSETOF_EXPR);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidOffsetofExprError expectedFieldTagType(Type actualType) {
        final String description = format("Cannot use type '%s' as the first parameter of 'offsetof', expecting a structure or union type",
                actualType);
        return new InvalidOffsetofExprError(description);
    }

    public static InvalidOffsetofExprError incompleteType(Type actualType) {
        final String description = format("Cannot use incomplete type '%s' as the first parameter of 'offsetof'", actualType);
        return new InvalidOffsetofExprError(description);
    }

    public static InvalidOffsetofExprError memberErroneousType(FieldTagType<?> type, String name) {
        final String tagText = IssuesUtils.getFullFieldTagText(type.getDeclaration(), false);
        final String description = format("Type of field '%s' from %s is erroneous, correct its definition",
                name, tagText);
        return new InvalidOffsetofExprError(description);
    }

    public static InvalidOffsetofExprError memberExpectedFieldTagType(FieldTagType<?> type, String name) {
        final String tagText = IssuesUtils.getFullFieldTagText(type.getDeclaration(), false);
        final String description = format("Cannot use '%s' from %s as intermediate field for 'offsetof', expecting a structure or union",
                name, tagText);
        return new InvalidOffsetofExprError(description);
    }

    public static InvalidOffsetofExprError memberBitFieldReferred(FieldTagType<?> type, String name) {
        final String description = format("Cannot use bit-field '%s' as the 2nd parameter of 'offsetof'",
                name);
        return new InvalidOffsetofExprError(description);
    }

    public static InvalidOffsetofExprError memberIncompleteType(FieldTagType<?> type, String name) {
        final String tagText = IssuesUtils.getFullFieldTagText(type.getDeclaration(), false);
        final String description = format("Type of field '%s' from %s is incomplete, correct its definition",
                name, tagText);
        return new InvalidOffsetofExprError(description);
    }

    public static InvalidOffsetofExprError nonexistentMemberReferred(FieldTagType<?> type, String name) {
        final String tagText = IssuesUtils.getFullFieldTagText(type.getDeclaration(), true);
        final String description = format("%s does not contain field '%s'", tagText, name);
        return new InvalidOffsetofExprError(description);
    }

    private InvalidOffsetofExprError(String description) {
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
