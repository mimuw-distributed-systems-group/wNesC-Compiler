package pl.edu.mimuw.nesc.problem.issue;

import com.google.common.base.Optional;
import java.math.BigInteger;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidBitFieldDeclarationError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_BITFIELD_DECLARATION);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidBitFieldDeclarationError externalBitFieldInNonexternalTag(
            Optional<String> bitFieldName, StructKind kind) {
        final String description = format("Cannot define %s of external type in a non-external %s",
                getBitFieldText(bitFieldName), IssuesUtils.getStructKindText(kind, false));
        return new InvalidBitFieldDeclarationError(description);
    }

    public static InvalidBitFieldDeclarationError negativeWidth(Optional<String> bitFieldName,
            BigInteger width) {
        final String description = format("Cannot define %s with negative width %s",
                getBitFieldText(bitFieldName), width);
        return new InvalidBitFieldDeclarationError(description);
    }

    public static InvalidBitFieldDeclarationError tooLargeWidth(Optional<String> bitFieldName,
            BigInteger width, Type bitFieldType) {
        final String description = format("Cannot define %s with width %s that exceeds the size of its type '%s'",
                getBitFieldText(bitFieldName), width, bitFieldType);
        return new InvalidBitFieldDeclarationError(description);
    }

    private static String getBitFieldText(Optional<String> bitFieldName) {
        return bitFieldName.isPresent()
                ? format("bit-field '%s'", bitFieldName.get())
                : "an unnamed bit-field";
    }

    private InvalidBitFieldDeclarationError(String description) {
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
