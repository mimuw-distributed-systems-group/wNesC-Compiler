package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.type.PointerType;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.astwriting.Tokens.BinaryOp;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class VoidPointerArithmeticsWarning extends CautionaryIssue {
    private static final WarningCode _CODE = WarningCode.onlyInstance(Issues.WarningType.VOID_POINTER_ARITHMETICS);
    public static final Code CODE = _CODE;

    private final Expression ptrExpr;
    private final PointerType ptrType;
    private final BinaryOp op;

    public VoidPointerArithmeticsWarning(Expression ptrExpr, PointerType ptrType, BinaryOp op) {
        super(_CODE);

        checkNotNull(ptrExpr, "pointer expression cannot be null");
        checkNotNull(ptrType, "type of the pointer expression cannot be null");
        checkNotNull(op, "binary operator cannot be null");

        this.ptrExpr = ptrExpr;
        this.ptrType = ptrType;
        this.op = op;
    }

    @Override
    public String generateDescription() {
        return format("Pointer '%s' to '%s' used for an arithmetic operation with operator %s",
                ASTWriter.writeToString(ptrExpr), ptrType.getReferencedType(), op);
    }
}
