package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.NescDeclarationKind;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidNescDeclarationName extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_NESC_DECLARATION_NAME);
    public static final Code CODE = _CODE;

    private final String description;

    public InvalidNescDeclarationName(String actualNescDeclName, String expectedNescDeclName,
            NescDeclarationKind nescEntityKind, String absoluteFilePath) {
        super(_CODE);

        // Generate the description
        final String entityDescriptor = nescEntityKind == NescDeclarationKind.INTERFACE
                ? "interface"
                : "component";
        this.description = "Invalid " + entityDescriptor + " name, file '" + absoluteFilePath
                + "' must define interface or component '" + expectedNescDeclName
                + "' instead of '" + actualNescDeclName + "'";
    }

    @Override
    public String generateDescription() {
        return description;
    }
}
