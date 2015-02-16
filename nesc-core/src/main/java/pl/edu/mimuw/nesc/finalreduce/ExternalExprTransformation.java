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

import static com.google.common.base.Preconditions.checkArgument;
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
        } else if (expr instanceof Increment) {
            return transformIncrement((Increment) expr, blockData);
        } else {
            return transformRead(expr, blockData);
        }
    }

    private LinkedList<Expression> transformSimpleAssign(Assign assign, ExternalExprBlockData blockData) {
        if (!needsTransformation(assign.getLeftArgument())) {
            return Lists.<Expression>newList(assign);
        }

        final Expression lhsExpr = assign.getLeftArgument();
        final ExternalCallFactory callFactory = newFactoryForExpr(lhsExpr);
        final Expression replacement = callFactory.newWriteCall(callFactory.newDataExpr(lhsExpr),
                assign.getRightArgument(), assign.getType());

        lhsExpr.setIsNxTransformed(true);

        return Lists.newList(replacement);
    }

    private LinkedList<Expression> transformCompoundAssign(Assignment assign, ExternalExprBlockData blockData) {
        if (!needsTransformation(assign.getLeftArgument())) {
            return Lists.<Expression>newList(assign);
        }

        final Expression lhsExpr = assign.getLeftArgument();
        final ExternalCallFactory callFactory = newFactoryForExpr(lhsExpr);
        assign.getRightArgument().setParenthesesCount(1);

        // Create and add the declaration of the temporary variable

        final String temporaryName = nameMangler.mangle(NAME_TEMPORARY_VAR);
        blockData.getEnclosingBlock().get().getDeclarations().addFirst(newDataTemporary(temporaryName));

        // Prepare the temporary identifier

        final Identifier temporaryId = AstUtils.newIdentifier(temporaryName,
                temporaryName, false, true);
        temporaryId.setType(Optional.<Type>of(new PointerType(new VoidType())));
        temporaryId.setIsLvalue(true);

        // Make the transformation

        final Expression temporaryAssignedVal = callFactory.newDataExpr(lhsExpr);
        final Expression oldValueReadExpr = callFactory.newReadCall(temporaryId, lhsExpr.getType());
        final Expression newValueExpr = AstUtils.newBinaryExpr(oldValueReadExpr, assign.getRightArgument(), assign);
        final Expression compoundAssign = callFactory.newWriteCall(temporaryId.deepCopy(true),
                newValueExpr, lhsExpr.getType());

        // Construct the final replacement

        final Expression temporaryAssign = new Assign(Location.getDummyLocation(),
                temporaryId.deepCopy(true), temporaryAssignedVal);

        final LinkedList<Expression> exprs = Lists.newList(temporaryAssign);
        exprs.add(compoundAssign);

        lhsExpr.setIsNxTransformed(true);

        return exprs;
    }

    private LinkedList<Expression> transformIncrement(Increment increment, ExternalExprBlockData blockData) {
        if (!needsTransformation(increment.getArgument())) {
            return Lists.<Expression>newList(increment);
        }

        if (increment instanceof Preincrement || increment instanceof Predecrement) {
            return transformPreOperators(increment, blockData);
        } else if (increment instanceof Postincrement || increment instanceof Postdecrement) {
            return transformPostOperators(increment, blockData);
        } else {
            throw new RuntimeException("unexpected class of increment expression '"
                    + increment.getClass().getCanonicalName() + "'");
        }
    }

    private LinkedList<Expression> transformPreOperators(Increment increment, ExternalExprBlockData blockData) {
        /* Use the fact that '++e' is equivalent to 'e += 1' and '--e' to
           'e -= 1'. */

        final Expression one = AstUtils.newIntegerConstant(1);
        final Assignment equivalentExpr = increment instanceof Preincrement
                ? new PlusAssign(Location.getDummyLocation(), increment.getArgument(), one)
                : new MinusAssign(Location.getDummyLocation(), increment.getArgument(), one);

        equivalentExpr.setIsLvalue(increment.getIsLvalue());
        equivalentExpr.setType(increment.getType());
        equivalentExpr.setParenthesesCount(1);
        equivalentExpr.setIsPasted(increment.getIsPasted());

        return transformCompoundAssign(equivalentExpr, blockData);
    }

    private LinkedList<Expression> transformPostOperators(Increment increment, ExternalExprBlockData blockData) {
        return new PostOperatorsTransformer(increment, blockData).transform();
    }

    private LinkedList<Expression> transformRead(Expression expr, ExternalExprBlockData blockData) {
        if (!needsTransformation(expr)) {
            return Lists.newList(expr);
        }

        final ExternalCallFactory callFactory = newFactoryForExpr(expr);
        final Expression replacement = callFactory.newReadCall(callFactory.newDataExpr(expr), expr.getType());
        expr.setIsNxTransformed(true);

        return Lists.newList(replacement);
    }

    private DataDecl newDataTemporary(String name) {
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

    private ExternalCallFactory newFactoryForExpr(Expression expr) {
        final ExternalScheme scheme = ((ArithmeticType) expr.getType().get())
                .getExternalScheme().get();

        if (expr instanceof FieldRef) {
            final FieldDeclaration fieldDeclaration = ((FieldRef) expr).getDeclaration();
            return fieldDeclaration.isBitField()
                    ? new BitFieldCallFactory(scheme, fieldDeclaration)
                    : new NonBitFieldCallFactory(scheme);
        } else {
            return new NonBitFieldCallFactory(scheme);
        }
    }

    private boolean isBitFieldReference(Expression expr) {
        return expr instanceof FieldRef
                && ((FieldRef) expr).getDeclaration().isBitField();
    }

    private interface ExternalCallFactory {
        Expression newDataExpr(Expression expr);
        FunctionCall newReadCall(Expression source, Optional<Type> resultType);
        FunctionCall newWriteCall(Expression target, Expression value, Optional<Type> resultType);
    }

    private static final class BitFieldCallFactory implements ExternalCallFactory {
        private final ExternalScheme scheme;
        private final int offsetInBits;
        private final int sizeInBits;

        private BitFieldCallFactory(ExternalScheme scheme, FieldDeclaration declaration) {
            this.scheme = scheme;
            this.offsetInBits = declaration.getOffsetInBits();
            this.sizeInBits = declaration.getSizeInBits();
        }

        @Override
        public Expression newDataExpr(Expression bitFieldRef) {
            final FieldRef afterCast = (FieldRef) bitFieldRef;

            final AddressOf result = new AddressOf(Location.getDummyLocation(),
                    afterCast.getArgument());
            result.setIsLvalue(false);
            result.setType(Optional.<Type>of(new PointerType(afterCast.getArgument().getType().get())));
            afterCast.getArgument().setParenthesesCount(1);

            return result;
        }

        @Override
        public FunctionCall newReadCall(Expression source, Optional<Type> resultType) {
            final FunctionCall result = AstUtils.newNormalCall(
                    scheme.getReadBitFieldFunctionName(),
                    source,
                    AstUtils.newIntegerConstant(offsetInBits),
                    AstUtils.newIntegerConstant(sizeInBits)
            );
            result.setIsLvalue(false);
            result.setType(resultType);
            return result;
        }

        @Override
        public FunctionCall newWriteCall(Expression target, Expression value, Optional<Type> resultType) {
            final FunctionCall result = AstUtils.newNormalCall(
                    scheme.getWriteBitFieldFunctionName(),
                    target,
                    AstUtils.newIntegerConstant(offsetInBits),
                    AstUtils.newIntegerConstant(sizeInBits),
                    value
            );
            result.setIsLvalue(false);
            result.setType(resultType);
            return result;
        }
    }

    private static final class NonBitFieldCallFactory implements ExternalCallFactory {
        private final ExternalScheme scheme;

        private NonBitFieldCallFactory(ExternalScheme scheme) {
            this.scheme = scheme;
        }

        @Override
        public Expression newDataExpr(Expression expr) {
            final FieldRef result = new FieldRef(Location.getDummyLocation(), expr,
                    ExternalBaseTransformer.getBaseFieldName());
            result.setIsLvalue(false);
            expr.setParenthesesCount(1);
            return result;
        }

        @Override
        public FunctionCall newReadCall(Expression source, Optional<Type> resultType) {
            final FunctionCall result = AstUtils.newNormalCall(scheme.getReadFunctionName(),
                    source);
            result.setIsLvalue(false);
            result.setType(resultType);
            return result;
        }

        @Override
        public FunctionCall newWriteCall(Expression target, Expression value, Optional<Type> resultType) {
            final FunctionCall result = AstUtils.newNormalCall(scheme.getWriteFunctionName(),
                    target, value);
            result.setIsLvalue(false);
            result.setType(resultType);
            return result;
        }
    }

    /**
     * Class that facilities transforming of post operators expressions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class PostOperatorsTransformer {
        private final Increment increment;
        private final ExternalExprBlockData blockData;

        private final Expression incrementedObj;
        private final ExternalCallFactory callFactory;

        private Identifier dataTemporaryIdent;
        private Identifier valueTemporaryIdent;

        private Expression dataInitExpr;
        private Expression assignExpr;

        private PostOperatorsTransformer(Increment increment, ExternalExprBlockData blockData) {
            checkNotNull(increment, "increment cannot be null");
            checkNotNull(blockData, "block data cannot be null");
            checkArgument(increment instanceof Postincrement || increment instanceof Postdecrement,
                    "invalid expression for post operators transformer '" + increment.getClass().getCanonicalName()
                            + "'");

            this.increment = increment;
            this.blockData = blockData;
            this.incrementedObj = increment.getArgument();
            this.callFactory = newFactoryForExpr(increment.getArgument());
        }

        private LinkedList<Expression> transform() {
            declareTemporaries();
            generateCommonExpressions();
            updateTransformedExprState();
            return generateFinalExpressions();
        }

        private void declareTemporaries() {
            // Create and declare necessary temporary variables

            final String dataTemporaryName = nameMangler.mangle(NAME_TEMPORARY_VAR);
            final String valueTemporaryName = nameMangler.mangle(NAME_TEMPORARY_VAR);
            final LinkedList<Declaration> blockDecls = blockData.getEnclosingBlock().get().getDeclarations();

            blockDecls.addFirst(AstUtils.newSimpleDeclaration(valueTemporaryName,
                    valueTemporaryName, true, Optional.<Expression>absent(),
                    incrementedObj.getType().get().toAstType()));
            blockDecls.addFirst(newDataTemporary(dataTemporaryName));

            // Prepare identifiers

            dataTemporaryIdent = AstUtils.newIdentifier(dataTemporaryName,
                    dataTemporaryName, false, true);
            dataTemporaryIdent.setIsLvalue(true);
            dataTemporaryIdent.setType(Optional.<Type>of(new PointerType(new VoidType())));

            valueTemporaryIdent = AstUtils.newIdentifier(valueTemporaryName,
                    valueTemporaryName, false, true);
            valueTemporaryIdent.setIsLvalue(true);
            valueTemporaryIdent.setType(incrementedObj.getType());
            valueTemporaryIdent.setIsNxTransformed(true);
        }

        private void generateCommonExpressions() {
            dataInitExpr = callFactory.newDataExpr(incrementedObj);

            // Generate the expression that only reads the old value

            final Expression oldValueExpr = callFactory.newReadCall(dataTemporaryIdent.deepCopy(true),
                    incrementedObj.getType());

            // Generate the expression that saves value the before incrementing it

            final Assign oldValueAssign = new Assign(Location.getDummyLocation(),
                    valueTemporaryIdent.deepCopy(true), oldValueExpr);
            oldValueAssign.setIsLvalue(false);
            oldValueAssign.setType(incrementedObj.getType());
            oldValueAssign.setParenthesesCount(1);

            // Generate the expression that saves the new value

            final Expression newValueExpr = AstUtils.newBinaryExpr(oldValueAssign, increment);

            assignExpr = callFactory.newWriteCall(dataTemporaryIdent.deepCopy(true),
                    newValueExpr, incrementedObj.getType());
        }

        private void updateTransformedExprState() {
            incrementedObj.setIsNxTransformed(true);
        }

        private LinkedList<Expression> generateFinalExpressions() {
            final Expression dataTmpAssign = new Assign(Location.getDummyLocation(),
                    dataTemporaryIdent.deepCopy(true), dataInitExpr);

            final LinkedList<Expression> replacement = new LinkedList<Expression>();
            replacement.add(dataTmpAssign);
            replacement.add(assignExpr);
            replacement.add(valueTemporaryIdent.deepCopy(true));

            return replacement;
        }
    }
}
