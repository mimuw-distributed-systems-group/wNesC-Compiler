package pl.edu.mimuw.nesc.codepartition;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.codepartition.context.PartitionContext;
import pl.edu.mimuw.nesc.codesize.CodeSizeEstimation;
import pl.edu.mimuw.nesc.common.AtomicSpecification;
import pl.edu.mimuw.nesc.common.util.IntegerSumIntervalTreeOperation;
import pl.edu.mimuw.nesc.common.util.IntervalTree;
import pl.edu.mimuw.nesc.compilation.CompilationListener;
import pl.edu.mimuw.nesc.problem.NescWarning;
import pl.edu.mimuw.nesc.problem.issue.Issue;
import pl.edu.mimuw.nesc.refsgraph.EntityNode;
import pl.edu.mimuw.nesc.refsgraph.Reference;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Code partitioner that takes into consideration the biconnected components
 * of the call graph during the partition operation.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class BComponentsCodePartitioner implements CodePartitioner {
    /**
     * Bank schema assumed by this partitioner.
     */
    private final BankSchema bankSchema;

    /**
     * Common bank allocator used by this partitioner.
     */
    private final CommonBankAllocator commonBankAllocator;

    /**
     * Comparator of tree allocations used by this partitioner.
     */
    private final Comparator<TreeAllocation> treeAllocationsComparator;

    /**
     * Listener that will be notified about detected issues.
     */
    private final CompilationListener listener;

    public BComponentsCodePartitioner(BankSchema bankSchema, AtomicSpecification atomicSpec,
            CompilationListener listener) {
        checkNotNull(bankSchema, "bank schema cannot be null");
        checkNotNull(atomicSpec, "atomic specification cannot be null");
        checkNotNull(listener, "listener cannot be null");
        this.bankSchema = bankSchema;
        this.commonBankAllocator = new CommonBankAllocator(atomicSpec);
        this.treeAllocationsComparator = new TreeAllocationComparator(bankSchema.getCommonBankName());
        this.listener = listener;
    }

    @Override
    public BankSchema getBankSchema() {
        return bankSchema;
    }

    @Override
    public BankTable partition(Iterable<FunctionDecl> functions, CodeSizeEstimation estimation,
            ReferencesGraph refsGraph) throws PartitionImpossibleException {
        checkNotNull(functions, "functions cannot be null");
        checkNotNull(estimation, "estimation cannot be null");
        checkNotNull(refsGraph, "references graph cannot be null");

        final BComponentsPartitionContext context = new BComponentsPartitionContext(
                functions, estimation.getFunctionsSizes(), refsGraph);
        commonBankAllocator.allocate(context, functions);
        final Queue<FunctionVertex> topologicalOrdering = computeTopologicalOrdering(context);
        computeFrequencyEstimations(topologicalOrdering);
        fillCommonBank(context);
        computeBiconnectedComponents(context);
        updateFunctionsSizesTree(context);
        assignFunctions(context);

        return context.getBankTable();
    }

    private void fillCommonBank(BComponentsPartitionContext context) {
        final List<FunctionVertex> allVertices = new ArrayList<>(
                context.callGraph.getVertices().values());
        final String commonBankName = bankSchema.getCommonBankName();
        Collections.sort(allVertices, new FunctionFrequencyComparator());

        for (FunctionVertex vertex : allVertices) {
            if (context.getBankTable().getFreeSpace(commonBankName) >= context.getFunctionSize(vertex.getUniqueName())
                    && !vertex.getTargetBank().isPresent()) {
                context.assign(context.functions.get(vertex.getUniqueName()), commonBankName);
            }
        }
    }

    private Queue<FunctionVertex> computeTopologicalOrdering(BComponentsPartitionContext context) {
        final TopologicalSortDfsVertexVisitor topologicalDfsVisitor = new TopologicalSortDfsVertexVisitor();
        final DepthFirstSearcher depthFirstSearcher = new DepthFirstSearcher(new SuccessorsProvider());

        for (FunctionVertex vertex : context.callGraph.getVertices().values()) {
            depthFirstSearcher.search(vertex, topologicalDfsVisitor);
        }

        return topologicalDfsVisitor.getTopologicalOrdering();
    }

    private void computeFrequencyEstimations(Queue<FunctionVertex> topologicalOrdering) {
        final Set<FunctionVertex> completedVertices = new HashSet<>();
        boolean warningEmitted = false;

        while (!topologicalOrdering.isEmpty()) {
            final FunctionVertex currentVertex = topologicalOrdering.remove();
            completedVertices.add(currentVertex);

            if (currentVertex.getPredecessors().isEmpty()) {
                currentVertex.increaseFrequencyEstimation(1.);
            }

            for (CallEdge edge : currentVertex.getSuccessors()) {
                if (edge.getTargetVertex() != currentVertex
                        && !completedVertices.contains(edge.getTargetVertex())) {
                    edge.increaseFrequencyEstimation(currentVertex.getFrequencyEstimation()
                            * Math.pow(2., edge.getEnclosingLoopsCount())
                            * Math.pow(0.5, edge.getEnclosingConditionalStmtsCount()));
                    edge.getTargetVertex().increaseFrequencyEstimation(edge.getFrequencyEstimation());
                } else if (edge.getTargetVertex() != currentVertex && !warningEmitted) {
                    listener.warning(new NescWarning(Optional.<Location>absent(),
                            Optional.<Location>absent(), Optional.<Issue.Code>absent(),
                            "a call loop involving function '" + currentVertex.getUniqueName()
                            + "' is present which decreases the accuracy of the biconnected components heuristic"));
                    warningEmitted = true;
                }
            }
        }
    }

    private void computeBiconnectedComponents(BComponentsPartitionContext context) {
        final BComponentsDfsVertexVisitor bcomponentsDfsVisitor = new BComponentsDfsVertexVisitor();
        final DepthFirstSearcher depthFirstSearcher = new DepthFirstSearcher(
                new UnassignedNeighboursProvider());

        for (FunctionVertex vertex : context.callGraph.getVertices().values()) {
            if (!vertex.hasDfsTreeNumber() && !vertex.getTargetBank().isPresent()) {
                depthFirstSearcher.search(vertex, bcomponentsDfsVisitor);
            }
        }
    }

    private void updateFunctionsSizesTree(BComponentsPartitionContext context) {
        for (FunctionVertex vertex : context.callGraph.getVertices().values()) {
            if (!vertex.getTargetBank().isPresent()) {
                context.functionsSizesTree.set(vertex.getDfsTreeNumber(),
                        context.getFunctionSize(vertex.getUniqueName()));
            }
        }
    }

    private void assignFunctions(BComponentsPartitionContext context) throws PartitionImpossibleException {
        int functionsLeft = computeUnallocatedFunctionsCount(context);
        final Map<FunctionVertex, Set<FunctionVertex>> allocationVertices =
                findAllocationVertices(context);

        while (functionsLeft != 0) {
            final Optional<TreeAllocation> allocation = determineAllocation(
                    context, allocationVertices);
            if (allocation.isPresent()) {
                functionsLeft -= performAllocation(context, allocation.get(),
                        allocationVertices.get(allocation.get().dfsTreeRoot));
                if (allocation.get().direction == AllocationDirection.UP_TREE) {
                    allocationVertices.put(allocation.get().vertex, allocationVertices.get(
                            allocation.get().dfsTreeRoot));
                    allocationVertices.remove(allocation.get().dfsTreeRoot);
                }
                if (allocation.get().direction == AllocationDirection.DOWN_TREE
                        && allocation.get().vertex == allocation.get().dfsTreeRoot) {
                    allocationVertices.remove(allocation.get().dfsTreeRoot);
                }
            } else {
                final FunctionVertex treeToAllocate = Collections.max(allocationVertices.keySet(),
                        new FunctionSizeComparator(context.functionsSizesTree));
                functionsLeft -= allocateTree(context, treeToAllocate);
                allocationVertices.remove(treeToAllocate);
            }
        }
    }

    private int computeUnallocatedFunctionsCount(BComponentsPartitionContext context) {
        int unallocatedFunctionsCount = 0;
        for (FunctionVertex vertex : context.callGraph.getVertices().values()) {
            if (!vertex.getTargetBank().isPresent()) {
                ++unallocatedFunctionsCount;
            }
        }
        return unallocatedFunctionsCount;
    }

    private int performAllocation(BComponentsPartitionContext context, TreeAllocation allocation,
                Set<FunctionVertex> cutVertices) {
        final FunctionVertex startVertex;
        switch (allocation.direction) {
            case UP_TREE:
                startVertex = allocation.dfsTreeRoot;
                break;
            case DOWN_TREE:
                startVertex = allocation.vertex;
                break;
            default:
                throw new RuntimeException("unexpected allocation direction '"
                        + allocation.direction + "'");
        }

        final Queue<FunctionVertex> queue = new ArrayDeque<>();
        final Set<FunctionVertex> visitedVertices = new HashSet<>();
        int allocatedFunctionsCount = 0;

        queue.add(startVertex);
        visitedVertices.addAll(queue);

        while (!queue.isEmpty()) {
            final FunctionVertex vertex = queue.remove();

            for (FunctionVertex dfsTreeChild : vertex.getDfsTreeChildren()) {
                if (!dfsTreeChild.getTargetBank().isPresent()) {
                    if (!visitedVertices.contains(dfsTreeChild)
                            && (allocation.direction != AllocationDirection.UP_TREE || dfsTreeChild != allocation.vertex)) {
                        visitedVertices.add(dfsTreeChild);
                        queue.add(dfsTreeChild);
                    }
                }
            }

            context.assign(context.functions.get(vertex.getUniqueName()), allocation.bankName);
            ++allocatedFunctionsCount;
            cutVertices.remove(vertex);
        }

        return allocatedFunctionsCount;
    }

    private Map<FunctionVertex, Set<FunctionVertex>> findAllocationVertices(BComponentsPartitionContext context) {
        final Map<FunctionVertex, Set<FunctionVertex>> dfsTreesRoots = new HashMap<>();
        for (FunctionVertex vertex : context.callGraph.getVertices().values()) {
            if (!vertex.getDfsTreeParent().isPresent()
                    && !vertex.getTargetBank().isPresent()) {
                dfsTreesRoots.put(vertex, findCutVertices(vertex));
            }
        }
        return dfsTreesRoots;
    }

    private Set<FunctionVertex> findCutVertices(FunctionVertex vertex) {
        final Set<FunctionVertex> cutVertices = new HashSet<>();
        final Queue<FunctionVertex> queue = new ArrayDeque<>();
        final Set<FunctionVertex> visitedVertices = new HashSet<>();

        queue.add(vertex);
        visitedVertices.addAll(queue);

        while (!queue.isEmpty()) {
            final FunctionVertex descendant = queue.remove();
            if (descendant.getBiconnectedComponents().size() > 1) {
                cutVertices.add(descendant);
            }
            for (FunctionVertex child : descendant.getDfsTreeChildren()) {
                if (!visitedVertices.contains(child)) {
                    visitedVertices.add(child);
                    queue.add(child);
                }
            }
        }

        return cutVertices;
    }

    private Optional<TreeAllocation> determineAllocation(BComponentsPartitionContext context,
                Map<FunctionVertex, Set<FunctionVertex>> allocationVertices) {

        Optional<TreeAllocation> bestAllocation = Optional.absent();
        final List<TreeAllocation> candidateAllocations = new ArrayList<>();

        // Check all allocations in cut vertices
        for (Map.Entry<FunctionVertex, Set<FunctionVertex>> dfsTreeEntry : allocationVertices.entrySet()) {
            final FunctionVertex treeRoot = dfsTreeEntry.getKey();
            final int allFunctionsSize = context.functionsSizesTree.compute(treeRoot.getDfsTreeNumber(),
                    treeRoot.getMaximumDfsDescendantNumber() + 1);
            candidateAllocations.clear();

            if (bestAllocation.isPresent()) {
                candidateAllocations.add(bestAllocation.get());
            }
            addRootAllocation(candidateAllocations, context, treeRoot, allFunctionsSize);

            for (FunctionVertex cutVertex : dfsTreeEntry.getValue()) {
                if (cutVertex == treeRoot) {
                    continue;
                }

                addTreeAllocation(candidateAllocations, context, cutVertex,
                        AllocationDirection.DOWN_TREE, allFunctionsSize, treeRoot);
                addTreeAllocation(candidateAllocations, context, cutVertex,
                        AllocationDirection.UP_TREE, allFunctionsSize, treeRoot);
            }

            if (!candidateAllocations.isEmpty()) {
                bestAllocation = Optional.of(Collections.max(candidateAllocations, treeAllocationsComparator));
            }
        }

        return bestAllocation;
    }

    private void addRootAllocation(Collection<TreeAllocation> candidateAllocations,
                BComponentsPartitionContext context, FunctionVertex treeRoot,
                int allFunctionsSize) {
        final Optional<String> targetBank = context.getFloorBank(allFunctionsSize);
        if (targetBank.isPresent()) {
            candidateAllocations.add(new TreeAllocation(treeRoot, treeRoot,
                    AllocationDirection.DOWN_TREE, targetBank.get(), allFunctionsSize));
        }
    }

    private void addTreeAllocation(Collection<TreeAllocation> allocations,
                BComponentsPartitionContext context, FunctionVertex cutVertex,
                AllocationDirection direction, int allFunctionsSize,
                FunctionVertex treeRoot) {
        final int downTreeFunctionsSize = context.functionsSizesTree.compute(cutVertex.getDfsTreeNumber(),
                cutVertex.getMaximumDfsDescendantNumber() + 1);
        final int functionsSize;

        switch (direction) {
            case DOWN_TREE:
                functionsSize = downTreeFunctionsSize;
                break;
            case UP_TREE:
                functionsSize = allFunctionsSize - downTreeFunctionsSize;
                break;
            default:
                throw new RuntimeException("unexpected allocation direction " + direction);
        }

        final Optional<String> targetBank = context.getFloorBank(functionsSize);
        if (targetBank.isPresent()) {
            allocations.add(new TreeAllocation(cutVertex, treeRoot, direction,
                    targetBank.get(), functionsSize));
        }
    }

    private int allocateTree(BComponentsPartitionContext context, FunctionVertex root)
                throws PartitionImpossibleException {
        final AllocatingDfsVertexVisitor allocatingVisitor = new AllocatingDfsVertexVisitor(context);
        new DepthFirstSearcher(new UnassignedDfsTreeChildrenProvider())
                .search(root, allocatingVisitor);
        if (allocatingVisitor.failure) {
            throw new PartitionImpossibleException("not enough space");
        }
        return allocatingVisitor.allocatedFunctionsCount;
    }

    /**
     * Context for a biconnected components code partitioner.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class BComponentsPartitionContext extends PartitionContext {
        /**
         * Call graph of functions that are partitioned in this context. It
         * should contain only functions that has not been assigned yet.
         */
        private final CallGraph callGraph;

        /**
         * Interval tree for fast computation of total size of functions from
         * a given subtree of the DFS tree.
         */
        private final IntervalTree<Integer> functionsSizesTree;

        /**
         * Map with functions that will be partitioned.
         */
        private final ImmutableMap<String, FunctionDecl> functions;

        private BComponentsPartitionContext(Iterable<FunctionDecl> functions,
                    Map<String, Range<Integer>> functionsSizes, ReferencesGraph refsGraph) {
            super(bankSchema, functionsSizes);
            this.callGraph = new CallGraph(functions, refsGraph);
            this.functionsSizesTree = new IntervalTree<>(Integer.class,
                    new IntegerSumIntervalTreeOperation(), functionsSizes.size());

            final ImmutableMap.Builder<String, FunctionDecl> functionsMapBuilder =
                    ImmutableMap.builder();
            for (FunctionDecl function : functions) {
                final String uniqueName = DeclaratorUtils.getUniqueName(
                        function.getDeclarator()).get();
                functionsMapBuilder.put(uniqueName, function);
            }
            this.functions = functionsMapBuilder.build();
        }

        @Override
        public void assign(FunctionDecl function, String bankName) {
            super.assign(function, bankName);

            final String funUniqueName = DeclaratorUtils.getUniqueName(
                    function.getDeclarator()).get();
            final FunctionVertex vertex = callGraph.getVertices().get(funUniqueName);
            if (vertex.hasDfsTreeNumber()) {
                functionsSizesTree.set(vertex.getDfsTreeNumber(), 0);
            }
            vertex.assigned(bankName);
        }
    }

    /**
     * The call graph whose characteristics determine the partition made by this
     * partitioner.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class CallGraph {
        /**
         * Vertices of the graph: unique names of functions mapped to objects
         * that represent them in the graph.
         */
        private final Map<String, FunctionVertex> vertices;

        /**
         * Unmodifiable view of the vertices map.
         */
        private final Map<String, FunctionVertex> unmodifiableVertices;

        private CallGraph(Iterable<FunctionDecl> functions, ReferencesGraph refsGraph) {
            final PrivateBuilder builder = new RealBuilder(functions, refsGraph);
            this.vertices = builder.buildVertices();
            this.unmodifiableVertices = Collections.unmodifiableMap(this.vertices);
        }

        /**
         * Get an unmodifiable view of the vertices of the graph.
         *
         * @return Unmodifiable map with vertices of the graph.
         */
        private Map<String, FunctionVertex> getVertices() {
            return unmodifiableVertices;
        }

        /**
         * Removes the vertex that represents function with given unique name
         * from the graph.
         *
         * @param funUniqueName Unique name of the function to remove from the
         *                      call graph.
         */
        private void removeVertex(String funUniqueName) {
            checkNotNull(funUniqueName, "unique name of the function cannot be null");
            checkArgument(!funUniqueName.isEmpty(), "unique name of the function cannot be an empty string");
            checkState(vertices.containsKey(funUniqueName),
                    "function with given unique name does not exist in this graph");

            this.vertices.get(funUniqueName).removeAllEdges();
            this.vertices.remove(funUniqueName);
        }

        /**
         * Interface for building particular elements of a call graph.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private interface PrivateBuilder {
            Map<String, FunctionVertex> buildVertices();
        }

        /**
         * Implementation of the private builder interface.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private static final class RealBuilder implements PrivateBuilder {
            private final Iterable<FunctionDecl> functions;
            private final ReferencesGraph refsGraph;
            private final NeighbourComparator neighbourComparator;

            private RealBuilder(Iterable<FunctionDecl> functions, ReferencesGraph refsGraph) {
                this.functions = functions;
                this.refsGraph = refsGraph;
                this.neighbourComparator = new NeighbourComparator(refsGraph);
            }

            @Override
            public Map<String, FunctionVertex> buildVertices() {
                final Map<String, FunctionVertex> vertices = new TreeMap<>();
                final PriorityQueue<String> functionsQueue = new PriorityQueue<>();

                // Create all vertices of the graph
                for (FunctionDecl function : functions) {
                    final String uniqueName = DeclaratorUtils.getUniqueName(
                            function.getDeclarator()).get();
                    if (vertices.containsKey(uniqueName)) {
                        throw new RuntimeException("function '" + uniqueName
                                + "' with more than one definition");
                    }
                    vertices.put(uniqueName, new FunctionVertex(uniqueName));
                    functionsQueue.add(uniqueName);
                }

                // Add all edges
                while (!functionsQueue.isEmpty()) {
                    final String funUniqueName = functionsQueue.remove();
                    final EntityNode entityNode = refsGraph.getOrdinaryIds().get(funUniqueName);
                    final FunctionVertex funVertex = vertices.get(funUniqueName);
                    final List<Reference> successors = new ArrayList<>(funVertex.getSuccessors().size()
                        + funVertex.getPredecessors().size());

                    for (Reference successorReference : entityNode.getSuccessors()) {
                        if (!successorReference.isInsideNotEvaluatedExpr()
                                && successorReference.getType() == Reference.Type.CALL
                                && vertices.containsKey(successorReference.getReferencedNode().getUniqueName())) {
                            successors.add(successorReference);
                        }
                    }
                    Collections.sort(successors, neighbourComparator);
                    for (Reference successorReference : successors) {
                        funVertex.addCall(vertices.get(successorReference.getReferencedNode().getUniqueName()),
                                successorReference.getEnclosingLoopsCount(),
                                successorReference.getEnclosingConditionalStmtsCount());
                    }
                }

                return vertices;
            }
        }
    }

    /**
     * Vertex in the call graph. It is a directed graph.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class FunctionVertex {
        /**
         * Unique name of the function represented by this vertex.
         */
        private final String uniqueName;

        /**
         * List of edges to functions that are called by this one.
         */
        private final List<CallEdge> successors;

        /**
         * Unmodifiable view of the successors list.
         */
        private final List<CallEdge> unmodifiableSuccessors;

        /**
         * List of edges to this function from functions that call it.
         */
        private final List<CallEdge> predecessors;

        /**
         * Unmodifiable view of the predecessors list.
         */
        private final List<CallEdge> unmodifiablePredecessors;

        /**
         * Number of this vertex in the DFS tree.
         */
        private Optional<Integer> dfsTreeNumber;

        /**
         * Parent of the vertex in the DFS tree.
         */
        private Optional<FunctionVertex> dfsTreeParent;

        /**
         * Minimum of DFS numbers of all descendants of this vertex in the DFS
         * tree and of all DFS numbers of vertices accessible from all
         * descendants of this vertex in the DFS tree by a non-tree edge.
         * A vertex is considered a descendant of itself.
         */
        private Optional<Integer> lowpoint;

        /**
         * Maximum number of a descendant of this vertex in the DFS tree.
         */
        private Optional<Integer> maximumDfsDescendantNumber;

        /**
         * Set with identifiers of biconnected components this vertex belongs
         * to.
         */
        private final Set<Integer> biconnectedComponents;

        /**
         * Unmodifiable view of the set with identifiers of biconnected
         * components.
         */
        private final Set<Integer> unmodifiableBiconnectedComponents;

        /**
         * Estimation of the frequency of calls to the function represented by
         * this vertex.
         */
        private double frequencyEstimation;

        /**
         * Bank the function represented by this vertex is assigned to.
         */
        private Optional<String> targetBank;

        private FunctionVertex(String uniqueName) {
            checkNotNull(uniqueName, "unique name cannot be null");
            checkArgument(!uniqueName.isEmpty(), "unique name cannot be an empty string");
            this.uniqueName = uniqueName;
            this.successors = new ArrayList<>();
            this.unmodifiableSuccessors = Collections.unmodifiableList(this.successors);
            this.predecessors = new ArrayList<>();
            this.unmodifiablePredecessors = Collections.unmodifiableList(this.predecessors);
            this.dfsTreeNumber = Optional.absent();
            this.dfsTreeParent = Optional.absent();
            this.lowpoint = Optional.absent();
            this.maximumDfsDescendantNumber = Optional.absent();
            this.biconnectedComponents = new HashSet<>();
            this.unmodifiableBiconnectedComponents = Collections.unmodifiableSet(this.biconnectedComponents);
            this.frequencyEstimation = 0.;
            this.targetBank = Optional.absent();
        }

        /**
         * Get the unique name of the function represented by this vertex.
         *
         * @return Unique name of the function of this vertex.
         */
        private String getUniqueName() {
            return uniqueName;
        }

        /**
         * Unmodifiable view of the list with edges to functions called by this
         * one which are target vertices in the edges objects.
         *
         * @return List with successors.
         */
        private List<CallEdge> getSuccessors() {
            return unmodifiableSuccessors;
        }

        /**
         * Unmodifiable view of the list with edges from functions that call
         * this one which are source vertices in the edges objects.
         *
         * @return List with predecessor edges.
         */
        private List<CallEdge> getPredecessors() {
            return unmodifiablePredecessors;
        }

        /**
         * Get iterable with all neighbours of this vertex: both successors and
         * predecessors. The iterator does not support removing elements.
         *
         * @return Iterable with all neighbours of this vertex.
         */
        private Iterable<FunctionVertex> getAllNeighbours() {
            return new NeighboursIterable();
        }

        /**
         * Get iterable with all children of this vertex in the DFS tree.
         *
         * @return Iterable with children of this vertex.
         */
        private Iterable<FunctionVertex> getDfsTreeChildren() {
            return new DfsTreeChildrenIterable();
        }

        /**
         * Create and add a new edge from this vertex to the given one. It
         * represents a call made by this function to the given one.
         *
         * @param successor Successor to add.
         */
        private void addCall(FunctionVertex successor, int enclosingLoopsCount,
                    int enclosingConditionalStmtsCount) {
            checkNotNull(successor, "successor cannot be null");
            checkArgument(enclosingLoopsCount >= 0, "count of enclosing loops cannot be negative");
            checkArgument(enclosingConditionalStmtsCount >= 0, "count of enclosing conditional statements cannot be negative");

            final CallEdge newEdge = new CallEdge(this, successor, enclosingLoopsCount,
                    enclosingConditionalStmtsCount);
            this.successors.add(newEdge);
            successor.predecessors.add(newEdge);
        }

        /**
         * Remove all edges from predecessors and to successors that this vertex
         * is incident to. After call to this method the vertex becomes
         * isolated.
         */
        private void removeAllEdges() {
            // Remove from successors
            for (CallEdge successor : successors) {
                final Iterator<CallEdge> predecessorsIt =
                        successor.getTargetVertex().predecessors.iterator();
                while (predecessorsIt.hasNext()) {
                    if (predecessorsIt.next().getSourceVertex() == this) {
                        predecessorsIt.remove();
                    }
                }
            }

            // Remove from predecessors
            for (CallEdge predecessor : predecessors) {
                final Iterator<CallEdge> successorsIt =
                        predecessor.getSourceVertex().successors.iterator();
                while (successorsIt.hasNext()) {
                    if (successorsIt.next().getTargetVertex() == this) {
                        successorsIt.remove();
                    }
                }
            }

            this.successors.clear();
            this.predecessors.clear();
        }

        /**
         * Set the number of this vertex in the DFS tree. It can be set exactly
         * once.
         *
         * @param number Number to set.
         */
        private void setDfsTreeNumber(int number) {
            checkArgument(number >= 0, "number cannot be negative");
            checkState(!dfsTreeNumber.isPresent(), "DFS tree number has been already set");
            this.dfsTreeNumber = Optional.of(number);
        }

        /**
         * Check if a number in the DFS tree has been assigned to this vertex.
         *
         * @return <code>true</code> if and only if a number in the DFS tree has
         *         been assigned to this vertex.
         */
        private boolean hasDfsTreeNumber() {
            return this.dfsTreeNumber.isPresent();
        }

        /**
         * Get the number of this vertex in the DFS tree.
         *
         * @return The number of this vertex in the DFS tree.
         * @throws IllegalStateException The DFS tree number has not been set
         *                               yet.
         */
        private int getDfsTreeNumber() {
            checkState(dfsTreeNumber.isPresent(), "DFS tree number has not been set yet");
            return dfsTreeNumber.get();
        }

        /**
         * Set the parent of this vertex in the DFS tree. It can be set exactly
         * once.
         *
         * @param parent Parent to set.
         */
        private void setDfsTreeParent(FunctionVertex parent) {
            checkNotNull(parent, "parent cannot be null");
            checkArgument(parent != this, "a vertex cannot be the parent of itself");
            checkState(!dfsTreeParent.isPresent(), "the DFS tree parent has been already set");
            this.dfsTreeParent = Optional.of(parent);
        }

        /**
         * Get the parent of this vertex in the DFS tree. The object is absent
         * if this vertex is the root.
         *
         * @return Parent of this vertex in the DFS tree.
         */
        private Optional<FunctionVertex> getDfsTreeParent() {
            return dfsTreeParent;
        }

        /**
         * Set the lowpoint of this vertex. It can be set exactly once.
         *
         * @param number DFS tree number of a vertex to set as the lowpoint.
         */
        private void setLowpoint(int number) {
            checkArgument(number >= 0, "number cannot be negative");
                    checkState(!lowpoint.isPresent(), "lowpoint has been already set");
            this.lowpoint = Optional.of(number);
        }

        /**
         * Set the lowpoint of this vertex to the given number if it is less
         * than the current lowpoint.
         *
         * @param number Candidate for the lowpoint of this vertex.
         */
        private void updateLowpoint(int number) {
            checkState(lowpoint.isPresent(), "lowpoint has not been set yet");
            this.lowpoint = Optional.of(Math.min(this.lowpoint.get(), number));
        }

        /**
         * Get the lowpoint of this vertex.
         *
         * @return Lowpoint of this vertex.
         */
        private int getLowpoint() {
            return lowpoint.get();
        }

        /**
         * Set the maximum number of a descendant of this vertex in the DFS
         * tree. It can be set exactly once.
         *
         * @param number Number to set.
         */
        private void setMaximumDfsDescendantNumber(int number) {
            checkArgument(number >= 0, "number cannot be negative");
            checkState(!maximumDfsDescendantNumber.isPresent(),
                    "maximum DFS descendant number has been already set");
            this.maximumDfsDescendantNumber = Optional.of(number);
        }

        /**
         * Set the maximum DFS descendant number to the given one if it is
         * greater than the current maximum DFS descendant number.
         *
         * @param number Candidate for the maximum DFS descendant number.
         */
        private void updateMaximumDfsDescendantNumber(int number) {
            checkArgument(number >= 0, "number cannot be negative");
            checkState(maximumDfsDescendantNumber.isPresent(),
                    "maximum DFS descendant number has not been set yet");
            this.maximumDfsDescendantNumber = Optional.of(Math.max(this.maximumDfsDescendantNumber.get(), number));
        }

        /**
         * Get the maximum number of a descendant of this vertex in the DFS
         * tree.
         *
         * @return Maximum number of a descendant of this vertex in the DFS
         *         tree.
         */
        private int getMaximumDfsDescendantNumber() {
            return maximumDfsDescendantNumber.get();
        }

        /**
         * Add the biconnected component to the set of biconnected components
         * this vertex belongs to.
         *
         * @param number Number to add.
         */
        private void addBiconnectedComponent(int number) {
            this.biconnectedComponents.add(number);
        }

        /**
         * Get the unmodifiable view of the set of biconnected components this
         * vertex belongs to.
         *
         * @return Set with identifiers of biconnected components of this
         *         vertex.
         */
        private Set<Integer> getBiconnectedComponents() {
            return unmodifiableBiconnectedComponents;
        }

        /**
         * Get the estimation of frequency of calls to the function represented
         * by this vertex.
         *
         * @return Estimation of the frequency of the function represented by
         *         this vertex being executed.
         */
        private double getFrequencyEstimation() {
            return frequencyEstimation;
        }

        /**
         * Increase the estimation of the frequency of calls to the function
         * represented by this vertex.
         *
         * @param value The size of the increase (it will be added to the
         *              current frequency estimation).
         */
        private void increaseFrequencyEstimation(double value) {
            checkArgument(value >= 0., "value cannot be negative");
            this.frequencyEstimation += value;
        }

        /**
         * Get the name of the bank the function represented by this vertex is
         * assigned to. The value is absent if the function is not assigned yet.
         *
         * @return Name of the bank for this function.
         */
        private Optional<String> getTargetBank() {
            return targetBank;
        }

        /**
         * Set the value indicating that this function has been assigned to
         * a bank.
         *
         * @throws IllegalStateException This function has been already assigned
         *                               to a bank.
         */
        private void assigned(String targetBank) {
            checkNotNull(targetBank, "target bank cannot be null");
            checkArgument(!targetBank.isEmpty(), "target bank cannot be an empty string");
            checkState(!this.targetBank.isPresent(), "the function has been already assigned to a bank");
            this.targetBank = Optional.of(targetBank);
        }

        /**
         * Class that allows easy iteration over all neighbours of a vertex.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private final class NeighboursIterable implements Iterable<FunctionVertex> {
            @Override
            public Iterator<FunctionVertex> iterator() {
                return new NeighboursIterator();
            }

            /**
             * Iterator over all neighbours of a vertex.
             *
             * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
             */
            private final class NeighboursIterator implements Iterator<FunctionVertex> {
                private final Iterator<CallEdge> successorsIt = FunctionVertex.this.successors.iterator();
                private final Iterator<CallEdge> predecessorsIt = FunctionVertex.this.predecessors.iterator();

                @Override
                public boolean hasNext() {
                    return successorsIt.hasNext() || predecessorsIt.hasNext();
                }

                @Override
                public FunctionVertex next() {
                    if (successorsIt.hasNext()) {
                        return successorsIt.next().getTargetVertex();
                    } else if (predecessorsIt.hasNext()) {
                        return predecessorsIt.next().getSourceVertex();
                    } else {
                        throw new NoSuchElementException("no more neighbours in this iterator");
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("neighbours iterator does not support removal of elements");
                }
            }
        }

        /**
         * Iterable with children of the vertex in the DFS tree.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private final class DfsTreeChildrenIterable implements Iterable<FunctionVertex> {
            @Override
            public Iterator<FunctionVertex> iterator() {
                return new DfsTreeChildrenIterator();
            }

            /**
             * Iterator that returns children of the vertex in the DFS tree.
             *
             * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
             */
            private final class DfsTreeChildrenIterator implements Iterator<FunctionVertex> {
                private final Iterator<FunctionVertex> neighboursIt = FunctionVertex.this.getAllNeighbours().iterator();
                private Optional<FunctionVertex> nextDfsTreeChild = Optional.absent();

                @Override
                public boolean hasNext() {
                    advance();
                    return nextDfsTreeChild.isPresent();
                }

                @Override
                public FunctionVertex next() {
                    advance();
                    if (nextDfsTreeChild.isPresent()) {
                        final FunctionVertex nextChild = nextDfsTreeChild.get();
                        nextDfsTreeChild = Optional.absent();
                        return nextChild;
                    } else {
                        throw new NoSuchElementException("no more children in the DFS tree");
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("DFS tree children iterator does not support removal of elements");
                }

                private void advance() {
                    while (!nextDfsTreeChild.isPresent() && neighboursIt.hasNext()) {
                        final FunctionVertex neighbour = neighboursIt.next();
                        if (neighbour.getDfsTreeParent().isPresent()
                                && neighbour.getDfsTreeParent().get() == FunctionVertex.this) {
                            this.nextDfsTreeChild = Optional.of(neighbour);
                        }
                    }
                }
            }
        }
    }

    /**
     * An edge in the call graph. It is directed.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class CallEdge {
        /**
         * Source vertex of this edge, i.e. its tail.
         */
        private final FunctionVertex sourceVertex;

        /**
         * Target vertex of this edge, i.e. its head.
         */
        private final FunctionVertex targetVertex;

        /**
         * Count of loops enclosing this call.
         */
        private final int enclosingLoopsCount;

        /**
         * Count of conditional statements and conditional expressions that
         * enclose this call.
         */
        private final int enclosingConditionalStmtsCount;

        /**
         * Estimation of the frequency of this call during the program
         * execution.
         */
        private double frequencyEstimation;

        private CallEdge(FunctionVertex sourceVertex, FunctionVertex targetVertex,
                    int enclosingLoopsCount, int enclosingConditionalStmtsCount) {
            checkNotNull(sourceVertex, "source vertex cannot be null");
            checkNotNull(targetVertex, "target vertex cannot be null");
            checkArgument(sourceVertex != targetVertex, "source and target vertices cannot be the same");
            checkArgument(enclosingLoopsCount >= 0, "count of enclosing loops cannot be negative");
            checkArgument(enclosingConditionalStmtsCount >= 0, "count of enclosing conditional statements cannot be negative");
            this.sourceVertex = sourceVertex;
            this.targetVertex = targetVertex;
            this.enclosingLoopsCount = enclosingLoopsCount;
            this.enclosingConditionalStmtsCount = enclosingConditionalStmtsCount;
            this.frequencyEstimation = 0.;
        }

        private FunctionVertex getSourceVertex() {
            return sourceVertex;
        }

        private FunctionVertex getTargetVertex() {
            return targetVertex;
        }

        private int getEnclosingLoopsCount() {
            return enclosingLoopsCount;
        }

        private int getEnclosingConditionalStmtsCount() {
            return enclosingConditionalStmtsCount;
        }

        private double getFrequencyEstimation() {
            return frequencyEstimation;
        }

        private void increaseFrequencyEstimation(double value) {
            checkArgument(value >= 0., "value cannot be negative");
            this.frequencyEstimation += value;
        }
    }

    /**
     * Interface that provides abstraction for neighbours of a vertex in the
     * call graph.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface NeighboursProvider {
        Iterable<FunctionVertex> getNeighbours(FunctionVertex vertex);
    }

    /**
     * This neighbours provider returns iterable with all neighbours of the
     * given vertex except neighbours that have been already assigned to
     * a bank.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class UnassignedNeighboursProvider implements NeighboursProvider {
        @Override
        public Iterable<FunctionVertex> getNeighbours(FunctionVertex vertex) {
            checkNotNull(vertex, "vertex cannot be null");
            return FluentIterable.from(vertex.getAllNeighbours())
                    .filter(new UnassignedVertexPredicate());
        }
    }

    /**
     * Successors provider returns iterable with all successors of a vertex. The
     * graph is viewed as directed.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class SuccessorsProvider implements NeighboursProvider {
        @Override
        public Iterable<FunctionVertex> getNeighbours(FunctionVertex vertex) {
            checkNotNull(vertex, "vertex cannot be null");
            return FluentIterable.from(vertex.getSuccessors())
                    .transform(new TargetVertexTransformation());
        }
    }

    /**
     * This neighbours provider returns iterable with children of the vertex
     * in the DFS tree.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class UnassignedDfsTreeChildrenProvider implements NeighboursProvider {
        @Override
        public Iterable<FunctionVertex> getNeighbours(FunctionVertex vertex) {
            checkNotNull(vertex, "vertex cannot be null");
            return FluentIterable.from(vertex.getDfsTreeChildren())
                    .filter(new UnassignedVertexPredicate());
        }
    }

    /**
     * Interface with events that occur during a DFS traversal of a graph.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface DfsVertexVisitor {
        /**
         * The given vertex is visited during DFS and the traversal of its
         * neighbours has not yet started.
         *
         * @param vertex Vertex that is visited.
         * @param parent Vertex that is the parent in the DFS tree of the
         *               performed search.
         */
        void enterVertex(FunctionVertex vertex, Optional<FunctionVertex> parent);

        /**
         * All descendants of the given vertex have been visited.
         *
         * @param vertex Vertex that is visited.
         * @param parent Vertex that is the parent in the DFS tree of the
         *               performed search.
         */
        void leaveVertex(FunctionVertex vertex, Optional<FunctionVertex> parent);
    }

    /**
     * DFS vertex visitor that determines the biconnected components.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class BComponentsDfsVertexVisitor implements DfsVertexVisitor {
        /**
         * Stack with vertices that allows determining vertices of a biconnected
         * component.
         */
        private final Deque<FunctionVertex> verticesStack = new ArrayDeque<>();

        /**
         * Next unused number for a vertex in the DFS tree.
         */
        private int nextDfsNumber = 0;

        /**
         * Next unused number of a biconnected component.
         */
        private int nextBComponentNumber = 0;

        @Override
        public void enterVertex(FunctionVertex vertex, Optional<FunctionVertex> parent) {
            verticesStack.push(vertex);
            vertex.setDfsTreeNumber(nextDfsNumber++);
            if (parent.isPresent()) {
                vertex.setDfsTreeParent(parent.get());
            }
        }

        @Override
        public void leaveVertex(FunctionVertex vertex, Optional<FunctionVertex> parent) {
            computeLowpoint(vertex);
            computeMaximumDfsDescendant(vertex);
            popBiconnectedComponent(vertex);
        }

        private void computeLowpoint(FunctionVertex vertex) {
            vertex.setLowpoint(vertex.getDfsTreeNumber());
            for (FunctionVertex neighbour : vertex.getAllNeighbours()) {
                if (!neighbour.getTargetBank().isPresent()) {
                    if (neighbour.getDfsTreeParent().isPresent()
                            && neighbour.getDfsTreeParent().get() == vertex) {
                        // edge to a child in the DFS tree
                        vertex.updateLowpoint(neighbour.getLowpoint());
                    } else if (!vertex.getDfsTreeParent().isPresent()
                            || neighbour != vertex.getDfsTreeParent().get()) {
                        // a non-tree edge
                        vertex.updateLowpoint(neighbour.getDfsTreeNumber());
                    }
                }
            }
        }

        private void computeMaximumDfsDescendant(FunctionVertex vertex) {
            vertex.setMaximumDfsDescendantNumber(vertex.getDfsTreeNumber());
            for (FunctionVertex dfsTreeChild : vertex.getDfsTreeChildren()) {
                vertex.updateMaximumDfsDescendantNumber(dfsTreeChild.getMaximumDfsDescendantNumber());
            }
        }

        private void popBiconnectedComponent(FunctionVertex vertex) {
            if (vertex.getDfsTreeParent().isPresent()
                    && vertex.getLowpoint() >= vertex.getDfsTreeParent().get().getDfsTreeNumber()) {
                // New biconnected component found
                FunctionVertex descendant;
                do {
                    descendant = verticesStack.pop();
                    descendant.addBiconnectedComponent(nextBComponentNumber);
                } while (descendant != vertex);
                vertex.getDfsTreeParent().get().addBiconnectedComponent(nextBComponentNumber);
                ++nextBComponentNumber;
            } else if (!vertex.getDfsTreeParent().isPresent()) {
                final FunctionVertex lastVertex = verticesStack.pop();
                if (lastVertex != vertex) {
                    throw new RuntimeException("expecting the root on the vertices stack");
                } else if (!verticesStack.isEmpty()) {
                    throw new RuntimeException("too many elements on the stack, expecting only the root");
                }
                if (!vertex.getAllNeighbours().iterator().hasNext()) {
                    vertex.addBiconnectedComponent(nextBComponentNumber++);
                }
            }
        }
    }

    /**
     * Visitor that computes the topological ordering of vertices in a graph. If
     * the graph has not any cycles, then it will be correct. Otherwise, the
     * topological sort is impossible.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class TopologicalSortDfsVertexVisitor implements DfsVertexVisitor {
        private final Deque<FunctionVertex> ordering = new ArrayDeque<>();

        private Queue<FunctionVertex> getTopologicalOrdering() {
            return ordering;
        }

        @Override
        public void enterVertex(FunctionVertex vertex, Optional<FunctionVertex> parent) {
            // nothing to do
        }

        @Override
        public void leaveVertex(FunctionVertex vertex, Optional<FunctionVertex> parent) {
            ordering.addFirst(vertex);
        }
    }

    /**
     * Visitor that allocates visited vertices to banks.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class AllocatingDfsVertexVisitor implements DfsVertexVisitor {
        private final BComponentsPartitionContext context;
        private Optional<String> currentBank;
        private boolean failure;
        private int allocatedFunctionsCount;

        private AllocatingDfsVertexVisitor(BComponentsPartitionContext context) {
            checkNotNull(context, "context cannot be null");
            this.context = context;
            this.currentBank = Optional.absent();
            this.failure = false;
        }

        @Override
        public void enterVertex(FunctionVertex vertex, Optional<FunctionVertex> parent) {
            if (failure) {
                return;
            }

            final int functionSize = context.getFunctionSize(vertex.getUniqueName());

            if (!currentBank.isPresent() || context.getBankTable().getFreeSpace(currentBank.get()) < functionSize) {
                currentBank = context.getCeilingBank(functionSize);
            }

            if (currentBank.isPresent()) {
                context.assign(context.functions.get(vertex.getUniqueName()), currentBank.get());
                ++allocatedFunctionsCount;
            } else {
                failure = true;
            }
        }

        @Override
        public void leaveVertex(FunctionVertex vertex, Optional<FunctionVertex> parent) {
            // nothing to do
        }
    }

    /**
     * Object responsible for performing the depth-first search in the call
     * graph.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class DepthFirstSearcher {
        /**
         * Neighbours provider used by this depth-first search executor.
         */
        private final NeighboursProvider neighboursProvider;

        /**
         * Set with vertices that have been already visited by this searcher.
         */
        private final Set<FunctionVertex> visitedVertices;

        private DepthFirstSearcher(NeighboursProvider neighboursProvider) {
            checkNotNull(neighboursProvider, "neighbours provider cannot be null");
            this.neighboursProvider = neighboursProvider;
            this.visitedVertices = new HashSet<>();
        }

        /**
         * Perform the depth-first search starting in the given vertex. All
         * events of the search are emitted to the given vertex visitor.
         *
         * @param startVertex The start vertex of the search.
         * @param vertexVisitor Visitor of the vertices encountered during the
         *                      search. It will be notified about all events
         *                      during the search.
         */
        private void search(FunctionVertex startVertex, DfsVertexVisitor vertexVisitor) {
            checkNotNull(startVertex, "start vertex cannot be null");
            checkNotNull(vertexVisitor, "vertex visitor cannot be null");

            final Deque<DfsStackElement> dfsStack = new ArrayDeque<>();

            if (!visitedVertices.contains(startVertex)) {
                visitedVertices.add(startVertex);
                dfsStack.push(new DfsStackElement(startVertex, neighboursProvider.getNeighbours(
                        startVertex).iterator()));
                vertexVisitor.enterVertex(startVertex, Optional.<FunctionVertex>absent());
            }

            while (!dfsStack.isEmpty()) {
                final FunctionVertex currentVertex = dfsStack.peek().getVertex();
                final Optional<FunctionVertex> nextVertex = dfsStack.peek().nextNeighbour();

                if (nextVertex.isPresent() && !visitedVertices.contains(nextVertex.get())) {
                    visitedVertices.add(nextVertex.get());
                    dfsStack.push(new DfsStackElement(nextVertex.get(), neighboursProvider.getNeighbours(
                            nextVertex.get()).iterator()));
                    vertexVisitor.enterVertex(nextVertex.get(), Optional.of(currentVertex));
                } else if (!nextVertex.isPresent()) {
                    dfsStack.pop();
                    final Optional<FunctionVertex> parent = !dfsStack.isEmpty()
                            ? Optional.of(dfsStack.peek().getVertex())
                            : Optional.<FunctionVertex>absent();
                    vertexVisitor.leaveVertex(currentVertex, parent);
                }
            }
        }

        /**
         * Element of the DFS stack during the depth-first search.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private static final class DfsStackElement {
            private final FunctionVertex vertex;
            private final Iterator<FunctionVertex> neighboursIt;

            private DfsStackElement(FunctionVertex vertex, Iterator<FunctionVertex> neighboursIt) {
                checkNotNull(vertex, "vertex cannot be null");
                checkNotNull(neighboursIt, "neighbours iterator cannot be null");
                this.vertex = vertex;
                this.neighboursIt = neighboursIt;
            }

            private FunctionVertex getVertex() {
                return vertex;
            }

            private Optional<FunctionVertex> nextNeighbour() {
                return neighboursIt.hasNext()
                        ? Optional.of(neighboursIt.next())
                        : Optional.<FunctionVertex>absent();
            }
        }
    }

    /**
     * Enum type that determines how the allocation of functions in given DFS
     * tree node will be performed.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private enum AllocationDirection {
        /**
         * All functions in the subtree rooted in the vertex will be allocated.
         */
        DOWN_TREE,
        /**
         * All function in the whole tree will be allocated except functions
         * from the tree rooted at given vertex.
         */
        UP_TREE,
    }

    /**
     * Object that represents an allocation of functions in a DFS tree.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class TreeAllocation {
        private final FunctionVertex vertex;
        private final FunctionVertex dfsTreeRoot;
        private final AllocationDirection direction;
        private final String bankName;
        private final int totalFunctionsSize;

        private TreeAllocation(FunctionVertex vertex, FunctionVertex dfsTreeRoot,
                    AllocationDirection direction, String bankName, int totalFunctionsSize) {
            checkNotNull(vertex, "vertex cannot be null");
            checkNotNull(dfsTreeRoot, "DFS tree root cannot be null");
            checkNotNull(direction, "direction cannot be null");
            checkNotNull(bankName, "name of the bank cannot be null");
            checkArgument(!bankName.isEmpty(), "name of the bank cannot be an empty string");
            checkArgument(totalFunctionsSize >= 0, "total size of allocated functions cannot be negative");
            this.vertex = vertex;
            this.dfsTreeRoot = dfsTreeRoot;
            this.direction = direction;
            this.bankName = bankName;
            this.totalFunctionsSize = totalFunctionsSize;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper("TreeAllocation")
                    .add("vertex", vertex.getUniqueName())
                    .add("dfs-tree-root", dfsTreeRoot.getUniqueName())
                    .add("direction", direction)
                    .add("target-bank", bankName)
                    .add("total-functions-size", totalFunctionsSize)
                    .toString();
        }
    }

    /**
     * Object that allows comparing tree allocations. The order defined by it
     * determines preferred allocations - an allocation is preferred and
     * considered better than another one if it is greater in the order.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class TreeAllocationComparator implements Comparator<TreeAllocation> {
        private final String commonBankName;

        private TreeAllocationComparator(String commonBankName) {
            checkNotNull(commonBankName, "name of the common bank cannot be null");
            checkArgument(!commonBankName.isEmpty(), "name of the common bank cannot be an empty string");
            this.commonBankName = commonBankName;
        }

        @Override
        public int compare(TreeAllocation allocation1, TreeAllocation allocation2) {
            checkNotNull(allocation1, "first tree allocation cannot be null");
            checkNotNull(allocation2, "second tree allocation cannot be null");

            // Compare total functions sizes
            final int totalSizesResult = Integer.compare(allocation1.totalFunctionsSize,
                    allocation2.totalFunctionsSize);
            if (totalSizesResult != 0) {
                return totalSizesResult;
            }

            // Compare by destination bank
            if (allocation1.bankName.equals(commonBankName)
                    && !allocation2.bankName.equals(commonBankName)) {
                return 1;
            } else if (!allocation1.bankName.equals(commonBankName)
                    && allocation2.bankName.equals(commonBankName)) {
                return -1;
            }

            return allocation1.vertex.getUniqueName().compareTo(
                    allocation2.vertex.getUniqueName());
        }
    }

    /**
     * Comparator that compares trees by looking at sizes of their functions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class FunctionSizeComparator implements Comparator<FunctionVertex> {
        private final IntervalTree<Integer> functionsSizes;

        private FunctionSizeComparator(IntervalTree<Integer> functionsSizes) {
            checkNotNull(functionsSizes, "functions sizes tree cannot be null");
            this.functionsSizes = functionsSizes;
        }

        @Override
        public int compare(FunctionVertex tree1, FunctionVertex tree2) {
            checkNotNull(tree1, "the first tree cannot be null");
            checkNotNull(tree2, "the second tree cannot be null");

            final int functionsSize1 = functionsSizes.compute(tree1.getDfsTreeNumber(),
                    tree1.getMaximumDfsDescendantNumber() + 1);
            final int functionsSize2 = functionsSizes.compute(tree2.getDfsTreeNumber(),
                    tree2.getMaximumDfsDescendantNumber() + 1);

            final int sizesResult = Integer.compare(functionsSize1, functionsSize2);

            return sizesResult != 0
                    ? sizesResult
                    : tree1.getUniqueName().compareTo(tree2.getUniqueName());
        }
    }

    /**
     * Comparator of functions that compares using estimation of their
     * frequencies.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class FunctionFrequencyComparator implements Comparator<FunctionVertex> {
        @Override
        public int compare(FunctionVertex vertex1, FunctionVertex vertex2) {
            checkNotNull(vertex1, "the first vertex cannot be null");
            checkNotNull(vertex2, "the second vertex cannot be null");
            return Double.compare(vertex2.getFrequencyEstimation(),
                    vertex1.getFrequencyEstimation());
        }
    }

    /**
     * Comparator for neighbours of a vertex. It specifies their order on the
     * neighbourhood lists.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class NeighbourComparator implements Comparator<Reference> {
        private final ImmutableMap<String, Integer> neighboursCounts;

        private NeighbourComparator(ReferencesGraph refsGraph) {
            checkNotNull(refsGraph, "references graph cannot be null");
            final PrivateBuilder builder = new RealBuilder(refsGraph);
            this.neighboursCounts = builder.buildNeighboursCounts();
        }

        @Override
        public int compare(Reference reference1, Reference reference2) {
            checkNotNull(reference1, "the first reference cannot be null");
            checkNotNull(reference2, "the second reference cannot be null");
            checkState(neighboursCounts.containsKey(reference1.getReferencedNode().getUniqueName()),
                    "the first function is unknown");
            checkState(neighboursCounts.containsKey(reference2.getReferencedNode().getUniqueName()),
                    "the second function is unknown");

            final String funUniqueName1 = reference1.getReferencedNode().getUniqueName();
            final String funUniqueName2 = reference2.getReferencedNode().getUniqueName();

            final int neighboursCountsResult = Integer.compare(neighboursCounts.get(funUniqueName1),
                    neighboursCounts.get(funUniqueName2));
            return neighboursCountsResult != 0
                    ? neighboursCountsResult
                    : funUniqueName1.compareTo(funUniqueName2);
        }

        private interface PrivateBuilder {
            ImmutableMap<String, Integer> buildNeighboursCounts();
        }

        private static final class RealBuilder implements PrivateBuilder {
            private final ReferencesGraph refsGraph;

            private RealBuilder(ReferencesGraph refsGraph) {
                this.refsGraph = refsGraph;
            }

            @Override
            public ImmutableMap<String, Integer> buildNeighboursCounts() {
                final ImmutableMap.Builder<String, Integer> neighboursCountsBuilder =
                        ImmutableMap.builder();
                for (EntityNode ordinaryId : refsGraph.getOrdinaryIds().values()) {
                    if (ordinaryId.getKind() == EntityNode.Kind.FUNCTION) {
                        final Set<String> neighboursNames = new HashSet<>();

                        for (Reference reference : ordinaryId.getSuccessors()) {
                            if (!reference.isInsideNotEvaluatedExpr()
                                    && reference.getType() == Reference.Type.CALL) {
                                neighboursNames.add(reference.getReferencedNode().getUniqueName());
                            }
                        }

                        for (Reference reference : ordinaryId.getPredecessors()) {
                            if (!reference.isInsideNotEvaluatedExpr()
                                    && reference.getType() == Reference.Type.CALL) {
                                neighboursNames.add(reference.getReferencingNode().getUniqueName());
                            }
                        }

                        neighboursCountsBuilder.put(ordinaryId.getUniqueName(), neighboursNames.size());
                    }
                }

                return neighboursCountsBuilder.build();
            }
        }
    }

    /**
     * Transformation that returns the target vertex of the given call edge.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class TargetVertexTransformation implements Function<CallEdge, FunctionVertex> {
        @Override
        public FunctionVertex apply(CallEdge callEdge) {
            checkNotNull(callEdge, "call edge cannot be null");
            return callEdge.getTargetVertex();
        }
    }

    /**
     * Predicate that is fulfilled if and only if the vertex has not been
     * assigned to a bank.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class UnassignedVertexPredicate implements Predicate<FunctionVertex> {
        @Override
        public boolean apply(FunctionVertex vertex) {
            checkNotNull(vertex, "vertex cannot be null");
            return !vertex.getTargetBank().isPresent();
        }
    }
}
