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
public final class InvalidArrayRefExprError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_ARRAYREF_EXPR);
    public static final Code CODE = _CODE;

    private final Type typeArray, typeIndex;
    private final Expression exprArray, exprIndex;

    public InvalidArrayRefExprError(Type typeArray, Expression exprArray,
            Type typeIndex, Expression exprIndex) {
        super(_CODE);

        checkNotNull(typeArray, "type of the array expression cannot be null");
        checkNotNull(exprArray, "array expression cannot be null");
        checkNotNull(typeIndex, "type of the index expression cannot be null");
        checkNotNull(exprIndex, "index expression cannot be null");

        this.typeArray = typeArray;
        this.typeIndex = typeIndex;
        this.exprArray = exprArray;
        this.exprIndex = exprIndex;
    }

    @Override
    public String generateDescription() {
        if (!typeArray.isPointerType() && !typeIndex.isPointerType()) {
            return format("Operands '%s' and '%s' for operator [] have types '%s' and '%s' but expecting an operand with array or pointer type",
                    ASTWriter.writeToString(exprArray), ASTWriter.writeToString(exprIndex), typeArray, typeIndex);
        } else {
            final PointerType ptrType = typeArray.isPointerType()
                    ? (PointerType) typeArray
                    : (PointerType) typeIndex;
            final Expression ptrExpr = ptrType == typeArray
                    ? exprArray
                    : exprIndex;
            final Type otherType = ptrType == typeArray
                    ? typeIndex
                    : typeArray;
            final Expression otherExpr = otherType == typeIndex
                    ? exprIndex
                    : exprArray;
            final Type refType = ptrType.getReferencedType();

            if (!otherType.isGeneralizedIntegerType()) {
                return format("Operand '%s' for operator [] has type '%s' but expecting an integer type as the other operand is a pointer",
                              ASTWriter.writeToString(otherExpr), otherType);
            } else if (!refType.isComplete()) {
                return format("Operand '%s' for operator [] is a pointer to incomplete type '%s'",
                              ASTWriter.writeToString(ptrExpr), refType);
            } else if (!refType.isObjectType()) {
                return format("Operand '%s' for operator [] is not a pointer to an object; expecting a pointer to an object type",
                              ASTWriter.writeToString(ptrExpr));
            }
        }

        return format("Invalid operands with types '%s' and '%s' for operator []",
                      typeArray, typeIndex);
    }
}
