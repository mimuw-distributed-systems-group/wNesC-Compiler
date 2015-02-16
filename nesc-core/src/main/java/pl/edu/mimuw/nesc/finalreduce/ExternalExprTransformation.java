package pl.edu.mimuw.nesc.finalreduce;

import com.google.common.base.Optional;
import java.util.LinkedList;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.common.util.VariousUtils;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.declaration.tag.FieldDeclaration;
import pl.edu.mimuw.nesc.external.ExternalScheme;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.type.ArithmeticType;
import pl.edu.mimuw.nesc.type.PointerType;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.type.VoidType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class ExternalExprTransformation implements ExprTransformation<ExternalExprBlockData> {
    /**
     * Unmangled name of temporary variables used to preserve the semantics
     * of expressions.
     */
    private static final String NAME_TEMPORARY_VAR = "__nxtmp";

    /**
     * Name mangler that will be used.
     */
    private final NameMangler nameMangler;

    ExternalExprTransformation(NameMangler nameMangler) {
        checkNotNull(nameMangler, "name mangler cannot be null");
        this.nameMangler = nameMangler;
    }

    @Override
    public LinkedList<Expression> transform(Expression expr, ExternalExprBlockData blockData) {
        if (expr instanceof Assign) {
            return transformSimpleAssign((Assign) expr, blockData);
        } else if (expr instanceof Assignment) {
            return transformCompoundAssign((Assignment) expr, blockData);
        } else {
            return transformRead(expr, blockData);
        }
    }

    private LinkedList<Expression> transformSimpleAssign(Assign assign, ExternalExprBlockData blockData) {
        if (!needsTransformation(assign.getLeftArgument())) {
            return Lists.<Expression>newList(assign);
        }

        final Expression lhsExpr = assign.getLeftArgument();
        final ExternalScheme externalScheme = getExternalScheme(lhsExpr);
        final Expression replacement;

        if (isBitFieldReference(lhsExpr)) {
            final FieldRef fieldRef = (FieldRef) lhsExpr;
            final FieldDeclaration fieldDeclaration = fieldRef.getDeclaration();

            replacement = newWriteBitFieldCall(externalScheme.getWriteBitFieldFunctionName(),
                    newBitFieldDataExpr(fieldRef), fieldDeclaration.getOffsetInBits(),
                    fieldDeclaration.getSizeInBits(), assign.getRightArgument(), assign.getType());
        } else {
            replacement = newWriteCall(externalScheme.getWriteFunctionName(),
                    newNonBitFieldDataExpr(lhsExpr), assign.getRightArgument(),
                    assign.getType());
        }

        lhsExpr.setIsNxTransformed(true);

        return Lists.newList(replacement);
    }

    private LinkedList<Expression> transformCompoundAssign(Assignment assign, ExternalExprBlockData blockData) {
        if (!needsTransformation(assign.getLeftArgument())) {
            return Lists.<Expression>newList(assign);
        }

        final Expression lhsExpr = assign.getLeftArgument();
        final ExternalScheme externalScheme = getExternalScheme(lhsExpr);
        assign.getRightArgument().setParenthesesCount(1);

        // Create and add the declaration of the temporary variable

        final String temporaryName = nameMangler.mangle(NAME_TEMPORARY_VAR);
        blockData.getEnclosingBlock().get().getDeclarations().add(0, newTemporaryVariable(temporaryName));

        // Prepare the temporary identifier

        final Identifier temporaryId = AstUtils.newIdentifier(temporaryName,
                temporaryName, false, true);
        temporaryId.setType(Optional.<Type>of(new PointerType(new VoidType())));
        temporaryId.setIsLvalue(true);

        // Make the transformation

        final Expression temporaryAssignedVal;
        final Expression compoundAssign;

        if (isBitFieldReference(lhsExpr)) {
            final FieldRef fieldRef = (FieldRef) lhsExpr;
            final FieldDeclaration fieldDeclaration = fieldRef.getDeclaration();
            temporaryAssignedVal = newBitFieldDataExpr(fieldRef);

            final Expression oldValueReadExpr = newReadBitFieldCall(externalScheme.getReadBitFieldFunctionName(),
                    temporaryId, fieldDeclaration.getOffsetInBits(), fieldDeclaration.getSizeInBits(),
                    lhsExpr.getType());
            final Expression newValueExpr = AstUtils.newBinaryExpr(oldValueReadExpr, assign.getRightArgument(), assign);
            compoundAssign = newWriteBitFieldCall(externalScheme.getWriteBitFieldFunctionName(),
                    temporaryId.deepCopy(true), fieldDeclaration.getOffsetInBits(),
                    fieldDeclaration.getSizeInBits(), newValueExpr, lhsExpr.getType());
        } else {
            temporaryAssignedVal = newNonBitFieldDataExpr(lhsExpr);
            final Expression oldValueReadExpr = newReadCall(externalScheme.getReadFunctionName(),
                    temporaryId, lhsExpr.getType());
            final Expression newValueExpr = AstUtils.newBinaryExpr(oldValueReadExpr,
                    assign.getRightArgument(), assign);
            compoundAssign = newWriteCall(externalScheme.getWriteFunctionName(),
                    temporaryId.deepCopy(true), newValueExpr, lhsExpr.getType());
        }

        // Construct the final replacement

        final Expression temporaryAssign = new Assign(Location.getDummyLocation(),
                temporaryId.deepCopy(true), temporaryAssignedVal);

        final LinkedList<Expression> exprs = Lists.newList(temporaryAssign);
        exprs.add(compoundAssign);

        lhsExpr.setIsNxTransformed(true);

        return exprs;

    }

    private LinkedList<Expression> transformRead(Expression expr, ExternalExprBlockData blockData) {
        if (!needsTransformation(expr)) {
            return Lists.newList(expr);
        }

        final ExternalScheme externalScheme = getExternalScheme(expr);
        final Expression replacement;

        if (isBitFieldReference(expr)) {
            final FieldRef fieldRef = (FieldRef) expr;
            final FieldDeclaration fieldDeclaration = fieldRef.getDeclaration();

            replacement = newReadBitFieldCall(externalScheme.getReadBitFieldFunctionName(),
                    newBitFieldDataExpr(fieldRef), fieldDeclaration.getOffsetInBits(),
                    fieldDeclaration.getSizeInBits(), expr.getType());
        } else {
            replacement = newReadCall(externalScheme.getReadFunctionName(),
                    newNonBitFieldDataExpr(expr), expr.getType());
        }

        expr.setIsNxTransformed(true);

        return Lists.newList(replacement);
    }

    private FunctionCall newWriteBitFieldCall(String funName, Expression target,
                int offsetInBits, int sizeInBits, Expression assignedValue,
                Optional<Type> callType) {
        final FunctionCall result = AstUtils.newNormalCall(funName, target,
                AstUtils.newIntegerConstant(offsetInBits),
                AstUtils.newIntegerConstant(sizeInBits),
                assignedValue);
        result.setIsLvalue(false);
        result.setType(callType);

        return result;
    }

    private FunctionCall newWriteCall(String funName, Expression target,
            Expression value, Optional<Type> callType) {
        final FunctionCall result = AstUtils.newNormalCall(funName, target, value);
        result.setIsLvalue(false);
        result.setType(callType);
        return result;
    }

    private FunctionCall newReadBitFieldCall(String funName, Expression source,
                int offsetInBits, int sizeInBits, Optional<Type> callType) {
        final FunctionCall result = AstUtils.newNormalCall(funName, source,
                AstUtils.newIntegerConstant(offsetInBits),
                AstUtils.newIntegerConstant(sizeInBits));
        result.setIsLvalue(false);
        result.setType(callType);
        return result;
    }

    private FunctionCall newReadCall(String funName, Expression source, Optional<Type> callType) {
        final FunctionCall result = AstUtils.newNormalCall(funName, source);
        result.setIsLvalue(false);
        result.setType(callType);
        return result;
    }

    private AddressOf newBitFieldDataExpr(FieldRef bitFieldRef) {
        final AddressOf result = new AddressOf(Location.getDummyLocation(),
                bitFieldRef.getArgument());
        result.setIsLvalue(false);
        result.setType(Optional.<Type>of(new PointerType(bitFieldRef.getArgument().getType().get())));
        bitFieldRef.getArgument().setParenthesesCount(1);
        return result;
    }

    private FieldRef newNonBitFieldDataExpr(Expression expr) {
        final FieldRef result = new FieldRef(Location.getDummyLocation(), expr,
                ExternalBaseTransformer.getBaseFieldName());
        result.setIsLvalue(false);
        expr.setParenthesesCount(1);
        return result;
    }

    private DataDecl newTemporaryVariable(String name) {
        // Identifier declarator

        final IdentifierDeclarator identDecl = new IdentifierDeclarator(Location.getDummyLocation(),
                name);
        identDecl.setUniqueName(Optional.of(name));

        // Pointer declarator

        final PointerDeclarator ptrDecl = new PointerDeclarator(Location.getDummyLocation(),
                Optional.<Declarator>of(identDecl));

        // Inner declaration

        final VariableDecl variableDecl = new VariableDecl(Location.getDummyLocation(),
                Optional.<Declarator>of(ptrDecl), Lists.<Attribute>newList(),
                Optional.<AsmStmt>absent());
        variableDecl.setInitializer(Optional.<Expression>absent());

        // Outer declaration

        return new DataDecl(
                Location.getDummyLocation(),
                AstUtils.newRidsList(RID.VOID),
                Lists.<Declaration>newList(variableDecl)
        );
    }

    private boolean needsTransformation(Expression expr) {
        return (expr instanceof FieldRef || expr instanceof ArrayRef
                    || expr instanceof Identifier || expr instanceof Dereference)
                && !VariousUtils.getBooleanValue(expr.getIsNxTransformed())
                && expr.getType() != null
                && expr.getType().isPresent()
                && expr.getType().get().isExternalBaseType();
    }

    private ExternalScheme getExternalScheme(Expression expr) {
        return ((ArithmeticType) expr.getType().get()).getExternalScheme().get();
    }

    private boolean isBitFieldReference(Expression expr) {
        return expr instanceof FieldRef
                && ((FieldRef) expr).getDeclaration().isBitField();
    }
}
