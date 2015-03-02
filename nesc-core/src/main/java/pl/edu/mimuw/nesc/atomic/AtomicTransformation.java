package pl.edu.mimuw.nesc.atomic;

import com.google.common.base.Optional;
import java.util.LinkedList;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.NescCallKind;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.common.AtomicSpecification;
import pl.edu.mimuw.nesc.common.util.VariousUtils;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.type.VoidType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class responsible for transforming atomic statements to equivalent
 * statements that guarantee atomic execution.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class AtomicTransformation implements StmtTransformation<AtomicBlockData> {
    /**
     * Name that will be mangled to create unique names for variables that
     * will be used to store the result of the atomic start function.
     */
    private static final String ATOMIC_VARIABLE_BASE_NAME = "atomic_data";

    /**
     * Name used for variables that are assigned the returned value before
     * ending the atomic block.
     */
    private static final String ATOMIC_RETURN_VARIABLE_BASE_NAME = "atomic_ret";

    /**
     * Information necessary to transform 'atomic' statements.
     */
    private final AtomicSpecification atomicSpecification;

    /**
     * Object necessary for creating new names.
     */
    private final NameMangler nameMangler;

    /**
     * Visitor that modifies visited type elements of the return type of
     * a function for new variable that has assigned a returned expression
     * in an atomic block.
     */
    private final AtomicReturnTypeVisitor atomicReturnTypeVisitor;

    AtomicTransformation(AtomicSpecification atomicSpecification, NameMangler nameMangler) {
        checkNotNull(atomicSpecification, "atomic specification cannot be null");
        checkNotNull(nameMangler, "name mangler cannot be null");
        this.atomicSpecification = atomicSpecification;
        this.nameMangler = nameMangler;
        this.atomicReturnTypeVisitor = new AtomicReturnTypeVisitor(nameMangler);
    }

    @Override
    public LinkedList<Statement> transform(Statement stmt, AtomicBlockData arg) {
        if (stmt instanceof AtomicStmt) {
            return transform((AtomicStmt) stmt, arg);
        } else if (stmt instanceof ReturnStmt) {
            return transform((ReturnStmt) stmt, arg);
        } else if (stmt instanceof ContinueStmt) {
            return transform((ContinueStmt) stmt, arg);
        } else if (stmt instanceof BreakStmt) {
            return transform((BreakStmt) stmt, arg);
        } else if (stmt instanceof GotoStmt) {
            return transform((GotoStmt) stmt, arg);
        } else {
            return Lists.newList(stmt);
        }
    }

    /**
     * Perform the small transformation on a single atomic statement.
     *
     * @param stmt Atomic statement to transform.
     * @param stmtData Data about the atomic statement.
     * @return List with statements that will replace the given atomic
     *         statement. If it is empty, the atomic statement will be removed
     *         and no other statement takes its place.
     */
    private LinkedList<Statement> transform(AtomicStmt stmt, AtomicBlockData stmtData) {
        return stmtData.isInsideAtomicBlock()
                ? atomizeAtomicInsideAtomic(stmt, stmtData)
                : atomizeRealAtomic(stmt);
    }

    private LinkedList<Statement> atomizeAtomicInsideAtomic(AtomicStmt stmt, AtomicBlockData stmtData) {
        /* 'atomic' is removed from an atomic statement nested in an atomic
           block. We need to continue transforming the statement inside atomic
           because it wouldn't happen if the statement was returned. */
        return transform(stmt.getStatement(), stmtData);
    }

    private LinkedList<Statement> atomizeRealAtomic(AtomicStmt stmt) {
        final CompoundStmt result = stmt.getStatement() instanceof CompoundStmt
                ? (CompoundStmt) stmt.getStatement()
                : new CompoundStmt(
                Location.getDummyLocation(),
                Lists.<IdLabel>newList(),
                Lists.<Declaration>newList(),
                Lists.newList(stmt.getStatement())
        );

        final String atomicVariableUniqueName = nameMangler.mangle(ATOMIC_VARIABLE_BASE_NAME);

        result.setAtomicVariableUniqueName(Optional.of(atomicVariableUniqueName));
        result.getDeclarations().addFirst(createAtomicInitialCall(atomicVariableUniqueName));
        result.getStatements().addLast(createAtomicFinalCall(atomicVariableUniqueName));

        return Lists.<Statement>newList(result);
    }

    private DataDecl createAtomicInitialCall(String atomicVariableUniqueName) {
        // Type name

        final Typename atomicVarTypename = new Typename(
                Location.getDummyLocation(),
                atomicSpecification.getTypename()
        );

        atomicVarTypename.setIsGenericReference(false);
        atomicVarTypename.setUniqueName(atomicSpecification.getTypename());
        atomicVarTypename.setIsDeclaredInThisNescEntity(false);

        // Call to the atomic start function

        final Identifier funIdentifier = new Identifier(
                Location.getDummyLocation(), atomicSpecification.getStartFunctionName()
        );
        funIdentifier.setUniqueName(Optional.of(atomicSpecification.getStartFunctionName()));
        funIdentifier.setIsGenericReference(false);
        funIdentifier.setRefsDeclInThisNescEntity(false);
        funIdentifier.setType(Optional.<Type>absent());

        final FunctionCall atomicStartCall = new FunctionCall(
                Location.getDummyLocation(),
                funIdentifier,
                Lists.<Expression>newList(),
                NescCallKind.NORMAL_CALL
        );
        atomicStartCall.setType(Optional.<Type>absent());

        // Variable declaration

        final IdentifierDeclarator declarator = new IdentifierDeclarator(
                Location.getDummyLocation(),
                ATOMIC_VARIABLE_BASE_NAME
        );

        declarator.setIsNestedInNescEntity(true);
        declarator.setUniqueName(Optional.of(atomicVariableUniqueName));

        final VariableDecl atomicVariableDecl = new VariableDecl(
                Location.getDummyLocation(),
                Optional.<Declarator>of(declarator),
                Lists.<Attribute>newList(),
                Optional.<AsmStmt>absent()
        );

        atomicVariableDecl.setInitializer(Optional.<Expression>of(atomicStartCall));

        // Final declaration

        return new DataDecl(
                Location.getDummyLocation(),
                Lists.<TypeElement>newList(atomicVarTypename),
                Lists.<Declaration>newList(atomicVariableDecl)
        );
    }

    private ExpressionStmt createAtomicFinalCall(String atomicVariableUniqueName) {
        // Prepare identifiers

        final Identifier funIdentifier = new Identifier(
                Location.getDummyLocation(), atomicSpecification.getEndFunctionName()
        );
        funIdentifier.setRefsDeclInThisNescEntity(false);
        funIdentifier.setUniqueName(Optional.of(atomicSpecification.getEndFunctionName()));
        funIdentifier.setIsGenericReference(false);
        funIdentifier.setType(Optional.<Type>absent());

        final Identifier argIdentifier = new Identifier(
                Location.getDummyLocation(), ATOMIC_VARIABLE_BASE_NAME
        );
        argIdentifier.setRefsDeclInThisNescEntity(true);
        argIdentifier.setUniqueName(Optional.of(atomicVariableUniqueName));
        argIdentifier.setIsGenericReference(false);
        argIdentifier.setType(Optional.<Type>absent());

        // Prepare the call to the atomic end function

        final FunctionCall finalCall = new FunctionCall(
                Location.getDummyLocation(),
                funIdentifier,
                Lists.<Expression>newList(argIdentifier),
                NescCallKind.NORMAL_CALL
        );
        finalCall.setType(Optional.<Type>of(new VoidType()));

        return new ExpressionStmt(
                Location.getDummyLocation(),
                finalCall
        );
    }

    private LinkedList<Statement> transform(ReturnStmt stmt, AtomicBlockData stmtData) {
        if (!stmtData.isInsideAtomicBlock() || VariousUtils.getBooleanValue(stmt.getIsAtomicSafe())) {
            return Lists.<Statement>newList(stmt);
        }

        stmt.setIsAtomicSafe(true);

        return stmt.getValue().isPresent()
                ? atomizeExprReturnStmt(stmt.getValue().get(), stmtData)
                : atomizeVoidReturnStmt(stmt, stmtData);
    }

    private LinkedList<Statement> atomizeExprReturnStmt(Expression retExpr, AtomicBlockData stmtData) {
        final String retVariableUniqueName = nameMangler.mangle(ATOMIC_RETURN_VARIABLE_BASE_NAME);

        // Prepare the identifier of the returned variable

        final Identifier retIdentifier = new Identifier(
                Location.getDummyLocation(),
                ATOMIC_RETURN_VARIABLE_BASE_NAME
        );
        retIdentifier.setIsGenericReference(false);
        retIdentifier.setRefsDeclInThisNescEntity(true);
        retIdentifier.setUniqueName(Optional.of(retVariableUniqueName));
        retIdentifier.setType(retExpr.getType());

        // Prepare the new 'return' statement

        final ReturnStmt newRetStmt = new ReturnStmt(
                Location.getDummyLocation(),
                Optional.<Expression>of(retIdentifier)
        );
        newRetStmt.setIsAtomicSafe(true);

        // Prepare statements in the created block

        final LinkedList<Statement> stmts = new LinkedList<>();
        stmts.add(createAtomicFinalCall(stmtData.getAtomicVariableUniqueName().get()));
        stmts.add(newRetStmt);

        // Prepare the block

        final CompoundStmt resultStmt = new CompoundStmt(
                Location.getDummyLocation(),
                Lists.<IdLabel>newList(),
                Lists.<Declaration>newList(createReturnExprEvalVariable(stmtData.getFunctionReturnType().get(),
                        retVariableUniqueName, retExpr)),
                stmts
        );

        return Lists.<Statement>newList(resultStmt);
    }

    private DataDecl createReturnExprEvalVariable(AstType retType, String retVarUniqueName, Expression retExpr) {
        // Prepare the type

        final AstType usedType = retType.deepCopy(true);
        for (TypeElement typeElement : usedType.getQualifiers()) {
            typeElement.accept(atomicReturnTypeVisitor, null);
        }

        // Create and assign the identifier declarator

        final IdentifierDeclarator identifierDeclarator = new IdentifierDeclarator(
                Location.getDummyLocation(),
                ATOMIC_RETURN_VARIABLE_BASE_NAME
        );
        identifierDeclarator.setUniqueName(Optional.of(retVarUniqueName));

        /* Set to 'true' even if we aren't in a NesC entity to remangle the name
           if it appears in a generic component. If no, it has no effect. */
        identifierDeclarator.setIsNestedInNescEntity(true);

        if (!usedType.getDeclarator().isPresent()) {
            usedType.setDeclarator(Optional.<Declarator>of(identifierDeclarator));
        } else {
            final NestedDeclarator deepestDeclarator = DeclaratorUtils.getDeepestNestedDeclarator(
                    usedType.getDeclarator().get()).get();
            deepestDeclarator.setDeclarator(Optional.<Declarator>of(identifierDeclarator));
        }

        // Create the inner declaration

        final VariableDecl variableDecl = new VariableDecl(
                Location.getDummyLocation(),
                usedType.getDeclarator(),
                Lists.<Attribute>newList(),
                Optional.<AsmStmt>absent()
        );
        variableDecl.setInitializer(Optional.of(retExpr));

        // Return the final declaration

        return new DataDecl(
                Location.getDummyLocation(),
                usedType.getQualifiers(),
                Lists.<Declaration>newList(variableDecl)
        );
    }

    private LinkedList<Statement> atomizeVoidReturnStmt(ReturnStmt stmt, AtomicBlockData stmtData) {
        final LinkedList<Statement> result = new LinkedList<>();
        result.add(createAtomicFinalCall(stmtData.getAtomicVariableUniqueName().get()));
        result.add(stmt);
        return result;
    }

    private LinkedList<Statement> transform(BreakStmt stmt, AtomicBlockData stmtData) {
        final boolean isAtomicSafe = VariousUtils.getBooleanValue(stmt.getIsAtomicSafe());
        stmt.setIsAtomicSafe(true);
        return atomizeBreakingStmt(stmt, stmtData, isAtomicSafe);
    }

    private LinkedList<Statement> transform(ContinueStmt stmt, AtomicBlockData stmtData) {
        final boolean isAtomicSafe = VariousUtils.getBooleanValue(stmt.getIsAtomicSafe());
        stmt.setIsAtomicSafe(true);
        return atomizeBreakingStmt(stmt, stmtData, isAtomicSafe);
    }

    private LinkedList<Statement> atomizeBreakingStmt(Statement breakingStmt, AtomicBlockData stmtData,
            boolean isAtomicSafe) {
        if (!stmtData.isInsideBreakableAtomic() || isAtomicSafe) {
            return Lists.newList(breakingStmt);
        }

        final LinkedList<Statement> result = new LinkedList<>();
        result.add(createAtomicFinalCall(stmtData.getAtomicVariableUniqueName().get()));
        result.add(breakingStmt);
        return result;
    }

    private LinkedList<Statement> transform(GotoStmt stmt, AtomicBlockData stmtData) {
        if (!stmtData.isInsideAtomicBlock() || VariousUtils.getBooleanValue(stmt.getIsAtomicSafe())
                || !stmt.getToNonAtomicArea()) {
            return Lists.<Statement>newList(stmt);
        }

        stmt.setIsAtomicSafe(true);

        final LinkedList<Statement> result = Lists.<Statement>newList(stmt);
        result.addFirst(createAtomicFinalCall(stmtData.getAtomicVariableUniqueName().get()));
        return result;
    }
}
