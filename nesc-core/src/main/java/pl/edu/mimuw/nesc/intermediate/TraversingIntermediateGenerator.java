package pl.edu.mimuw.nesc.intermediate;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.Assign;
import pl.edu.mimuw.nesc.ast.gen.BreakStmt;
import pl.edu.mimuw.nesc.ast.gen.CaseLabel;
import pl.edu.mimuw.nesc.ast.gen.CompoundStmt;
import pl.edu.mimuw.nesc.ast.gen.DefaultLabel;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.ExpressionStmt;
import pl.edu.mimuw.nesc.ast.gen.FunctionCall;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.IfStmt;
import pl.edu.mimuw.nesc.ast.gen.LabeledStmt;
import pl.edu.mimuw.nesc.ast.gen.ReturnStmt;
import pl.edu.mimuw.nesc.ast.gen.Statement;
import pl.edu.mimuw.nesc.ast.gen.SwitchStmt;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.wiresgraph.IndexedNode;
import pl.edu.mimuw.nesc.wiresgraph.IntermediateFunctionData;
import pl.edu.mimuw.nesc.wiresgraph.SpecificationElementNode;
import pl.edu.mimuw.nesc.wiresgraph.WiresGraph;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TraversingIntermediateGenerator implements IntermediateGenerator {
    /**
     * Key in the implementations map associated with unconditional wiring.
     */
    private static final Optional<ImmutableList<BigInteger>> KEY_UNCONDITIONAL = Optional.absent();

    /**
     * Graph of wires in the NesC application.
     */
    private final WiresGraph wiresGraph;

    /**
     * Name mangler used to create unique names for variables.
     */
    private final NameMangler nameMangler;

    /**
     * Object responsible for determining combining functions to use in
     * generated intermediate functions.
     */
    private final CombiningFunctionResolver combiningFunctionResolver;

    /**
     * Map with wrappers for nodes.
     */
    private final Map<SpecificationElementNode, NodeWrapper> nodeWrappers = new HashMap<>();

    public TraversingIntermediateGenerator(WiresGraph graph, Map<String, String> combiningFunctions,
            NameMangler nameMangler) {
        checkNotNull(graph, "the wires graph cannot be null");
        checkNotNull(combiningFunctions, "combining functions map cannot be null");
        checkNotNull(nameMangler, "name mangler cannot be null");

        this.wiresGraph = graph;
        this.combiningFunctionResolver = new CombiningFunctionResolver(combiningFunctions);
        this.nameMangler = nameMangler;
    }

    @Override
    public Multimap<String, FunctionDecl> generate() {
        final ListMultimap<String, FunctionDecl> intermediateFunctions = ArrayListMultimap.create();

        for (SpecificationElementNode node : wiresGraph.getNodes().values()) {
            if (node.getEntityData().isImplemented()) {
                continue;
            }

            final IntermediateFunctionData funData = (IntermediateFunctionData) node.getEntityData();

            if (!funData.isCallSource()) {
                continue;
            }

            intermediateFunctions.put(node.getSpecificationElementFullName(),
                    generateFunction(node));
        }

        return intermediateFunctions;
    }

    private FunctionDecl generateFunction(SpecificationElementNode source) {
        final IntermediateFunctionData funData = (IntermediateFunctionData) source.getEntityData();

        // Get implementations connected to this used command or event

        final ListMultimap<Optional<ImmutableList<BigInteger>>, IndexedNode> impls =
                new ImplementationResolver(source, nodeWrappers).resolve();

        // Generate the function

        if (impls.isEmpty() && funData.getDefaultImplementationUniqueName().isPresent()
                || impls.size() == 1 && impls.get(KEY_UNCONDITIONAL).size() == 1) {
            return generateFunctionTrivial(funData, impls);
        } else if (funData.getDefaultImplementationUniqueName().isPresent()
                || !impls.get(KEY_UNCONDITIONAL).isEmpty()) {
            return generateFunctionNonTrivial(funData, impls);
        } else {
            return generateFunctionNotImplemented(funData);
        }
    }

    private FunctionDecl generateFunctionTrivial(IntermediateFunctionData funData,
                ListMultimap<Optional<ImmutableList<BigInteger>>, IndexedNode> impls) {

        final Optional<IndexedNode> onlySuccessor = !impls.isEmpty()
                ? Optional.of(impls.get(KEY_UNCONDITIONAL).get(0))
                : Optional.<IndexedNode>absent();

        // Generate the call to the implementation

        final FunctionCall implFunCall;

        if (onlySuccessor.isPresent()) {
            final LinkedList<Expression> callParameters = onlySuccessor.get().getIndices().isPresent()
                    ? AstUtils.newIntegerConstantsList(onlySuccessor.get().getIndices().get())
                    : new LinkedList<Expression>();
            callParameters.addAll(AstUtils.newIdentifiersList(funData.getParametersNames()));

            implFunCall = AstUtils.newNormalCall(onlySuccessor.get().getNode().getEntityData().getUniqueName(),
                    callParameters);
        } else {
            implFunCall = AstUtils.newNormalCall(funData.getDefaultImplementationUniqueName().get(),
                    AstUtils.newIdentifiersList(funData.getParametersNames()));
        }

        // Generate the statement

        final Statement stmt = funData.returnsVoid()
                ? new ExpressionStmt(Location.getDummyLocation(), implFunCall)
                : new ReturnStmt(Location.getDummyLocation(), Optional.<Expression>of(implFunCall));

        /* Add the only statement to the body and 'static' and 'inline'
           specifiers for optimization. */

        final FunctionDecl intermediateFun = funData.getIntermediateFunction();
        final CompoundStmt body = (CompoundStmt) intermediateFun.getBody();
        body.getStatements().add(stmt);
        intermediateFun.getModifiers().addAll(0, AstUtils.newRidsList(RID.STATIC, RID.INLINE));

        // Update intermediate data

        intermediateFun.getIntermediateData().get().setIsImplemented(true);

        return intermediateFun;
    }

    private FunctionDecl generateFunctionNonTrivial(IntermediateFunctionData funData,
                ListMultimap<Optional<ImmutableList<BigInteger>>, IndexedNode> impls) {

        final Optional<String> combiningFunName = combiningFunctionResolver.resolve(
                funData.getIntermediateFunction());
        final CompoundStmt body = (CompoundStmt) funData.getIntermediateFunction().getBody();

        /* Generate and declare the result variable if the function returns
           a value. */

        final Optional<String> resultUniqueName = !funData.returnsVoid()
                ? Optional.of(declareResultVariable(funData.getIntermediateFunction()))
                : Optional.<String>absent();

        // Generate calls to implementations

        generateImplementationCalls(funData, impls, body.getStatements(), resultUniqueName,
                combiningFunName);

        // Add the return statement if necessary

        if (!funData.returnsVoid()) {
            body.getStatements().add(AstUtils.newReturnStmt(resultUniqueName.get()));
        }

        // Update intermediate data

        funData.getIntermediateFunction().getIntermediateData().get().setIsImplemented(true);

        return funData.getIntermediateFunction();
    }

    private FunctionDecl generateFunctionNotImplemented(IntermediateFunctionData funData) {
        funData.getIntermediateFunction().getIntermediateData().get().setIsImplemented(false);
        return funData.getIntermediateFunction();
    }

    private void generateImplementationCalls(IntermediateFunctionData funData,
                ListMultimap<Optional<ImmutableList<BigInteger>>, IndexedNode> impls,
                LinkedList<Statement> stmts, Optional<String> resultUniqueName,
                Optional<String> combiningFunName) {
        boolean resultInitialized = false;

        // Generate unconditional calls

        final int unconditionalCallsCount = impls.get(KEY_UNCONDITIONAL).size();

        for (IndexedNode callee : impls.get(KEY_UNCONDITIONAL)) {
            stmts.add(generateImplementationCall(callee, funData.getParametersNames(),
                    resultUniqueName, combiningFunName, resultInitialized));
            resultInitialized = true;
        }

        // Generate remaining calls

        final boolean defaultImplCall = funData.getDefaultImplementationUniqueName().isPresent()
                && unconditionalCallsCount == 0;

        if (impls.size() - unconditionalCallsCount > 0) {
            final Statement condCalls;

            if (funData.getInstanceParametersNames().size() == 1) {
                condCalls = generateSwitchConditionalCalls(funData, impls, resultUniqueName,
                        combiningFunName, resultInitialized, defaultImplCall);
            } else {
                condCalls = generateIfElseConditionalCalls(funData, impls, resultUniqueName,
                        combiningFunName, resultInitialized, defaultImplCall);
            }

            stmts.add(condCalls);
        } else if (defaultImplCall) {
            stmts.add(generateDefaultImplementationCall(funData.getDefaultImplementationUniqueName().get(),
                    funData.getParametersNames(), resultUniqueName));
        }
    }

    private SwitchStmt generateSwitchConditionalCalls(
                IntermediateFunctionData funData,
                ListMultimap<Optional<ImmutableList<BigInteger>>, IndexedNode> impls,
                Optional<String> resultUniqueName,
                Optional<String> combiningFunName,
                boolean resultInitialized,
                boolean defaultImplCall
    ) {

        final CompoundStmt switchBody = AstUtils.newEmptyCompoundStmt();
        final SwitchStmt switchStmt = new SwitchStmt(Location.getDummyLocation(),
                AstUtils.newIdentifier(funData.getInstanceParametersNames().get(0)),
                switchBody);
        final List<String> normalParams = funData.getParametersNames().subList(1,
                funData.getParametersNames().size());

        for (Optional<ImmutableList<BigInteger>> key : impls.keySet()) {
            if (!key.isPresent()) {
                continue;
            }

            // Generate and add 'case'
            final CaseLabel caseLabel = new CaseLabel(Location.getDummyLocation(),
                    AstUtils.newIntegerConstant(key.get().get(0)), Optional.<Expression>absent());
            switchBody.getStatements().add(new LabeledStmt(Location.getDummyLocation(),
                    caseLabel, Optional.<Statement>absent()));

            // Generate and add calls

            boolean initializedCopy = resultInitialized;

            for (IndexedNode impl : impls.get(key)) {
                switchBody.getStatements().add(generateImplementationCall(impl, normalParams,
                        resultUniqueName, combiningFunName, initializedCopy));
                initializedCopy = true;
            }

            // Generate and add 'break'
            switchBody.getStatements().add(new BreakStmt(Location.getDummyLocation()));
        }

        // Generate the default implementation call
        if (defaultImplCall) {
            final DefaultLabel defaultLabel = new DefaultLabel(Location.getDummyLocation());
            switchBody.getStatements().add(new LabeledStmt(Location.getDummyLocation(),
                    defaultLabel, Optional.<Statement>absent()));
            switchBody.getStatements().add(generateDefaultImplementationCall(
                    funData.getDefaultImplementationUniqueName().get(), funData.getParametersNames(),
                    resultUniqueName));
            switchBody.getStatements().add(new BreakStmt(Location.getDummyLocation()));
        }

        return switchStmt;
    }

    private IfStmt generateIfElseConditionalCalls(
                IntermediateFunctionData funData,
                ListMultimap<Optional<ImmutableList<BigInteger>>, IndexedNode> impls,
                Optional<String> resultUniqueName,
                Optional<String> combiningFunName,
                boolean resultInitialized,
                boolean defaultImplCall
    ) {
        Optional<IfStmt> rootIf = Optional.absent();
        Optional<IfStmt> lastIf = Optional.absent();

        final List<String> normalParams = funData.getParametersNames().subList(
                funData.getInstanceParametersNames().size(), funData.getParametersNames().size());

        for (Optional<ImmutableList<BigInteger>> key : impls.keySet()) {
            if (!key.isPresent()) {
                continue;
            }

            // Generate the condition
            final Expression condition = AstUtils.newLogicalAnd(AstUtils.zipWithEq(
                    AstUtils.newIdentifiersList(funData.getInstanceParametersNames()),
                    AstUtils.newIntegerConstantsList(key.get())));

            // Generate and add calls
            final CompoundStmt ifBody = AstUtils.newEmptyCompoundStmt();
            final IfStmt ifStmt = new IfStmt(Location.getDummyLocation(), condition, ifBody,
                    Optional.<Statement>absent());

            boolean initializedCopy = resultInitialized;

            for (IndexedNode impl : impls.get(key)) {
                ifBody.getStatements().add(generateImplementationCall(impl, normalParams,
                        resultUniqueName, combiningFunName, initializedCopy));
                initializedCopy = true;
            }

            // Update variables
            if (lastIf.isPresent()) {
                lastIf.get().setFalseStatement(Optional.<Statement>of(ifStmt));
                lastIf = Optional.of(ifStmt);
            } else {
                rootIf = Optional.of(ifStmt);
                lastIf = Optional.of(ifStmt);
            }
        }

        // Generate the default implementation call

        if (defaultImplCall) {
            lastIf.get().setFalseStatement(Optional.<Statement>of(
                    generateDefaultImplementationCall(funData.getDefaultImplementationUniqueName().get(),
                            funData.getParametersNames(), resultUniqueName)
            ));
        }

        return rootIf.get();
    }

    private ExpressionStmt generateDefaultImplementationCall(String defaultFunUniqueName,
                List<String> allParams, Optional<String> resultUniqueName) {
        // Generate the call

        final FunctionCall call = AstUtils.newNormalCall(defaultFunUniqueName,
                AstUtils.newIdentifiersList(allParams));

        // Generate the assignment if necessary

        final Expression finalExpr = resultUniqueName.isPresent()
                ? new Assign(Location.getDummyLocation(), AstUtils.newIdentifier(resultUniqueName.get()), call)
                : call;

        return new ExpressionStmt(Location.getDummyLocation(), finalExpr);
    }

    private ExpressionStmt generateImplementationCall(IndexedNode impl, List<String> params,
                Optional<String> resultUniqueName, Optional<String> combiningFunName,
                boolean resultInitialized) {

        // Generate parameters list

        final LinkedList<Expression> callParameters = impl.getIndices().isPresent()
                ? AstUtils.newIntegerConstantsList(impl.getIndices().get())
                : new LinkedList<Expression>();
        callParameters.addAll(AstUtils.newIdentifiersList(params));

        // Generate the call

        final FunctionCall implCall = AstUtils.newNormalCall(impl.getNode().getEntityData().getUniqueName(),
                callParameters);

        // Generate the call to combining function if it is necessary

        final FunctionCall withCombiningFun;

        if (resultInitialized && resultUniqueName.isPresent() && combiningFunName.isPresent()) {
            withCombiningFun = AstUtils.newNormalCall(combiningFunName.get(), implCall,
                    AstUtils.newIdentifier(resultUniqueName.get()));
        } else {
            withCombiningFun = implCall;
        }

        // Generate assignment to the result variable

        final Expression finalExpr = resultUniqueName.isPresent()
                ? new Assign(Location.getDummyLocation(), AstUtils.newIdentifier(resultUniqueName.get()), withCombiningFun)
                : withCombiningFun;

        return new ExpressionStmt(Location.getDummyLocation(), finalExpr);
    }

    private String declareResultVariable(FunctionDecl funDecl) {
        final String resultUniqueName = nameMangler.mangle("result");
        final CompoundStmt body = (CompoundStmt) funDecl.getBody();

        body.getDeclarations().add(AstUtils.newSimpleDeclaration(resultUniqueName,
                resultUniqueName, false, Optional.<Expression>absent(),
                AstUtils.extractReturnType(funDecl)));

        return resultUniqueName;
    }
}
