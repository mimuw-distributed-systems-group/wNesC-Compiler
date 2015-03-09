package pl.edu.mimuw.nesc.optimization;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.IntegerCstKind;
import pl.edu.mimuw.nesc.ast.IntegerCstSuffix;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.common.SchedulerSpecification;
import pl.edu.mimuw.nesc.refsgraph.EntityNode;
import pl.edu.mimuw.nesc.refsgraph.Reference;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;
import pl.edu.mimuw.nesc.wiresgraph.IndexedNode;
import pl.edu.mimuw.nesc.wiresgraph.IntermediateFunctionData;
import pl.edu.mimuw.nesc.wiresgraph.SpecificationElementNode;
import pl.edu.mimuw.nesc.wiresgraph.WiresGraph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * <p>Optimizer that removes tasks that are never posted.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TaskOptimizer {
    /**
     * List with declarations to optimize.
     */
    private final ImmutableList<Declaration> declarations;

    /**
     * Graph of wires necessary to look at the scheduler component.
     */
    private final WiresGraph wiresGraph;

    /**
     * Graph of references to know which tasks are never posted.
     */
    private final ReferencesGraph refsGraph;

    /**
     * Specification of the scheduler.
     */
    private final SchedulerSpecification schedulerSpec;

    /**
     * Value indicating if the optimization has already taken place.
     */
    private Optional<ImmutableList<Declaration>> cleanedDeclarations;

    public TaskOptimizer(ImmutableList<Declaration> declarations, WiresGraph wiresGraph,
            ReferencesGraph refsGraph, SchedulerSpecification schedulerSpec) {
        checkNotNull(declarations, "declarations cannot be null");
        checkNotNull(wiresGraph, "wires graph cannot be null");
        checkNotNull(refsGraph, "references graph cannot be null");
        checkNotNull(schedulerSpec, "scheduler specification cannot be null");
        this.declarations = declarations;
        this.wiresGraph = wiresGraph;
        this.refsGraph = refsGraph;
        this.schedulerSpec = schedulerSpec;
        this.cleanedDeclarations = Optional.absent();
    }

    /**
     * Performs the optimization.
     *
     * @return List of declarations after the optimization.
     * @throws UnexpectedWiringException The optimization cannot be completed
     *                                   due to unexpected wiring. If this
     *                                   exception is thrown, the declarations
     *                                   are unmodified.
     */
    public ImmutableList<Declaration> optimize() throws UnexpectedWiringException {
        if (cleanedDeclarations.isPresent()) {
            return cleanedDeclarations.get();
        }

        checkTasksConnections();
        final SpecificationElementNode postSink = findPostSink();
        final ImmutableSet<BigInteger> usedTasksIds = determinePostedTasks(postSink);
        final ImmutableSet<String> tasksForRemoval = determineTasksForRemoval(usedTasksIds);

        if (!tasksForRemoval.isEmpty()) {
            final ImmutableMap<Long, Long> identifiersMap = updateTasksIdentifiers(postSink);
            updateRunTaskEvent(tasksForRemoval, identifiersMap, postSink);
            this.cleanedDeclarations = Optional.of(removeUnusedTasks(tasksForRemoval));
            updateUniqueCount(tasksForRemoval.size(), this.cleanedDeclarations.get());
        } else {
            this.cleanedDeclarations = Optional.of(declarations);
        }

        return this.cleanedDeclarations.get();
    }

    /**
     * Checks if there are no parameterised interfaces connected directly to
     * the scheduler task interface.
     *
     * @throws UnexpectedWiringException There are parameterised interfaces
     *                                   connected directly to the scheduler
     *                                   task interface.
     */
    private void checkTasksConnections() throws UnexpectedWiringException {
        final String runEventName = format("%s.%s.%s", schedulerSpec.getComponentName(),
                schedulerSpec.getInterfaceNameInScheduler(), schedulerSpec.getTaskRunEventName());
        final SpecificationElementNode node = wiresGraph.requireNode(runEventName);

        // Iterate over tasks and check

        for (Map.Entry<Optional<ImmutableList<BigInteger>>, IndexedNode> successor : node.getSuccessors().entries()) {
            if (!successor.getKey().isPresent()) {
                final SpecificationElementNode successorNode = successor.getValue().getNode();
                throw new UnexpectedWiringException(format("parameterised interface '%s.%s' is directly connected to the task interface of scheduler '%s.%s'",
                        successorNode.getComponentName(), successorNode.getInterfaceRefName().get(),
                        schedulerSpec.getComponentName(), schedulerSpec.getInterfaceNameInScheduler()));
            }
        }
    }

    /**
     * Finds the implementation of the command of the scheduler that is
     * responsible for posting tasks.
     *
     * @return Specification element that represents the implementation of
     *         posting a task.
     */
    private SpecificationElementNode findPostSink() throws UnexpectedWiringException {
        final String sourceName = format("%s.%s.%s", schedulerSpec.getComponentName(),
                schedulerSpec.getInterfaceNameInScheduler(),
                schedulerSpec.getTaskPostCommandName());
        if (!wiresGraph.getNodes().containsKey(sourceName)) {
            throw new IllegalStateException("the wires graph does not contain scheduler node '"
                    + sourceName + "'");
        }

        final Optional<ImmutableList<BigInteger>> expectedKey = Optional.absent();
        SpecificationElementNode currentNode = wiresGraph.requireNode(sourceName);

        // Find the implementation

        while (!currentNode.getEntityData().isImplemented()) {
            final ListMultimap<Optional<ImmutableList<BigInteger>>, IndexedNode> successors =
                    currentNode.getSuccessors();

            if (successors.size() != 1) {
                throw new UnexpectedWiringException(format("specification element '%s' of scheduler component '%s' is connected more than once",
                        schedulerSpec.getInterfaceNameInScheduler(), schedulerSpec.getComponentName()));
            } else if (successors.get(expectedKey).size() != 1) {
                throw new UnexpectedWiringException(format("a single element of the scheduler task interface '%s.%s' is connected",
                        schedulerSpec.getComponentName(), schedulerSpec.getInterfaceNameInScheduler()));
            }

            final IndexedNode successor = successors.get(expectedKey).get(0);

            if (successor.getIndices().isPresent()) {
                throw new RuntimeException("expecting a connection without indices");
            }

            currentNode = successor.getNode();
        }

        return currentNode;
    }

    /**
     * Determine the identifiers of tasks that are posted.
     *
     * @param postSink Implementation of posting a task in the scheduler.
     * @return Set with identifiers of tasks that are posted.
     */
    private ImmutableSet<BigInteger> determinePostedTasks(SpecificationElementNode postSink) {
        final ImmutableSet.Builder<BigInteger> postedIdentifiersBuilder = ImmutableSet.builder();
        final String funUniqueName = postSink.getEntityData().getUniqueName();

        if (!refsGraph.getOrdinaryIds().containsKey(funUniqueName)) {
            // No tasks are posted and the function has been removed
            return ImmutableSet.of();
        }

        final EntityNode node = refsGraph.getOrdinaryIds().get(funUniqueName);

        // Collect identifiers of tasks that are posted

        for (Reference predecessor : node.getPredecessors()) {
            final FunctionCall call = (FunctionCall) predecessor.getASTNode();
            final IntegerCst cst = (IntegerCst) call.getArguments().getFirst();
            postedIdentifiersBuilder.add(cst.getValue().get());
        }

        return postedIdentifiersBuilder.build();
    }

    private ImmutableSet<String> determineTasksForRemoval(ImmutableSet<BigInteger> usedTasksIds) {
        final ImmutableSet.Builder<String> forRemovalBuilder = ImmutableSet.builder();

        // Get the node that represents running a task in the scheduler

        final String runTaskEventName = format("%s.%s.%s", schedulerSpec.getComponentName(),
                schedulerSpec.getInterfaceNameInScheduler(), schedulerSpec.getTaskRunEventName());
        checkState(wiresGraph.getNodes().containsKey(runTaskEventName),
                "node that represents running a task in the scheduler is absent");
        final SpecificationElementNode runTaskNode = wiresGraph.requireNode(runTaskEventName);

        // Determine unique names of functions for removal

        for (Map.Entry<Optional<ImmutableList<BigInteger>>, IndexedNode> successor : runTaskNode.getSuccessors().entries()) {
            if (!usedTasksIds.contains(successor.getKey().get().get(0))) {
                forRemovalBuilder.add(successor.getValue().getNode().getEntityData().getUniqueName());
            }
        }

        return forRemovalBuilder.build();
    }

    private ImmutableMap<Long, Long> updateTasksIdentifiers(SpecificationElementNode postSink) {
        final String postImplUniqueName = postSink.getEntityData().getUniqueName();
        final Map<Long, Long> identifiersMap = new HashMap<>();
        long nextId = 0;

        // Iterate over predecessors and assign new identifier

        for (Reference predecessor : refsGraph.getOrdinaryIds().get(postImplUniqueName).getPredecessors()) {
            final FunctionCall funCall = (FunctionCall) predecessor.getASTNode();
            final IntegerCst oldIdCst = (IntegerCst) funCall.getArguments().getFirst();
            final long oldId = oldIdCst.getValue().get().longValue();
            final long newId;

            if (identifiersMap.containsKey(oldId)) {
                newId = identifiersMap.get(oldId);
            } else {
                newId = nextId;
                ++nextId;
                identifiersMap.put(oldId, newId);
            }

            oldIdCst.setValue(Optional.of(BigInteger.valueOf(newId)));
            oldIdCst.setString(Long.toString(newId));
            oldIdCst.setSuffix(IntegerCstSuffix.NO_SUFFIX);
            oldIdCst.setKind(IntegerCstKind.DECIMAL);
        }

        return ImmutableMap.copyOf(identifiersMap);
    }

    private void updateRunTaskEvent(ImmutableSet<String> tasksForRemoval, ImmutableMap<Long, Long> identifiersMap,
                SpecificationElementNode postSink) {
        // Get the intermediate function for running a task in the scheduler

        final String runTaskSourceName = format("%s.%s.%s", postSink.getComponentName(),
                postSink.getInterfaceRefName().get(), schedulerSpec.getTaskRunEventName());
        final SpecificationElementNode runTaskSource = wiresGraph.requireNode(runTaskSourceName);
        checkState(!runTaskSource.getEntityData().isImplemented(), "source of running tasks is implemented");
        final IntermediateFunctionData funData = (IntermediateFunctionData) runTaskSource.getEntityData();

        // Get connections with functions that perform tasks

        final String runTaskEventName = format("%s.%s.%s", schedulerSpec.getComponentName(),
                schedulerSpec.getInterfaceNameInScheduler(), schedulerSpec.getTaskRunEventName());
        final SpecificationElementNode runTaskEvent = wiresGraph.requireNode(runTaskEventName);

        // Generate new body - body of the switch

        final String paramUniqueName = funData.getInstanceParametersNames().get(0);
        final CompoundStmt switchBody = AstUtils.newEmptyCompoundStmt();

        for (Map.Entry<Optional<ImmutableList<BigInteger>>, IndexedNode> task : runTaskEvent.getSuccessors().entries()) {
            if (!tasksForRemoval.contains(task.getValue().getNode().getEntityData().getUniqueName())) {
                final long newId = identifiersMap.get(task.getKey().get().get(0).longValue());
                final CaseLabel caseLabel = new CaseLabel(Location.getDummyLocation(),
                        AstUtils.newIntegerConstant(BigInteger.valueOf(newId)),
                        Optional.<Expression>absent());
                switchBody.getStatements().add(new LabeledStmt(Location.getDummyLocation(),
                        caseLabel, Optional.<Statement>absent()));
                switchBody.getStatements().add(new ExpressionStmt(Location.getDummyLocation(),
                        AstUtils.newNormalCall(task.getValue().getNode().getEntityData().getUniqueName())));
                switchBody.getStatements().add(new BreakStmt(Location.getDummyLocation()));
            }
        }

        if (funData.getDefaultImplementationUniqueName().isPresent()) {
            final DefaultLabel defaultLabel = new DefaultLabel(Location.getDummyLocation());
            switchBody.getStatements().add(new LabeledStmt(Location.getDummyLocation(),
                    defaultLabel, Optional.<Statement>absent()));
            switchBody.getStatements().add(new ExpressionStmt(Location.getDummyLocation(),
                    AstUtils.newNormalCall(funData.getDefaultImplementationUniqueName().get(),
                            AstUtils.newIdentifier(paramUniqueName))));
            switchBody.getStatements().add(new BreakStmt(Location.getDummyLocation()));
        }

        // Generate new body - switch statement

        final SwitchStmt switchStmt = new SwitchStmt(Location.getDummyLocation(),
                AstUtils.newIdentifier(paramUniqueName), switchBody);

        // Generate new body - the outermost compound statement

        final CompoundStmt funBody = AstUtils.newEmptyCompoundStmt();
        funBody.getStatements().add(switchStmt);

        // Replace the body

        funData.getIntermediateFunction().setBody(funBody);
    }

    private ImmutableList<Declaration> removeUnusedTasks(ImmutableSet<String> tasksForRemoval) {
        final ImmutableList.Builder<Declaration> declarationsBuilder = ImmutableList.builder();
        final CleaningVisitor cleaningVisitor = new CleaningVisitor(tasksForRemoval);

        for (Declaration declaration : declarations) {
            if (declaration.accept(cleaningVisitor, null)) {
                declarationsBuilder.add(declaration);
            }
        }

        return declarationsBuilder.build();
    }

    private void updateUniqueCount(int decrease, ImmutableList<Declaration> declarations) {
        final UniqueCountUpdatingVisitor updatingVisitor = new UniqueCountUpdatingVisitor(decrease);

        for (Declaration declaration : declarations) {
            declaration.traverse(updatingVisitor, null);
        }
    }

    /**
     * Visitor that updates unique count of tasks in visited node.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class UniqueCountUpdatingVisitor extends IdentityVisitor<Void> {
        private final int decrease;

        private UniqueCountUpdatingVisitor(int decrease) {
            checkArgument(decrease > 0, "decrease must be positive");
            this.decrease = decrease;
        }

        @Override
        public Void visitUniqueCountCall(UniqueCountCall uniqueCountCall, Void arg) {
            if (schedulerSpec.getUniqueIdentifier().equals(uniqueCountCall.getIdentifier())) {
                uniqueCountCall.setValue(uniqueCountCall.getValue() - decrease);
                checkState(uniqueCountCall.getValue() >= 0, "count of tasks is negative");
            }
            return null;
        }
    }

    /**
     * Visitor that decides if a declaration shall be preserved.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class CleaningVisitor extends ExceptionVisitor<Boolean, Void> {
        private final ImmutableSet<String> tasksForRemoval;

        private CleaningVisitor(ImmutableSet<String> tasksForRemoval) {
            this.tasksForRemoval = tasksForRemoval;
        }

        @Override
        public Boolean visitFunctionDecl(FunctionDecl declaration, Void arg) {
            final String uniqueName = DeclaratorUtils.getUniqueName(
                    declaration.getDeclarator()).get();
            final boolean preserve = !tasksForRemoval.contains(uniqueName);

            if (!preserve) {
                refsGraph.removeOrdinaryId(uniqueName);
            }

            return preserve;
        }

        @Override
        public Boolean visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            return declaration.getDeclaration().accept(this, null);
        }

        @Override
        public Boolean visitDataDecl(DataDecl declaration, Void arg) {
            if (declaration.getDeclarations().isEmpty()) {
                return true;
            }

            final Iterator<Declaration> innerDeclIt = declaration.getDeclarations().iterator();

            while (innerDeclIt.hasNext()) {
                if (!innerDeclIt.next().accept(this, null)) {
                    innerDeclIt.remove();
                }
            }

            return !declaration.getDeclarations().isEmpty();
        }

        @Override
        public Boolean visitVariableDecl(VariableDecl declaration, Void arg) {
            final String uniqueName = DeclaratorUtils.getUniqueName(
                    declaration.getDeclarator().get()).get();
            final boolean preserve = !tasksForRemoval.contains(uniqueName);

            if (!preserve) {
                refsGraph.removeOrdinaryId(uniqueName);
            }

            return preserve;
        }
    }
}
