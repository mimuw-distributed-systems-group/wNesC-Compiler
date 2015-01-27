package pl.edu.mimuw.nesc.problem.issue;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.declaration.tag.fieldtree.BlockElement.BlockType;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidExternalTagFieldError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_EXTERNAL_TAG_FIELD);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidExternalTagFieldError fieldOfNonExternalType(Optional<String> name, Type type, StructKind kind) {
        final String kindText = IssuesUtils.getExternalKindText(kind);
        final String description = name.isPresent()
                ? format("Cannot define field '%s' of non-external type '%s' in %s",
                        name.get(), type.toString(), kindText)
                : format("Cannot define an unnamed bit-field of a non-external type '%s' in %s",
                        type.toString(), kindText);
        return new InvalidExternalTagFieldError(description);
    }

    public static InvalidExternalTagFieldError nonExternalBlock(BlockType innerBlockType, StructKind outerKind) {
        final String kindText = IssuesUtils.getExternalKindText(outerKind);
        final String innerKindText;
        switch (innerBlockType) {
            case STRUCTURE:
                innerKindText = "structure";
                break;
            case UNION:
                innerKindText = "union";
                break;
            default:
                throw new RuntimeException("unexpected block type '" + innerBlockType + "'");
        }

        final String description = format("An non-external anonymous %s cannot be contained in %s",
                innerKindText, kindText);
        return new InvalidExternalTagFieldError(description);
    }

    private InvalidExternalTagFieldError(String description) {
        super(_CODE);
        checkNotNull(description, "description cannot be null");
        checkArgument(!description.isEmpty(), "description cannot an empty string");
        this.description = description;
    }

    @Override
    public String generateDescription() {
        return description;
    }
}
