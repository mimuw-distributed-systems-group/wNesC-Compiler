package pl.edu.mimuw.nesc.intermediate;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.wiresgraph.IntermediateFunctionData;
import pl.edu.mimuw.nesc.wiresgraph.SpecificationElementNode;
import pl.edu.mimuw.nesc.wiresgraph.WiresGraph;
import pl.edu.mimuw.nesc.wiresgraph.WiringEdge;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * <p>Class responsible for generating the intermediate functions that provide
 * program execution according to connections in configurations.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SimpleIntermediateGenerator implements IntermediateGenerator {
    /**
     * Graph of commands and events that reflect the connections.
     */
    private final WiresGraph wiresGraph;

    /**
     * Name mangler used for generating names of local variables.
     */
    private final NameMangler nameMangler;

    /**
     * Map with unique names of type definitions as keys and names of
     * corresponding combining functions as values.
     */
    private final Map<String, String> combiningFunctions;

    /**
     * Initializes this generator to generate functions according to the given
     * graph and using the given name mangler for names generation. For a wires
     * graph, the intermediate functions generation shall occur exactly once.
     *
     * @param graph Graph that specifies the connections and contains empty
     *              intermediate functions.
     * @param combiningFunctions Map with unique names of type definitions as
     *                           keys and names of corresponding combining
     *                           functions as values.
     * @param nameMangler Mangler used for mangling names.
     * @throws NullPointerException One of parameters is <code>null</code>.
     */
    public SimpleIntermediateGenerator(WiresGraph graph, Map<String, String> combiningFunctions,
            NameMangler nameMangler) {
        checkNotNull(graph, "graph cannot be null");
        checkNotNull(combiningFunctions, "combining functions map cannot be null");
        checkNotNull(nameMangler, "name mangler cannot be null");

        this.wiresGraph = graph;
        this.combiningFunctions = combiningFunctions;
        this.nameMangler = nameMangler;
    }

    @Override
    public Multimap<String, FunctionDecl> generate() {
        final Multimap<String, FunctionDecl> intermediateFunctions = HashMultimap.create();

        for (SpecificationElementNode node : wiresGraph.getNodes().values()) {
            if (node.getEntityData().isImplemented()) {
                continue;
            }

            final IntermediateFunctionData funData = (IntermediateFunctionData) node.getEntityData();

            if (!funData.returnsVoid() && !funData.getDefaultImplementationUniqueName().isPresent()) {
                generateValidResultFunctionUniqueName(node.getComponentName(), node.getInterfaceRefName(),
                        node.getEntityName(), funData);
                intermediateFunctions.put(node.getSpecificationElementFullName(),
                        generateValidResultFunction(funData, node.getSuccessors()));
            }

            intermediateFunctions.put(node.getSpecificationElementFullName(),
                    generateFunction(funData, node.getSuccessors()));
        }

        return intermediateFunctions;
    }

    private FunctionDecl generateValidResultFunction(IntermediateFunctionData funData, List<WiringEdge> successors) {
        // Check if a trivial function can be generated
        for (WiringEdge edge : successors) {
            if (edge.getDestinationNode().getEntityData().isImplemented()
                    && !edge.getSourceParameters().isPresent()) {
                return generateTrivialValidResultFun(funData);
            }
        }

        return generateNontrivialValidResultFun(funData, successors);
    }

    private FunctionDecl generateTrivialValidResultFun(IntermediateFunctionData funData) {
        final FunctionDecl validResultFun = generateEmptyValidResultFunction(funData);
        final LinkedList<Statement> stmts = ((CompoundStmt) validResultFun.getBody()).getStatements();
        stmts.add(AstUtils.newReturnStmt(1));
        return validResultFun;
    }

    private FunctionDecl generateNontrivialValidResultFun(IntermediateFunctionData funData,
            List<WiringEdge> successors) {
        final FunctionDecl validResultFun = generateEmptyValidResultFunction(funData);
        final LinkedList<Statement> statements = ((CompoundStmt) validResultFun.getBody()).getStatements();

        for (WiringEdge edge : successors) {
            statements.add(generateValidResultFunctionStep(funData, edge));
        }

        statements.add(AstUtils.newReturnStmt(0));  // no valid result

        return validResultFun;
    }

    private Statement generateValidResultFunctionStep(IntermediateFunctionData funData,
            WiringEdge edge) {
        final SpecificationElementNode successor = edge.getDestinationNode();

        // Add the instance parameters conditions

        final List<Expression> instanceParams = AstUtils.newIdentifiersList(funData.getInstanceParametersNames());
        final LinkedList<Expression> conditions = edge.getSourceParameters().isPresent()
                ? AstUtils.zipWithEq(instanceParams, edge.getSourceParameters().get())
                : Lists.<Expression>newList();

        // Add the call

        if (!successor.getEntityData().isImplemented()) {
            conditions.add(generateValidResultFunctionCall(
                    edge,
                    (IntermediateFunctionData) successor.getEntityData(),
                    funData.getInstanceParametersNames())
            );
        }

        // Generate the statement

        return conditions.isEmpty()
                ? AstUtils.newReturnStmt(1)
                : new IfStmt(
                        Location.getDummyLocation(),
                        AstUtils.newLogicalAnd(conditions),
                        AstUtils.newReturnStmt(1),
                        Optional.<Statement>absent()
                );
    }

    private FunctionDecl generateEmptyValidResultFunction(IntermediateFunctionData funData) {
        // Function parameters (instance parameters of the command or event)

        Declarator declarator = funData.getIntermediateFunction().getDeclarator();
        while (!(declarator instanceof FunctionDeclarator)) {
            declarator = ((NestedDeclarator) declarator).getDeclarator().get();
        }
        final FunctionDeclarator intermediateFunDeclarator = (FunctionDeclarator) declarator;
        final List<Declaration> instanceParams = intermediateFunDeclarator.getParameters().subList(
                0, funData.getInstanceParametersNames().size());
        final LinkedList<Declaration> params = AstUtils.deepCopyNodes(instanceParams, true);

        // Identifier declarator

        final IdentifierDeclarator identDeclarator = new IdentifierDeclarator(
                Location.getDummyLocation(),
                funData.getValidResultFunctionUniqueName().get()
        );
        identDeclarator.setUniqueName(Optional.of(funData.getValidResultFunctionUniqueName().get()));
        identDeclarator.setIsNestedInNescEntity(false);

        // Function declarator

        final FunctionDeclarator funDeclarator = new FunctionDeclarator(
                Location.getDummyLocation(),
                Optional.<Declarator>of(identDeclarator),
                params,
                Optional.<LinkedList<Declaration>>absent(),
                Lists.<TypeElement>newList()
        );

        final FunctionDecl funDecl = new FunctionDecl(
                Location.getDummyLocation(),
                funDeclarator,
                AstUtils.newRidsList(RID.STATIC, RID.INT),
                Lists.<Attribute>newList(),
                AstUtils.newEmptyCompoundStmt(),
                false
        );
        funDecl.setOldParms(Lists.<Declaration>newList());

        return funDecl;
    }

    private void generateValidResultFunctionUniqueName(String componentName,
                Optional<String> interfaceRefName, String entityName,
                IntermediateFunctionData funData) {

        if (funData.getValidResultFunctionUniqueName().isPresent()) {
            return;
        }

        final String unmangledName = interfaceRefName.isPresent()
                ? format("%s__%s__%s__result", componentName, interfaceRefName.get(), entityName)
                : format("%s__%s__result", componentName, entityName);
        funData.setValidResultFunctionUniqueName(nameMangler.mangle(unmangledName));
    }

    private FunctionDecl generateFunction(IntermediateFunctionData funData,
            List<WiringEdge> successors) {
        final Optional<String> combiningFunName = resolveCombiningFunction(funData.getIntermediateFunction());
        final LocalVariables localVariables = createLocalVariables(funData.returnsVoid(),
                AstUtils.extractReturnType(funData.getIntermediateFunction()));

        final CompoundStmt funBody = (CompoundStmt) funData.getIntermediateFunction().getBody();
        final LinkedList<Statement> stmts = funBody.getStatements();

        funBody.getDeclarations().addAll(localVariables.declarations);

        for (WiringEdge edge : successors) {
            stmts.add(generateFunctionStep(funData, edge, localVariables, combiningFunName));
        }

        if (funData.getDefaultImplementationUniqueName().isPresent()
                && (successors.isEmpty() || localVariables.calledUniqueName.isPresent())) {
            stmts.add(generateDefaultImplStmt(funData, localVariables));
        }

        if (!funData.returnsVoid()) {
            stmts.add(AstUtils.newReturnStmt(localVariables.resultUniqueName.get()));
        }

        return funData.getIntermediateFunction();
    }

    private Statement generateDefaultImplStmt(IntermediateFunctionData funData, LocalVariables variables) {
        // Condition for calling the default implementation

        final Optional<Expression> condition = variables.calledUniqueName.isPresent()
                ? Optional.<Expression>of(new Not(
                        Location.getDummyLocation(),
                        AstUtils.newIdentifier(variables.calledUniqueName.get())
                  ))
                : Optional.<Expression>absent();

        // Call of the default implementation

        final FunctionCall defaultImplCall = AstUtils.newNormalCall(funData.getDefaultImplementationUniqueName().get(),
                funData.getParametersNames());

        final Statement callStmt = funData.returnsVoid()
                ? new ExpressionStmt(Location.getDummyLocation(), defaultImplCall)
                : new ReturnStmt(Location.getDummyLocation(), Optional.<Expression>of(defaultImplCall));

        return !condition.isPresent()
                ? callStmt
                : new IfStmt(
                    Location.getDummyLocation(),
                    condition.get(),
                    callStmt,
                    Optional.<Statement>absent()
                );
    }

    private Statement generateFunctionStep(IntermediateFunctionData funData, WiringEdge edge,
            LocalVariables variables, Optional<String> combiningFunName) {

        final LinkedList<Expression> conditions = generateConnectionConditions(funData, edge);
        final FunctionCall successorCall = generateConnectionCall(funData, edge);
        final Statement connectionStmt = generateConnectionStmt(funData, variables,
                combiningFunName, successorCall);

        return conditions.isEmpty()
                ? connectionStmt
                : new IfStmt(
                    Location.getDummyLocation(),
                    AstUtils.newLogicalAnd(conditions),
                    connectionStmt,
                    Optional.<Statement>absent()
                );
    }

    private LinkedList<Expression> generateConnectionConditions(IntermediateFunctionData funData,
            WiringEdge edge) {
        final SpecificationElementNode successor = edge.getDestinationNode();
        final List<Expression> instanceParams = AstUtils.newIdentifiersList(funData.getInstanceParametersNames());
        final LinkedList<Expression> conditions = edge.getSourceParameters().isPresent()
                ? AstUtils.zipWithEq(instanceParams, edge.getSourceParameters().get())
                : Lists.<Expression>newList();

        if (!funData.returnsVoid() && !successor.getEntityData().isImplemented()) {
            conditions.add(generateValidResultFunctionCall(edge, (IntermediateFunctionData) successor.getEntityData(),
                    funData.getInstanceParametersNames()));
        }

        return conditions;
    }

    private FunctionCall generateConnectionCall(IntermediateFunctionData funData, WiringEdge edge) {
        final LinkedList<Expression> allParameters = edge.getDestinationParameters().isPresent()
                ? AstUtils.deepCopyNodes(edge.getDestinationParameters().get(), true)
                : Lists.<Expression>newList();
        final int usedInstanceParamsCount = edge.getSourceParameters().isPresent()
                ? edge.getSourceParameters().get().size()
                : 0;

        final List<String> remainingParameters = funData.getParametersNames().subList(
                usedInstanceParamsCount, funData.getParametersNames().size());
        allParameters.addAll(AstUtils.newIdentifiersList(remainingParameters));

        return AstUtils.newNormalCall(
                edge.getDestinationNode().getEntityData().getUniqueName(),
                allParameters
        );
    }

    private CompoundStmt generateConnectionStmt(IntermediateFunctionData funData, LocalVariables variables,
            Optional<String> combiningFunName, FunctionCall successorCall) {
        // Calling successor function

        final CompoundStmt stmt = AstUtils.newEmptyCompoundStmt();
        final Expression resultExpr = funData.returnsVoid()
                ? successorCall
                : generateConnectionResultAssign(variables.resultUniqueName.get(),
                        variables.calledUniqueName, combiningFunName,
                        successorCall);

        stmt.getStatements().add(new ExpressionStmt(Location.getDummyLocation(), resultExpr));

        // Setting 'called' variable

        if (variables.calledUniqueName.isPresent()) {
            final Assign calledVarAssign = new Assign(
                    Location.getDummyLocation(),
                    AstUtils.newIdentifier(variables.calledUniqueName.get()),
                    AstUtils.newIntegerConstant(1)
            );

            stmt.getStatements().add(new ExpressionStmt(Location.getDummyLocation(), calledVarAssign));
        }

        return stmt;
    }

    private Assign generateConnectionResultAssign(String resultUniqueName, Optional<String> calledUniqueName,
            Optional<String> combiningFunName, FunctionCall successorCall) {
        final Identifier resultIdentifier = AstUtils.newIdentifier(resultUniqueName);
        final Expression newResultExpr;

        if (combiningFunName.isPresent()) {
            final LinkedList<Expression> combiningFunParams = Lists.newList();
            combiningFunParams.add(AstUtils.newIdentifier(resultUniqueName));
            combiningFunParams.add(successorCall);

            final FunctionCall combiningFunCall = AstUtils.newNormalCall(combiningFunName.get(),
                    combiningFunParams);

            newResultExpr = !calledUniqueName.isPresent()
                    ? combiningFunCall
                    : new Conditional(
                            Location.getDummyLocation(),
                            AstUtils.newIdentifier(calledUniqueName.get()),
                            Optional.<Expression>of(combiningFunCall),
                            successorCall
                    );
        } else {
            newResultExpr = successorCall;
        }

        return new Assign(
                Location.getDummyLocation(),
                resultIdentifier,
                newResultExpr
        );
    }

    private FunctionCall generateValidResultFunctionCall(WiringEdge edge, IntermediateFunctionData successorData,
            List<String> instanceParamsNames) {
        final LinkedList<Expression> callParameters;

        if (edge.getDestinationParameters().isPresent()) {
            callParameters = AstUtils.deepCopyNodes(edge.getDestinationParameters().get(), true);
        } else if (!instanceParamsNames.isEmpty() && !edge.getSourceParameters().isPresent()) {
            callParameters = AstUtils.newIdentifiersList(instanceParamsNames);
        } else {
            callParameters = Lists.newList();
        }

        final SpecificationElementNode successor = edge.getDestinationNode();

        generateValidResultFunctionUniqueName(successor.getComponentName(), successor.getInterfaceRefName(),
                successor.getEntityName(), successorData);

        return AstUtils.newNormalCall(successorData.getValidResultFunctionUniqueName().get(), callParameters);
    }

    /**
     * Get the name of the combining function for the return type of given
     * function.
     *
     * @param funDecl AST node of function.
     * @return Name of the combining function for the return type of given
     *         function. The object is absent if no combining function is
     *         associated with the return type.
     */
    private Optional<String> resolveCombiningFunction(FunctionDecl funDecl) {
        /* Check if the return type of the function is entirely defined with
           a type elements list. */

        if (AstUtils.declaratorAffectsReturnType(funDecl)) {
            return Optional.absent();
        }

        final Optional<String> typedefUniqueName = TypeElementUtils.getTypedefUniqueName(funDecl.getModifiers());
        return typedefUniqueName.isPresent()
                ? Optional.fromNullable(combiningFunctions.get(typedefUniqueName.get()))
                : Optional.<String>absent();
    }

    private LocalVariables createLocalVariables(boolean returnsVoid, AstType returnType) {
        final LinkedList<DataDecl> declarations = new LinkedList<>();
        final String calledUniqueName = nameMangler.mangle("called");

        declarations.add(AstUtils.newSimpleDeclaration(calledUniqueName, calledUniqueName,
                false, Optional.of(AstUtils.newIntegerConstant(0)), RID.INT));

        if (returnsVoid) {
            return new LocalVariables(Optional.<String>absent(), Optional.of(calledUniqueName),
                    declarations);
        } else {
            final String resultUniqueName = nameMangler.mangle("result");
            declarations.add(AstUtils.newSimpleDeclaration(resultUniqueName, resultUniqueName,
                    false, Optional.<Expression>absent(), returnType));

            return new LocalVariables(Optional.of(resultUniqueName), Optional.of(calledUniqueName),
                    declarations);
        }
    }

    /**
     * Class that represents local variables declared in an intermediate
     * function.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class LocalVariables {
        private final Optional<String> resultUniqueName;
        private final Optional<String> calledUniqueName;
        private final List<DataDecl> declarations;

        private LocalVariables(Optional<String> resultUniqueName, Optional<String> calledUniqueName,
                List<DataDecl> declarations) {
            checkNotNull(resultUniqueName, "the unique name of 'result' variable cannot be null");
            checkNotNull(calledUniqueName, "the unique name of 'called' variable cannot be null");
            checkNotNull(declarations, "declarations of the variables cannot be null");

            this.resultUniqueName = resultUniqueName;
            this.calledUniqueName = calledUniqueName;
            this.declarations = declarations;
        }
    }
}
