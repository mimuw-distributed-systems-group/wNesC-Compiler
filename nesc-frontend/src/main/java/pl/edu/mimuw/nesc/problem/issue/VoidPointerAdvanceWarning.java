package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.type.PointerType;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class VoidPointerAdvanceWarning extends CautionaryIssue {
    private static final WarningCode _CODE = WarningCode.onlyInstance(Issues.WarningType.VOID_POINTER_ADVANCE);
    public static final Code CODE = _CODE;

    private final Expression ptrExpr;
    private final Type ptrType;

    public VoidPointerAdvanceWarning(Expression ptrExpr, Type ptrType) {
        super(_CODE);

        checkNotNull(ptrExpr, "the expression of pointer type cannot be null");
        checkNotNull(ptrType, "type of the pointer cannot be null");

        this.ptrExpr = ptrExpr;
        this.ptrType = ptrType;
    }

    @Override
    public String generateDescription() {
        if (ptrType.isPointerType()) {

            final PointerType castedPtrType = (PointerType) ptrType;

            return format("Advancing pointer '%s' to '%s'",
                          ASTWriter.writeToString(ptrExpr),
                          castedPtrType.getReferencedType());
        }

        return format("Advancing void pointer '%s'", ASTWriter.writeToString(ptrExpr));
    }
}
