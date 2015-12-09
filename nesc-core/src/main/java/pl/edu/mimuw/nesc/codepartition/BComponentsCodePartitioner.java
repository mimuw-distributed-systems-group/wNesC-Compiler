package pl.edu.mimuw.nesc.codepartition;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.codepartition.context.PartitionContext;
import pl.edu.mimuw.nesc.codesize.CodeSizeEstimation;
import pl.edu.mimuw.nesc.common.AtomicSpecification;
import pl.edu.mimuw.nesc.common.util.DoubleSumIntervalTreeOperation;
import pl.edu.mimuw.nesc.common.util.FindUnionSet;
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
     * Pattern specifying the smallest weights arbitrary subtree partitioning
     * allocation picker.
     */
    public static final Pattern PATTERN_ASP_ALLOCATION_PICKER_SMALLEST_WEIGHTS =
            Pattern.compile("smallest-weights");

    /**
     * Pattern specifying the smallest weights from biggest size allocation
     * picker.
     */
    public static final Pattern PATTERN_ASP_ALLOCATION_PICKER_SMALLEST_WEIGHTS_FROM_BIGGEST_SIZE =
            Pattern.compile("smallest-weights-from-(?<size>[1-9]\\d*)-biggest-size");

    /**
     * Pattern specifying the biggest size from smallest weights allocation
     * picker.
     */
    public static final Pattern PATTERN_ASP_ALLOCATION_PICKER_BIGGEST_SIZE_FROM_SMALLEST_WEIGHTS =
            Pattern.compile("biggest-size-from-(?<size>[1-9]\\d*)-smallest-weights");

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
     * Picker for the arbitrary subtree partitioning.
     */
    private final AllocationPicker aspPicker;

    /**
     * Listener that will be notified about detected issues.
     */
    private final CompilationListener listener;

    /**
     * Kind of the spanning forests built by this partitioner for creating
     * partitions.
     */
    private final SpanningForestKind spanningForestKind;

    /**
     * Value indicating the mode of the arbitrary subtree partitioning to use by
     * this partitioner.
     */
    private final ArbitrarySubtreePartitioningMode arbitrarySubtreePartitioning;

    /**
     * Value that controls the increase of a frequency estimation for a call
     * instruction inside a loop. It must be greater than or equal to 1.
     */
    private final double loopFactor;

    /**
     * Value that controls the decrease of a frequency estimation for a call
     * instruction inside a conditional statement. It must be greater than or
     * equal to 0 and less than or equal to 1.
     */
    private final double conditionalFactor;

    /**
     * Object used by this partitioner to allocate functions to the common bank
     * in the first stage of the heuristic.
     */
    private final CommonBankFiller commonBankFiller;

    /**
     * Target bank picker for allocations in cut vertices.
     */
    private final TargetBankPicker targetBankPickerCutVertices;

    /**
     * Target bank picker for DFS allocations.
     */
    private final TargetBankPicker targetBankPickerDfs;

    /**
     * Target bank picker for arbitrary subtree partitioning.
     */
    private final Optional<TargetBankPicker> targetBankPickerAsp;

    public BComponentsCodePartitioner(BankSchema bankSchema, AtomicSpecification atomicSpec,
            double loopFactor, double conditionalFactor, CommonBankAllocationAlgorithm commonBankAlgorithm,
            SpanningForestKind spanningForestKind, boolean preferHigherEstimateAllocations,
            ArbitrarySubtreePartitioningMode arbitrarySubtreePartitioningMode,
            String aspPickerDescription, TargetBankChoiceMethod targetBankChoiceMethodCutVertices,
            TargetBankChoiceMethod targetBankChoiceMethodDfs,
            Optional<TargetBankChoiceMethod> targetBankChoiceMethodAsp,
            CompilationListener listener) {
        checkNotNull(bankSchema, "bank schema cannot be null");
        checkNotNull(atomicSpec, "atomic specification cannot be null");
        checkNotNull(commonBankAlgorithm, "common bank allocation algorithm cannot be null");
        checkNotNull(spanningForestKind, "spanning forest kind cannot be null");
        checkNotNull(arbitrarySubtreePartitioningMode, "arbitrary subtree partitioning mode cannot be null");
        checkNotNull(aspPickerDescription, "arbitrary subtree partitioning picker description cannot be null");
        checkNotNull(targetBankChoiceMethodCutVertices, "target bank choice method for cut vertices allocation cannot be null");
        checkNotNull(targetBankChoiceMethodDfs, "target bank choice method for DFS allocation cannot be null");
        checkNotNull(targetBankChoiceMethodAsp, "target bank choice method for arbitrary subtree partitioning cannot be null");
        checkNotNull(listener, "listener cannot be null");
        checkArgument(loopFactor >= 1., "the loop factor must be greater than or equal to 1");
        checkArgument(0. <= conditionalFactor && conditionalFactor <= 1., "the conditional factor must be in range [0, 1]");
        checkArgument(!aspPickerDescription.isEmpty(), "arbitrary subtree partitioning picker description cannot be an empty string");
        final TargetBankPickerFactory targetBankPickerFactory = new TargetBankPickerFactory();
        this.bankSchema = bankSchema;
        this.commonBankAllocator = new CommonBankAllocator(atomicSpec);
        this.treeAllocationsComparator = new TreeAllocationComparator(bankSchema.getCommonBankName(),
                preferHigherEstimateAllocations);
        this.aspPicker = new AllocationPickerFactory(bankSchema, preferHigherEstimateAllocations)
                .create(aspPickerDescription);
        this.commonBankFiller = new CommonBankFillerFactory().create(commonBankAlgorithm);
        this.loopFactor = loopFactor;
        this.conditionalFactor = conditionalFactor;
        this.spanningForestKind = spanningForestKind;
        this.arbitrarySubtreePartitioning = arbitrarySubtreePartitioningMode;
        this.targetBankPickerCutVertices = targetBankPickerFactory.create(targetBankChoiceMethodCutVertices);
        this.targetBankPickerDfs = targetBankPickerFactory.create(targetBankChoiceMethodDfs);
        this.targetBankPickerAsp = targetBankChoiceMethodAsp.transform(targetBankPickerFactory.getFunction());
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
        commonBankFiller.fillCommonBank(context);
        computeBiconnectedComponents(context);
        computeSpanningForest(context, topologicalOrdering);
        updateIntervalTrees(context);
        assignFunctions(context);

        return context.getBankTable();
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

        for (final FunctionVertex currentVertex : topologicalOrdering) {
            completedVertices.add(currentVertex);

            if (currentVertex.getPredecessors().isEmpty()) {
                currentVertex.increaseFrequencyEstimation(1.);
            }

            for (CallEdge edge : currentVertex.getSuccessors()) {
                if (edge.getTargetVertex() != currentVertex
                        && !completedVertices.contains(edge.getTargetVertex())) {
                    edge.increaseFrequencyEstimation(currentVertex.getFrequencyEstimation()
                            * Math.pow(loopFactor, edge.getEnclosingLoopsCount())
                            * Math.pow(conditionalFactor, edge.getEnclosingConditionalStmtsCount()));
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

    private void computeSpanningForest(BComponentsPartitionContext context,
                Queue<FunctionVertex> topologicalOrdering) {
        if (spanningForestKind == SpanningForestKind.BCOMPONENTS) {
            return;
        }

        final List<CallEdge> sortedEdges = sortCallEdges(context);
        final ImmutableListMultimap<FunctionVertex, FunctionVertex> spanningForestNeighbours =
                computeSpanningForestNeighbours(context, sortedEdges);
        putSpanningForest(context, topologicalOrdering, spanningForestNeighbours);
    }

    private List<CallEdge> sortCallEdges(BComponentsPartitionContext context) {
        final List<CallEdge> edges = new ArrayList<>();

        // Add all edges to the list
        for (FunctionVertex funVertex : context.callGraph.getVertices().values()) {
            if (!funVertex.getTargetBank().isPresent()) {
                for (CallEdge successorEdge : funVertex.getSuccessors()) {
                    if (!successorEdge.getTargetVertex().getTargetBank().isPresent()) {
                        edges.add(successorEdge);
                    }
                }
            }
        }

        // Sort the edges in the ascending order of frequency estimations
        Collections.sort(edges, new FrequencyEstimationEdgeComparator());

        return edges;
    }

    /**
     * Compute the minimum or maximum spanning forest (depending on
     * {@link BComponentsCodePartitioner#spanningForestKind}) by using the
     * Kruskal's algorithm.
     *
     * @throws IllegalStateException The spanning forest kind of this
     *                               partitioner is
     *                               {@link SpanningForestKind#BCOMPONENTS}.
     */
    private ImmutableListMultimap<FunctionVertex, FunctionVertex> computeSpanningForestNeighbours(
            BComponentsPartitionContext context,
            List<CallEdge> sortedEdges
    ) {
        // Determine the order of iteration over the edges
        final List<CallEdge> edgesForIteration;
        switch (spanningForestKind) {
            case MINIMUM:
                edgesForIteration = sortedEdges;
                break;
            case MAXIMUM:
                edgesForIteration = Lists.reverse(sortedEdges);
                break;
            default:
                throw new IllegalStateException("unexpected spanning forest kind '"
                        + spanningForestKind + "'");
        }

        final FindUnionSet<String> connectedComponents = new FindUnionSet<>(
                context.callGraph.getVertices().keySet());
        final ImmutableListMultimap.Builder<FunctionVertex, FunctionVertex> neighboursBuilder =
                ImmutableListMultimap.builder();

        // Iterate over the edges building the spanning forest
        for (CallEdge currentEdge : edgesForIteration) {
            final FunctionVertex sourceVertex = currentEdge.getSourceVertex(),
                    targetVertex = currentEdge.getTargetVertex();

            if (!connectedComponents.find(sourceVertex.getUniqueName()).equals(
                    connectedComponents.find(targetVertex.getUniqueName()))) {
                neighboursBuilder.put(sourceVertex, targetVertex);
                neighboursBuilder.put(targetVertex, sourceVertex);
                connectedComponents.union(sourceVertex.getUniqueName(), targetVertex.getUniqueName());
            }
        }

        return neighboursBuilder.build();
    }

    private void putSpanningForest(
            BComponentsPartitionContext context,
            Queue<FunctionVertex> topologicalOrdering,
            ImmutableListMultimap<FunctionVertex, FunctionVertex> forestNeighbours
    ) {
        // Reset all vertices
        for (FunctionVertex vertex : context.callGraph.getVertices().values()) {
            if (!vertex.getTargetBank().isPresent()) {
                vertex.resetDfsTreeNumber();
                vertex.resetDfsTreeParent();
                vertex.resetMaximumDfsDescendantNumber();
            }
        }

        final DfsVertexVisitor spanningForestVisitor = new SpanningForestVertexVisitor();
        final DepthFirstSearcher depthFirstSearcher = new DepthFirstSearcher(
                new SpecificNeighboursProvider(forestNeighbours.asMap()));

        // Put the new spanning forest
        for (FunctionVertex funVertex : topologicalOrdering) {
            if (!funVertex.getTargetBank().isPresent() && !funVertex.hasDfsTreeNumber()) {
                depthFirstSearcher.search(funVertex, spanningForestVisitor);
            }
        }
    }

    private void updateIntervalTrees(BComponentsPartitionContext context) {
        for (FunctionVertex vertex : context.callGraph.getVertices().values()) {
            if (!vertex.getTargetBank().isPresent()) {
                context.functionsSizesTree.set(vertex.getDfsTreeNumber(),
                        context.getFunctionSize(vertex.getUniqueName()));
                context.frequencyEstimationsTree.set(vertex.getDfsTreeNumber(),
                        vertex.getFrequencyEstimation());
            }
        }
    }

    private void assignFunctions(BComponentsPartitionContext context) throws PartitionImpossibleException {
        int functionsLeft = computeUnallocatedFunctionsCount(context);
        final Map<FunctionVertex, Set<FunctionVertex>> allocationVertices =
                findAllocationVertices(context);

        while (functionsLeft != 0) {
            Optional<TreeAllocation> allocation;
            if (arbitrarySubtreePartitioning == ArbitrarySubtreePartitioningMode.NEVER
                    || arbitrarySubtreePartitioning == ArbitrarySubtreePartitioningMode.ON_EMERGENCY) {
                allocation = determineAllocation(context, allocationVertices);
                if (!allocation.isPresent() && arbitrarySubtreePartitioning == ArbitrarySubtreePartitioningMode.ON_EMERGENCY) {
                    allocation = findMinimumWeightAllocation(context, allocationVertices.keySet());
                }
            } else if (arbitrarySubtreePartitioning == ArbitrarySubtreePartitioningMode.ALWAYS) {
                allocation = findMinimumWeightAllocation(context, allocationVertices.keySet());
            } else {
                throw new IllegalStateException("unexpected arbitrary partitioning mode " + arbitrarySubtreePartitioning);
            }

            if (allocation.isPresent()) {
                functionsLeft -= performAllocation(context, allocation.get(),
                        allocationVertices.get(allocation.get().dfsTreeRoot));
                updateCutVerticesAfterAllocation(allocationVertices, allocation.get());
            } else if (arbitrarySubtreePartitioning == ArbitrarySubtreePartitioningMode.NEVER) {
                final FunctionVertex treeToAllocate = Collections.max(allocationVertices.keySet(),
                        new FunctionSizeComparator(context.functionsSizesTree));
                functionsLeft -= allocateTree(context, treeToAllocate);
                allocationVertices.remove(treeToAllocate);
            } else {
                throw new PartitionImpossibleException("not enough space");
            }
        }
    }

    private void updateCutVerticesAfterAllocation(Map<FunctionVertex, Set<FunctionVertex>> allocationVertices,
                TreeAllocation allocation) {
        if (allocation.direction == AllocationDirection.UP_TREE) {
            final Set<FunctionVertex> cutVertices = allocationVertices.get(allocation.dfsTreeRoot);
            allocationVertices.remove(allocation.dfsTreeRoot);

            if (!allocation.includesVertex) {
                allocationVertices.put(allocation.vertex, cutVertices);
            } else {
                for (FunctionVertex dfsTreeChild : allocation.vertex.getDfsTreeChildren()) {
                    if (!dfsTreeChild.getTargetBank().isPresent()) {
                        allocationVertices.put(dfsTreeChild, new HashSet<FunctionVertex>());
                    }
                }

                // Assign cut vertices
                for (FunctionVertex cutVertex : cutVertices) {
                    Optional<FunctionVertex> subtree = Optional.absent();

                    for (FunctionVertex dfsTreeChild : allocation.vertex.getDfsTreeChildren()) {
                        if (!dfsTreeChild.getTargetBank().isPresent()) {
                            if (dfsTreeChild.getDfsTreeNumber() <= cutVertex.getDfsTreeNumber()
                                    && cutVertex.getDfsTreeNumber() <= dfsTreeChild.getMaximumDfsDescendantNumber()) {
                                if (subtree.isPresent()) {
                                    throw new RuntimeException("cut vertex belongs to many trees");
                                }
                                subtree = Optional.of(dfsTreeChild);
                            }
                        }
                    }

                    if (!subtree.isPresent()) {
                        throw new RuntimeException("cannot find a tree for a cut vertex");
                    }

                    allocationVertices.get(subtree.get()).add(cutVertex);
                }
            }
        }

        if (allocation.vertex == allocation.dfsTreeRoot) {
            allocationVertices.remove(allocation.dfsTreeRoot);
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
        final ImmutableSet<FunctionVertex> startVertices = getAllocationStartVertices(allocation);
        return performAllocationWithStartVertices(context, allocation, cutVertices, startVertices);
    }

    private ImmutableSet<FunctionVertex> getAllocationStartVertices(TreeAllocation allocation) {
        final ImmutableSet<FunctionVertex> startVertices;
        switch (allocation.direction) {
            case UP_TREE:
                startVertices = ImmutableSet.of(allocation.dfsTreeRoot);
                break;
            case DOWN_TREE:
                if (allocation.includesVertex) {
                    startVertices = ImmutableSet.of(allocation.vertex);
                } else {
                    startVertices = ImmutableSet.copyOf(allocation.vertex.getDfsTreeChildren());
                }
                break;
            default:
                throw new RuntimeException("unexpected allocation direction '"
                        + allocation.direction + "'");
        }

        if (startVertices.isEmpty()) {
            throw new RuntimeException("unexpectedly no vertices to start an allocation");
        }

        return startVertices;
    }

    private int performAllocationWithStartVertices(BComponentsPartitionContext context, TreeAllocation allocation,
                Set<FunctionVertex> cutVertices, ImmutableSet<FunctionVertex> startVertices) {
        final Queue<FunctionVertex> queue = new ArrayDeque<>();
        final Set<FunctionVertex> visitedVertices = new HashSet<>();
        int allocatedFunctionsCount = 0;

        queue.addAll(startVertices);
        visitedVertices.addAll(queue);

        while (!queue.isEmpty()) {
            final FunctionVertex vertex = queue.remove();

            if (allocation.direction != AllocationDirection.UP_TREE
                    || allocation.vertex != vertex) {
                for (FunctionVertex dfsTreeChild : vertex.getDfsTreeChildren()) {
                    if (!dfsTreeChild.getTargetBank().isPresent()) {
                        if (!visitedVertices.contains(dfsTreeChild)
                                && (allocation.direction != AllocationDirection.UP_TREE
                                || dfsTreeChild != allocation.vertex || allocation.includesVertex)) {
                            visitedVertices.add(dfsTreeChild);
                            queue.add(dfsTreeChild);
                        }
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
            final double allFunctionsFrequency = context.frequencyEstimationsTree.compute(treeRoot.getDfsTreeNumber(),
                    treeRoot.getMaximumDfsDescendantNumber() + 1);
            candidateAllocations.clear();

            if (bestAllocation.isPresent()) {
                candidateAllocations.add(bestAllocation.get());
            }
            addRootAllocation(candidateAllocations, context, treeRoot,
                    allFunctionsSize, allFunctionsFrequency);

            for (FunctionVertex cutVertex : dfsTreeEntry.getValue()) {
                if (cutVertex == treeRoot) {
                    continue;
                }

                final AllocationDirection cutVertexMembership = determineVertexMembership(cutVertex);

                addTreeAllocation(candidateAllocations, context, cutVertex,
                        AllocationDirection.DOWN_TREE, cutVertexMembership,
                        allFunctionsSize, allFunctionsFrequency, treeRoot);
                addTreeAllocation(candidateAllocations, context, cutVertex,
                        AllocationDirection.UP_TREE, cutVertexMembership,
                        allFunctionsSize, allFunctionsFrequency, treeRoot);
            }

            if (!candidateAllocations.isEmpty()) {
                bestAllocation = Optional.of(Collections.max(candidateAllocations, treeAllocationsComparator));
            }
        }

        return bestAllocation;
    }

    private void addRootAllocation(Collection<TreeAllocation> candidateAllocations,
                BComponentsPartitionContext context, FunctionVertex treeRoot,
                int allFunctionsSize, double allFunctionsFrequency) {
        final Optional<String> targetBank = targetBankPickerCutVertices.pick(
                context, allFunctionsSize);
        if (targetBank.isPresent()) {
            candidateAllocations.add(new TreeAllocation(treeRoot, treeRoot,
                    AllocationDirection.DOWN_TREE, true, targetBank.get(),
                    allFunctionsSize, allFunctionsFrequency));
        }
    }

    private void addTreeAllocation(Collection<TreeAllocation> allocations,
                BComponentsPartitionContext context, FunctionVertex cutVertex,
                AllocationDirection direction, AllocationDirection cutVertexMembership,
                int allFunctionsSize, double allFunctionsFrequency, FunctionVertex treeRoot) {
        final int downTreeFirstVertexNumber = cutVertexMembership == AllocationDirection.DOWN_TREE
                ? cutVertex.getDfsTreeNumber()
                : (cutVertex.getDfsTreeNumber() + 1);
        final int downTreeFunctionsSize = context.functionsSizesTree.compute(downTreeFirstVertexNumber,
                cutVertex.getMaximumDfsDescendantNumber() + 1);
        final double downTreeFunctionsFrequency = context.frequencyEstimationsTree.compute(
                downTreeFirstVertexNumber, cutVertex.getMaximumDfsDescendantNumber() + 1);
        final int functionsSize;
        final double functionsFrequency;

        switch (direction) {
            case DOWN_TREE:
                functionsSize = downTreeFunctionsSize;
                functionsFrequency = downTreeFunctionsFrequency;
                break;
            case UP_TREE:
                functionsSize = allFunctionsSize - downTreeFunctionsSize;
                functionsFrequency = allFunctionsFrequency - downTreeFunctionsFrequency;
                break;
            default:
                throw new RuntimeException("unexpected allocation direction " + direction);
        }

        final Optional<String> targetBank = targetBankPickerCutVertices.pick(
                context, functionsSize);
        if (targetBank.isPresent()) {
            allocations.add(new TreeAllocation(cutVertex, treeRoot, direction,
                    direction == cutVertexMembership, targetBank.get(), functionsSize,
                    functionsFrequency));
        }
    }

    private int allocateTree(BComponentsPartitionContext context, FunctionVertex root)
                throws PartitionImpossibleException {
        final AllocatingDfsVertexVisitor allocatingVisitor = new AllocatingDfsVertexVisitor(
                context, targetBankPickerDfs);
        new DepthFirstSearcher(new UnassignedDfsTreeChildrenProvider())
                .search(root, allocatingVisitor);
        if (allocatingVisitor.failure) {
            throw new PartitionImpossibleException("not enough space");
        }
        return allocatingVisitor.allocatedFunctionsCount;
    }

    private Optional<TreeAllocation> findMinimumWeightAllocation(BComponentsPartitionContext context,
                Set<FunctionVertex> treeRoots) {
        aspPicker.clear();

        for (FunctionVertex treeRoot : treeRoots) {
            findMinimumWeightSubtree(context, treeRoot);
        }

        return Optional.<TreeAllocation>fromNullable(aspPicker.pick().orNull());
    }

    private void findMinimumWeightSubtree(BComponentsPartitionContext context, FunctionVertex treeRoot) {
        final int allFunctionsSize = context.functionsSizesTree.compute(treeRoot.getDfsTreeNumber(),
                treeRoot.getMaximumDfsDescendantNumber() + 1);
        final double allFunctionsFrequency = context.frequencyEstimationsTree.compute(treeRoot.getDfsTreeNumber(),
                treeRoot.getMaximumDfsDescendantNumber() + 1);

        final Set<FunctionVertex> enqueuedVertices = new HashSet<>();
        final Queue<FunctionVertex> queue = new ArrayDeque<>();
        queue.add(treeRoot);
        enqueuedVertices.addAll(queue);

        while (!queue.isEmpty()) {
            final FunctionVertex currentVertex = queue.remove();
            determineBestAllocationInVertex(context, currentVertex, treeRoot,
                    allFunctionsSize, allFunctionsFrequency);

            for (FunctionVertex dfsChild : currentVertex.getDfsTreeChildren()) {
                if (!dfsChild.getTargetBank().isPresent() && !enqueuedVertices.contains(dfsChild)) {
                    queue.add(dfsChild);
                    enqueuedVertices.add(dfsChild);
                }
            }
        }
    }

    private void determineBestAllocationInVertex(
                BComponentsPartitionContext context,
                FunctionVertex vertex,
                FunctionVertex treeRoot,
                int allFunctionsSize,
                double allFunctionsFrequency
    ) {
        final AllocationDirection vertexMembership = determineVertexMembership(vertex);
        final int downTreeFirstVertexNumber = vertexMembership == AllocationDirection.DOWN_TREE
                ? vertex.getDfsTreeNumber()
                : (vertex.getDfsTreeNumber() + 1);
        final int downTreeFunctionsSize = context.functionsSizesTree.compute(downTreeFirstVertexNumber,
                vertex.getMaximumDfsDescendantNumber() + 1);
        final double downTreeFunctionsFrequency = context.frequencyEstimationsTree.compute(downTreeFirstVertexNumber,
                vertex.getMaximumDfsDescendantNumber() + 1);
        final int upTreeFunctionsSize = allFunctionsSize - downTreeFunctionsSize;
        final double upTreeFunctionsFrequency = allFunctionsFrequency - downTreeFunctionsFrequency;

        final BestAllocationChoiceData choiceData = new BestAllocationChoiceData(vertex,
                treeRoot, downTreeFunctionsSize, downTreeFunctionsFrequency,
                upTreeFunctionsSize, upTreeFunctionsFrequency);
        for (AllocationDirection direction : AllocationDirection.values()) {
            determineBestDirectedAllocationInVertex(context, choiceData, direction,
                    vertexMembership);
        }
    }

    private void determineBestDirectedAllocationInVertex(BComponentsPartitionContext context,
                BestAllocationChoiceData choiceData, AllocationDirection direction,
                AllocationDirection vertexMembership) {
        if (direction == AllocationDirection.UP_TREE && choiceData.vertex == choiceData.treeRoot) {
            return;
        }

        final int functionsSize = choiceData.getFunctionsSize(direction);
        final double functionsFrequency = choiceData.getFunctionsFrequency(direction);

        final Iterable<String> banksToConsider = targetBankPickerAsp.isPresent()
                ? Optional.presentInstances(Collections.singleton(
                    targetBankPickerAsp.get().pick(context, functionsSize)))
                : bankSchema.getBanksNames();

        for (String bankName : banksToConsider) {
            // Check if there is enough place in the bank
            if (functionsSize > context.getFreeSpace(bankName)) {
                continue;
            }

            // Compute subtree vertices lazily
            if (!choiceData.subtreeVertices.isPresent()) {
                choiceData.subtreeVertices = Optional.of(collectSubtreeVertices(choiceData.vertex));
            }

            final ImmutableSet<FunctionVertex> nonBankedVertices = determineNonBankedFunctionsAfterAllocation(
                    context, bankName, choiceData.subtreeVertices.get());
            final double outerEdgesWeightsSum = computeOuterEdgesWeightsSum(choiceData.subtreeVertices.get(),
                    nonBankedVertices);

            aspPicker.add(new ExtendedTreeAllocation(choiceData.vertex,
                    choiceData.treeRoot, direction, direction == vertexMembership,
                    bankName, functionsSize, functionsFrequency, outerEdgesWeightsSum));
        }
    }

    private ImmutableSet<FunctionVertex> determineNonBankedFunctionsAfterAllocation(
                BComponentsPartitionContext context,
                String targetBankName,
                ImmutableSet<FunctionVertex> subtreeVertices
    ) {
        final ImmutableSet.Builder<FunctionVertex> nonBankedFunsBuilder = ImmutableSet.builder();
        final FluentIterable<FunctionVertex> bankVertices = FluentIterable
                .from(context.getBankTable().getBankContents(targetBankName))
                .transform(context.functionVertexFinder);
        final FluentIterable<FunctionVertex> consideredVertices = FluentIterable.from(subtreeVertices)
                .append(bankVertices);

        if (!targetBankName.equals(bankSchema.getCommonBankName())) {
            for (FunctionVertex vertex : consideredVertices) {
                boolean nonBanked = true;
                for (CallEdge predecessorEdge : vertex.getPredecessors()) {
                    final FunctionVertex callerVertex = predecessorEdge.getSourceVertex();
                    if (!subtreeVertices.contains(callerVertex)
                            && (!callerVertex.getTargetBank().isPresent()
                                || !callerVertex.getTargetBank().get().equals(targetBankName))) {
                        nonBanked = false;
                        break;
                    }
                }
                if (nonBanked) {
                    nonBankedFunsBuilder.add(vertex);
                }
            }
        } else {
            nonBankedFunsBuilder.addAll(consideredVertices);
        }

        return nonBankedFunsBuilder.build();
    }

    private ImmutableSet<FunctionVertex> collectSubtreeVertices(FunctionVertex vertex) {
        final ImmutableSet.Builder<FunctionVertex> subtreeVerticesBuilder = ImmutableSet.builder();

        final Set<FunctionVertex> enqueuedVertices = new HashSet<>();
        final Queue<FunctionVertex> queue = new ArrayDeque<>();
        queue.add(vertex);
        enqueuedVertices.addAll(queue);

        while (!queue.isEmpty()) {
            final FunctionVertex currentVertex = queue.remove();
            subtreeVerticesBuilder.add(currentVertex);

            for (FunctionVertex dfsChild : currentVertex.getDfsTreeChildren()) {
                if (!dfsChild.getTargetBank().isPresent() && !enqueuedVertices.contains(dfsChild)) {
                    queue.add(dfsChild);
                    enqueuedVertices.add(dfsChild);
                }
            }
        }

        return subtreeVerticesBuilder.build();
    }

    private double computeOuterEdgesWeightsSum(
                ImmutableSet<FunctionVertex> subtreeVertices,
                ImmutableSet<FunctionVertex> nonBankedVertices
    ) {
        double outerEdgesWeightsSum = 0.;

        for (FunctionVertex subtreeVertex : subtreeVertices) {
            final FluentIterable<CallEdge> neighbourEdges = FluentIterable.from(subtreeVertex.getPredecessors())
                    .append(subtreeVertex.getSuccessors());

            for (CallEdge edge : neighbourEdges) {
                final FunctionVertex callee = edge.getTargetVertex();
                if ((!callee.getTargetBank().isPresent() || !callee.getTargetBank().get().equals(bankSchema.getCommonBankName()))
                        && !nonBankedVertices.contains(callee)) {
                    outerEdgesWeightsSum += edge.getFrequencyEstimation();
                }
            }
        }

        return outerEdgesWeightsSum;
    }

    private AllocationDirection determineVertexMembership(FunctionVertex vertex) {
        double weightsSumUpTree = 0.0, weightsSumDownTree = 0.0;

        final FluentIterable<CallEdge> neighboursIterable = FluentIterable.from(vertex.getSuccessors())
                .append(vertex.getPredecessors());

        for (CallEdge neighbourEdge : neighboursIterable) {
            if (neighbourEdge.getTargetVertex() == vertex
                    && neighbourEdge.getSourceVertex() == vertex) {
                continue;
            } else if (neighbourEdge.getTargetVertex() != vertex
                    && neighbourEdge.getSourceVertex() != vertex) {
                throw new RuntimeException("edge of a vertex not incident with it");
            }

            final FunctionVertex neighbour = neighbourEdge.getTargetVertex() == vertex
                    ? neighbourEdge.getSourceVertex()
                    : neighbourEdge.getTargetVertex();
            if (neighbour.getTargetBank().isPresent()) {
                continue;
            }

            if (vertex.getDfsTreeNumber() < neighbour.getDfsTreeNumber()
                    && neighbour.getDfsTreeNumber() <= vertex.getMaximumDfsDescendantNumber()) {
                weightsSumDownTree += neighbourEdge.getFrequencyEstimation();
            } else {
                weightsSumUpTree += neighbourEdge.getFrequencyEstimation();
            }
        }

        return weightsSumUpTree > weightsSumDownTree
                ? AllocationDirection.UP_TREE
                : AllocationDirection.DOWN_TREE;
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
         * Interval tree that allows for computing sums of frequency
         * estimations.
         */
        private final IntervalTree<Double> frequencyEstimationsTree;

        /**
         * Map with functions that will be partitioned.
         */
        private final ImmutableMap<String, FunctionDecl> functions;

        /**
         * Function that finds the function vertex corresponding to a function
         * definition AST node in the call graph in this context.
         */
        private final FunctionVertexFinder functionVertexFinder;

        private BComponentsPartitionContext(Iterable<FunctionDecl> functions,
                    Map<String, Range<Integer>> functionsSizes, ReferencesGraph refsGraph) {
            super(bankSchema, functionsSizes);
            this.callGraph = new CallGraph(functions, refsGraph);
            this.functionsSizesTree = new IntervalTree<>(Integer.class,
                    new IntegerSumIntervalTreeOperation(), functionsSizes.size());
            this.frequencyEstimationsTree = new IntervalTree<>(Double.class,
                    new DoubleSumIntervalTreeOperation(), functionsSizes.size());
            this.functionVertexFinder = new FunctionVertexFinder(this.callGraph);

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
                frequencyEstimationsTree.set(vertex.getDfsTreeNumber(), 0.);
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
         * Removes the information about the current DFS tree number allowing
         * to set it again.
         *
         * @throws IllegalStateException The DFS tree number has not been set yet.
         */
        private void resetDfsTreeNumber() {
            checkState(dfsTreeNumber.isPresent(), "DFS tree number has not been set yet");
            this.dfsTreeNumber = Optional.absent();
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
         * Remove the current information about the parent of this vertex in the
         * DFS tree ensuring that it can be set, possibly again.
         */
        private void resetDfsTreeParent() {
            this.dfsTreeParent = Optional.absent();
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
         * Remove the information about the maximum DFS descendant number of
         * this vertex allowing to set it again.
         *
         * @throws IllegalStateException Maximum DFS descendant number of this
         *                               vertex has not been set yet.
         */
        private void resetMaximumDfsDescendantNumber() {
            checkState(maximumDfsDescendantNumber.isPresent(),
                    "maximum DFS descendant number has not been set yet");
            this.maximumDfsDescendantNumber = Optional.absent();
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
     * Neighbours provider that provides vertices from the map specified at
     * construction-time. If the map does not contain an entry for a vertex,
     * then this provider returns an empty iterable.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class SpecificNeighboursProvider implements NeighboursProvider {
        private final Map<FunctionVertex, ? extends Iterable<FunctionVertex>> neighbours;

        private SpecificNeighboursProvider(Map<FunctionVertex, ? extends Iterable<FunctionVertex>> neighbours) {
            checkNotNull(neighbours, "neighbours cannot be null");
            this.neighbours = neighbours;
        }

        @Override
        public Iterable<FunctionVertex> getNeighbours(FunctionVertex vertex) {
            checkNotNull(vertex, "vertex cannot be null");
            final Optional<Iterable<FunctionVertex>> neighboursIterable =
                    Optional.fromNullable(neighbours.get(vertex));
            return neighboursIterable.isPresent()
                    ? neighboursIterable.get()
                    : Collections.<FunctionVertex>emptyList();
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
        private final TargetBankPicker targetBankPicker;
        private Optional<String> currentBank;
        private boolean failure;
        private int allocatedFunctionsCount;

        private AllocatingDfsVertexVisitor(BComponentsPartitionContext context,
                    TargetBankPicker targetBankPicker) {
            checkNotNull(context, "context cannot be null");
            checkNotNull(targetBankPicker, "target bank picker cannot be null");
            this.context = context;
            this.targetBankPicker = targetBankPicker;
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
                currentBank = targetBankPicker.pick(context, functionSize);
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
     * Vertex visitor that builds a spanning forest corresponding to the DFS
     * search, numbers vertices in a pre-order numbering and computes maximum
     * DFS descendant numbers for vertices.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class SpanningForestVertexVisitor implements DfsVertexVisitor {
        private int nextDfsNumber = 0;

        @Override
        public void enterVertex(FunctionVertex funVertex, Optional<FunctionVertex> parent) {
            funVertex.setDfsTreeNumber(nextDfsNumber++);
            if (parent.isPresent()) {
                funVertex.setDfsTreeParent(parent.get());
            }
        }

        @Override
        public void leaveVertex(FunctionVertex funVertex, Optional<FunctionVertex> parent) {
            funVertex.setMaximumDfsDescendantNumber(funVertex.getDfsTreeNumber());
            for (FunctionVertex child : funVertex.getDfsTreeChildren()) {
                funVertex.updateMaximumDfsDescendantNumber(child.getMaximumDfsDescendantNumber());
            }
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
    private static class TreeAllocation {
        private final FunctionVertex vertex;
        private final FunctionVertex dfsTreeRoot;
        private final AllocationDirection direction;
        private final boolean includesVertex;
        private final String bankName;
        private final int totalFunctionsSize;
        private final double frequencyEstimationsSum;

        private TreeAllocation(FunctionVertex vertex, FunctionVertex dfsTreeRoot,
                    AllocationDirection direction, boolean includesVertex, String bankName,
                    int totalFunctionsSize, double frequencyEstimationsSum) {
            checkNotNull(vertex, "vertex cannot be null");
            checkNotNull(dfsTreeRoot, "DFS tree root cannot be null");
            checkNotNull(direction, "direction cannot be null");
            checkNotNull(bankName, "name of the bank cannot be null");
            checkArgument(!bankName.isEmpty(), "name of the bank cannot be an empty string");
            checkArgument(totalFunctionsSize >= 0, "total size of allocated functions cannot be negative");
            checkArgument(frequencyEstimationsSum >= 0., "frequency estimations sum cannot be negative");
            this.vertex = vertex;
            this.dfsTreeRoot = dfsTreeRoot;
            this.direction = direction;
            this.includesVertex = includesVertex;
            this.bankName = bankName;
            this.totalFunctionsSize = totalFunctionsSize;
            this.frequencyEstimationsSum = frequencyEstimationsSum;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper("TreeAllocation")
                    .add("vertex", vertex.getUniqueName())
                    .add("dfs-tree-root", dfsTreeRoot.getUniqueName())
                    .add("direction", direction)
                    .add("includes-vertex", includesVertex)
                    .add("target-bank", bankName)
                    .add("total-functions-size", totalFunctionsSize)
                    .add("frequency-estimations-sum", frequencyEstimationsSum)
                    .toString();
        }
    }

    /**
     * Object that represents an allocation used in the extended subtree
     * partitioning.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class ExtendedTreeAllocation extends TreeAllocation {
        private final double outerEdgesWeightsSum;

        private ExtendedTreeAllocation(FunctionVertex vertex, FunctionVertex dfsTreeRoot,
                AllocationDirection direction, boolean includesVertex, String bankName,
                int totalFunctionsSize, double frequencyEstimationsSum,
                double outerEdgesWeightsSum) {
            super(vertex, dfsTreeRoot, direction, includesVertex, bankName,
                    totalFunctionsSize, frequencyEstimationsSum);
            checkArgument(outerEdgesWeightsSum >= 0., "outer edges weights sum cannot be negative");
            this.outerEdgesWeightsSum = outerEdgesWeightsSum;
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
        private final boolean preferHigherEstimateAllocations;

        private TreeAllocationComparator(String commonBankName, boolean preferHigherEstimateAllocations) {
            checkNotNull(commonBankName, "name of the common bank cannot be null");
            checkArgument(!commonBankName.isEmpty(), "name of the common bank cannot be an empty string");
            this.commonBankName = commonBankName;
            this.preferHigherEstimateAllocations = preferHigherEstimateAllocations;
        }

        @Override
        public int compare(TreeAllocation allocation1, TreeAllocation allocation2) {
            checkNotNull(allocation1, "first tree allocation cannot be null");
            checkNotNull(allocation2, "second tree allocation cannot be null");

            if (preferHigherEstimateAllocations) {
                // Compare total frequency estimations
                final int frequencyEstimationsResult = Double.compare(allocation1.frequencyEstimationsSum,
                        allocation2.frequencyEstimationsSum);
                if (frequencyEstimationsResult != 0) {
                    return frequencyEstimationsResult;
                }
            }

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
     * Comparator for determining better extended tree allocations. A greater
     * allocation is the preferred allocation.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class ExtendedTreeAllocationComparator implements Comparator<ExtendedTreeAllocation> {
        private final Comparator<TreeAllocation> innerComparator;

        private ExtendedTreeAllocationComparator(Comparator<TreeAllocation> innerComparator) {
            checkNotNull(innerComparator, "inner comparator cannot be null");
            this.innerComparator = innerComparator;
        }

        @Override
        public int compare(ExtendedTreeAllocation allocation1, ExtendedTreeAllocation allocation2) {
            checkNotNull(allocation1, "the first allocation cannot be null");
            checkNotNull(allocation2, "the second allocation cannot be null");

            final int weightsSumResult = Double.compare(allocation2.outerEdgesWeightsSum,
                    allocation1.outerEdgesWeightsSum);
            return weightsSumResult != 0
                    ? weightsSumResult
                    : innerComparator.compare(allocation1, allocation2);
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

    /**
     * A comparator that specifies an order on edges in the call graph. It is
     * the ascending order of their frequency estimations. If the frequency
     * estimations are equal, then the smaller names of vertices incident with
     * the edges are compared. If they are equal too, then the greater names
     * are compared.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class FrequencyEstimationEdgeComparator implements Comparator<CallEdge> {
        @Override
        public int compare(CallEdge edge1, CallEdge edge2) {
            checkNotNull(edge1, "the first edge cannot be null");
            checkNotNull(edge2, "the second edge cannot be null");

            // Compare the frequency estimations
            final int frequencyEstimationResult = Double.compare(edge1.getFrequencyEstimation(),
                    edge2.getFrequencyEstimation());
            if (frequencyEstimationResult != 0) {
                return frequencyEstimationResult;
            }

            // Determine names of connected functions
            final String[] uniqueNames1 = getSortedIncidentVertices(edge1);
            final String[] uniqueNames2 = getSortedIncidentVertices(edge2);

            // Compare smaller names of incident functions
            final int smallerNamesResult = uniqueNames1[0].compareTo(uniqueNames2[0]);
            if (smallerNamesResult != 0) {
                return smallerNamesResult;
            }

            // Compare greater names of functions
            return uniqueNames1[1].compareTo(uniqueNames2[1]);
        }

        private String[] getSortedIncidentVertices(CallEdge edge) {
            final String[] sortedNames = new String[2];

            if (edge.getSourceVertex().getUniqueName().compareTo(edge.getTargetVertex().getUniqueName()) < 0) {
                sortedNames[0] = edge.getSourceVertex().getUniqueName();
                sortedNames[1] = edge.getTargetVertex().getUniqueName();
            } else {
                sortedNames[0] = edge.getTargetVertex().getUniqueName();
                sortedNames[1] = edge.getSourceVertex().getUniqueName();
            }

            return sortedNames;
        }
    }

    /**
     * Function transforming a function definition AST node to the corresponding
     * function vertex in a call graph given at construction.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class FunctionVertexFinder implements Function<FunctionDecl, FunctionVertex> {
        private final CallGraph callGraph;

        private FunctionVertexFinder(CallGraph callGraph) {
            checkNotNull(callGraph, "call graph cannot be null");
            this.callGraph = callGraph;
        }

        @Override
        public FunctionVertex apply(FunctionDecl functionDecl) {
            checkNotNull(functionDecl, "function definition AST node cannot be null");
            final String funUniqueName = DeclaratorUtils.getUniqueName(
                    functionDecl.getDeclarator()).get();
            final Optional<FunctionVertex> functionVertex = Optional.fromNullable(
                    callGraph.getVertices().get(funUniqueName));
            if (!functionVertex.isPresent()) {
                throw new IllegalArgumentException("function '" + funUniqueName
                        + "' absent in the call graph of this function finder");
            }
            return functionVertex.get();
        }
    }

    /**
     * An enum type representing the kind of the spanning forest that will be
     * used for the partitioning.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    public enum SpanningForestKind {
        /**
         * A minimum spanning forest will be computed for the call graph and
         * used for the partitioning.
         */
        MINIMUM,

        /**
         * A maximum spanning forest will be computed for the call graph and
         * used for the partitioning.
         */
        MAXIMUM,

        /**
         * The spanning forest built during the biconnected components
         * computation will be used.
         */
        BCOMPONENTS,
    }

    /**
     * An enum type representing the mode of the arbitrary subtree partitioning.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    public enum ArbitrarySubtreePartitioningMode {
        /**
         * Arbitrary subtree partitioning will be used to find the best
         * allocation instead of partitioning in cut vertices.
         */
        ALWAYS,

        /**
         * Arbitrary subtree partitioning will be used to find a feasible
         * allocation if and only if partitioning in cut vertices fails.
         */
        ON_EMERGENCY,

        /**
         * Arbitrary subtree partitioning will be never used to find an
         * allocation. If partitioning in cut vertices fails, the DFS algorithm
         * is used.
         */
        NEVER,
    }

    /**
     * Factory for creating common bank fillers.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class CommonBankFillerFactory {
        /**
         * Create a new instance of a common bank filler realizing the given
         * kind of algorithm.
         *
         * @param algorithm Algorithm that the created common bank filler will
         *                  follow.
         * @return Newly created instance of a common bank filler following
         *         the given algorithm.
         */
        private CommonBankFiller create(CommonBankAllocationAlgorithm algorithm) {
            checkNotNull(algorithm, "algorithm cannot be null");

            switch (algorithm) {
                case GREEDY_DESCENDING_ESTIMATIONS:
                    return new DescendingEstimationsFiller();
                case TWO_APPROXIMATION:
                    return new TwoApproximationFiller();
                case NO_OPERATION:
                    return new NoOperationFiller();
                default:
                    throw new RuntimeException("unexpected common bank allocation algorithm "
                            + algorithm);
            }
        }
    }

    /**
     * Interface for performing the first step of the biconnected components
     * heuristic, i.e. allocation to the common bank.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private interface CommonBankFiller {
        /**
         * Assign unallocated functions in the given context to the common bank.
         * It must be performed with a specific algorithm.
         *
         * @param context Partitioning context.
         * @throws NullPointerException The context is null.
         */
        void fillCommonBank(BComponentsPartitionContext context);
    }

    /**
     * Common bank filler realizing the algorithm represented by
     * {@link CommonBankAllocationAlgorithm#GREEDY_DESCENDING_ESTIMATIONS}.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class DescendingEstimationsFiller implements CommonBankFiller {
        @Override
        public void fillCommonBank(BComponentsPartitionContext context) {
            checkNotNull(context, "context cannot be null");

            final List<FunctionVertex> allVertices = new ArrayList<>(
                    context.callGraph.getVertices().values());
            Collections.sort(allVertices, new FunctionFrequencyComparator());

            for (FunctionVertex vertex : allVertices) {
                if (!vertex.getTargetBank().isPresent()
                        && context.fitsInCommonBank(vertex.getUniqueName())) {
                    context.assignToCommonBank(context.functions.get(vertex.getUniqueName()));
                }
            }
        }
    }

    /**
     * A common bank filler implementing the algorithm represented by
     * {@link CommonBankAllocationAlgorithm#TWO_APPROXIMATION}.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class TwoApproximationFiller implements CommonBankFiller {
        @Override
        public void fillCommonBank(BComponentsPartitionContext context) {
            checkNotNull(context, "context cannot be null");
            final ImmutableList<FunctionVertex> sortedFunctions = sortFunctions(context);
            final FunctionListPrefix longestFittingPrefix = determineLongestFittingPrefix(
                    context, sortedFunctions);
            allocateToCommonBank(context, sortedFunctions, longestFittingPrefix);
            allocateRemainingFunctions(context, sortedFunctions);
        }

        private ImmutableList<FunctionVertex> sortFunctions(BComponentsPartitionContext context) {
            final int commonBankFreeSpace = context.getFreeSpaceInCommonBank();

            /* Create a list with all unallocated functions that fit in the
               common bank. */
            final List<FunctionVertex> unallocatedFunctions = new ArrayList<>();
            for (FunctionVertex vertex : context.callGraph.getVertices().values()) {
                final int functionSize = context.getFunctionSize(vertex.getUniqueName());

                if (!vertex.getTargetBank().isPresent() && functionSize <= commonBankFreeSpace) {
                    unallocatedFunctions.add(vertex);
                }
            }

            /* Sort the functions in the descending order with the respect to
               their ratios. */
            Collections.sort(unallocatedFunctions, new FrequencyEstimationToSizeRatioComparator(context));

            return ImmutableList.copyOf(unallocatedFunctions);
        }

        private FunctionListPrefix determineLongestFittingPrefix(BComponentsPartitionContext context,
                    ImmutableList<FunctionVertex> functions) {
            final int commonBankFreeSpace = context.getFreeSpaceInCommonBank();

            int prefixSize = 0;
            int totalFunctionsSize = 0;
            double frequencyEstimationsSum = 0.;

            for (FunctionVertex vertex : functions) {
                final int functionSize = context.getFunctionSize(vertex.getUniqueName());

                if (totalFunctionsSize + functionSize <= commonBankFreeSpace) {
                    ++prefixSize;
                    totalFunctionsSize += functionSize;
                    frequencyEstimationsSum += vertex.getFrequencyEstimation();
                } else {
                    break;
                }
            }

            return new FunctionListPrefix(prefixSize, frequencyEstimationsSum);
        }

        private void allocateToCommonBank(
                    BComponentsPartitionContext context,
                    ImmutableList<FunctionVertex> functions,
                    FunctionListPrefix longestFittingPrefix
        ) {
            if (functions.isEmpty()) {
                return;
            }

            /* Choose the better solution: the prefix or the element directly
               after it. It guarantees the approximation factor. */
            if (longestFittingPrefix.size >= functions.size()
                    || longestFittingPrefix.frequencyEstimationsSum
                        >= functions.get(longestFittingPrefix.size).getFrequencyEstimation()) {
                // Allocate functions from the prefix
                for (int i = 0; i < longestFittingPrefix.size; ++i) {
                    context.assignToCommonBank(context.functions.get(functions.get(i).getUniqueName()));
                }
            } else {
                // Allocate the function directly following the prefix
                context.assignToCommonBank(context.functions.get(functions.get(
                        longestFittingPrefix.size).getUniqueName()));
            }
        }

        private void allocateRemainingFunctions(BComponentsPartitionContext context,
                    ImmutableList<FunctionVertex> functions) {
            for (FunctionVertex functionVertex : functions) {
                if (!functionVertex.getTargetBank().isPresent()
                        && context.fitsInCommonBank(functionVertex.getUniqueName())) {
                    context.assignToCommonBank(context.functions.get(functionVertex.getUniqueName()));
                }
            }
        }

        /**
         * Comparator that realizes the order of function vertices defined as
         * follows. A function vertex v is greater than a function vertex u if
         * and only if the ratio of the frequency estimation of v to the size of
         * v is less than the same ratio for u or the ratios for v and u are
         * equal and the unique name of function v is less than the unique name
         * for function u.
         *
         * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
         */
        private static final class FrequencyEstimationToSizeRatioComparator implements Comparator<FunctionVertex> {
            private final BComponentsPartitionContext context;

            private FrequencyEstimationToSizeRatioComparator(BComponentsPartitionContext context) {
                checkNotNull(context, "context cannot be null");
                this.context = context;
            }

            @Override
            public int compare(FunctionVertex vertex1, FunctionVertex vertex2) {
                checkNotNull(vertex1, "first vertex cannot be null");
                checkNotNull(vertex2, "second vertex cannot be null");

                final int resultRatio = Double.compare(computeRatio(vertex2),
                        computeRatio(vertex1));
                return resultRatio != 0
                        ? resultRatio
                        : vertex2.getUniqueName().compareTo(vertex1.getUniqueName());
            }

            private double computeRatio(FunctionVertex vertex) {
                return vertex.getFrequencyEstimation()
                        / (double) context.getFunctionSize(vertex.getUniqueName());
            }
        }

        /**
         * A helper object that carries information about a prefix of a list of
         * functions. The exact list of functions an object corresponds to is
         * determined from the context.
         *
         * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
         */
        private static final class FunctionListPrefix {
            /**
             * Size of the prefix (the number of functions it consists of).
             */
            private final int size;

            /**
             * Sum of frequency estimations of functions from the prefix.
             */
            private final double frequencyEstimationsSum;

            private FunctionListPrefix(int size, double frequencyEstimationsSum) {
                checkArgument(size >= 0, "size cannot be negative");
                checkArgument(frequencyEstimationsSum >= 0., "frequency estimations sum cannot be negative");
                this.size = size;
                this.frequencyEstimationsSum = frequencyEstimationsSum;
            }
        }
    }

    /**
     * A common bank filler implementing the algorithm represented by
     * {@link CommonBankAllocationAlgorithm#NO_OPERATION}.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class NoOperationFiller implements CommonBankFiller {
        @Override
        public void fillCommonBank(BComponentsPartitionContext context) {
            checkNotNull(context, "context cannot be null");
            // do nothing because this filler does not allocate any functions
        }
    }

    /**
     * A method of choosing the target bank for an allocation if all of the
     * functions from the allocation can be simultaneously allocated to more
     * than one bank without overflowing it.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    public enum TargetBankChoiceMethod {
        /**
         * All of the functions from the allocation are assigned to a bank
         * they fit in with the smallest amount of free space.
         */
        FLOOR_BANK,

        /**
         * All of the functions from the allocation are assigned to a bank
         * they fit in with the greatest amount of free space.
         */
        CEILING_BANK,
    }

    /**
     * A factory for target bank pickers.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class TargetBankPickerFactory {
        private final Function<TargetBankChoiceMethod, TargetBankPicker> factoryFunction =
                    new FactoryFunction();

        /**
         * Get a new instance of a target bank picker.
         *
         * @param method Method to use for choosing the target bank by the
         *               created picker.
         * @return Newly created instance of a target bank picker that uses the
         *         given method for selecting the target bank.
         * @throws NullPointerException The method is null.
         */
        private TargetBankPicker create(TargetBankChoiceMethod method) {
            checkNotNull(method, "method cannot be null");

            switch (method) {
                case FLOOR_BANK:
                    return new FloorTargetBankPicker();
                case CEILING_BANK:
                    return new CeilingTargetBankPicker();
                default:
                    throw new RuntimeException("unexpected target bank choice method " + method);
            }
        }

        /**
         * Get a function that creates target bank pickers. The returned
         * function simply calls method {@link TargetBankPickerFactory#create}.
         *
         * @return A function that creates target bank pickers.
         */
        private Function<TargetBankChoiceMethod, TargetBankPicker> getFunction() {
            return factoryFunction;
        }

        private final class FactoryFunction implements Function<TargetBankChoiceMethod, TargetBankPicker> {
            @Override
            public TargetBankPicker apply(TargetBankChoiceMethod method) {
                return create(method);
            }
        }
    }

    /**
     * Interface for selecting a bank for an allocation.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private interface TargetBankPicker {
        /**
         * Pick a bank that has at least the given amount of free space in the
         * current allocation (from the given context). Each implementing class
         * should use a specific strategy.
         *
         * @param context Context with the current allocation.
         * @param minimumFreeSpace Minimum amount of free space in the bank.
         * @return Name of a bank selected according to the strategy implemented
         *         by this picker amongst banks with at least the given amount
         *         of free space. The object is absent if and only if all banks
         *         have less free space than the given amount.
         * @throws NullPointerException Context is null.
         * @throws IllegalArgumentException The given minimum amount of free
         *                                  space is negative.
         */
        Optional<String> pick(BComponentsPartitionContext context, int minimumFreeSpace);
    }

    /**
     * A target bank picker choosing the bank with the smallest amount of free
     * space amongst all banks with at least the given amount of free space.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class FloorTargetBankPicker implements TargetBankPicker {
        @Override
        public Optional<String> pick(BComponentsPartitionContext context, int minimumFreeSpace) {
            checkNotNull(context, "context cannot be null");
            checkArgument(minimumFreeSpace >= 0, "minimum free space cannot be negative");
            return context.getFloorBank(minimumFreeSpace);
        }
    }

    /**
     * A target bank picker choosing the bank with the greatest amount of free
     * space amongst all banks with at least the given amount of free space.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class CeilingTargetBankPicker implements TargetBankPicker {
        @Override
        public Optional<String> pick(BComponentsPartitionContext context, int minimumFreeSpace) {
            checkNotNull(context, "context cannot be null");
            checkArgument(minimumFreeSpace >= 0, "minimum free space cannot be negative");
            return context.getCeilingBank(minimumFreeSpace);
        }
    }

    /**
     * Object holding information for choosing the best allocation in a single
     * vertex.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class BestAllocationChoiceData {
        private final FunctionVertex vertex;
        private final FunctionVertex treeRoot;
        private final ImmutableMap<AllocationDirection, Integer> sizes;
        private final ImmutableMap<AllocationDirection, Double> frequencies;
        private Optional<ImmutableSet<FunctionVertex>> subtreeVertices;

        private BestAllocationChoiceData(FunctionVertex vertex, FunctionVertex treeRoot,
                    int downTreeFunctionsSize, double downTreeFunctionsFrequency,
                    int upTreeFunctionsSize, double upTreeFunctionsFrequency) {
            checkNotNull(vertex, "vertex cannot be null");
            checkNotNull(treeRoot, "tree root cannot be null");
            checkArgument(downTreeFunctionsSize >= 0, "down tree functions size cannot be negative");
            checkArgument(downTreeFunctionsFrequency >= 0., "down tree functions frequency cannot be negative");
            checkArgument(upTreeFunctionsSize >= 0, "up tree functions size cannot be negative");
            checkArgument(upTreeFunctionsFrequency >= 0., "up tree functions frequency cannot be negative");
            this.vertex = vertex;
            this.treeRoot = treeRoot;
            this.subtreeVertices = Optional.absent();

            // Build the sizes map
            final EnumMap<AllocationDirection, Integer> sizesMap = new EnumMap<>(AllocationDirection.class);
            sizesMap.put(AllocationDirection.DOWN_TREE, downTreeFunctionsSize);
            sizesMap.put(AllocationDirection.UP_TREE, upTreeFunctionsSize);
            this.sizes = Maps.immutableEnumMap(sizesMap);

            // Build the frequencies map
            final EnumMap<AllocationDirection, Double> frequenciesMap = new EnumMap<>(AllocationDirection.class);
            frequenciesMap.put(AllocationDirection.DOWN_TREE, downTreeFunctionsFrequency);
            frequenciesMap.put(AllocationDirection.UP_TREE, upTreeFunctionsFrequency);
            this.frequencies = Maps.immutableEnumMap(frequenciesMap);
        }

        private int getFunctionsSize(AllocationDirection direction) {
            return getFromMap(sizes, direction);
        }

        private double getFunctionsFrequency(AllocationDirection direction) {
            return getFromMap(frequencies, direction);
        }

        private <V> V getFromMap(Map<AllocationDirection, V> map, AllocationDirection direction) {
            checkNotNull(direction, "direction cannot be null");
            final Optional<V> result = Optional.fromNullable(map.get(direction));
            if (!result.isPresent()) {
                throw new RuntimeException("unexpected allocation direction " + direction);
            }
            return result.get();
        }
    }

    /**
     * Factory for allocation pickers. It produces objects from their textual
     * description.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class AllocationPickerFactory {
        private final BankSchema bankSchema;
        private final boolean preferHigherEstimateAllocations;

        private AllocationPickerFactory(BankSchema bankSchema, boolean preferHigherEstimateAllocations) {
            checkNotNull(bankSchema, "bank schema cannot be null");
            this.bankSchema = bankSchema;
            this.preferHigherEstimateAllocations = preferHigherEstimateAllocations;
        }

        private AllocationPicker create(String description) {
            checkNotNull(description, "description cannot be null");
            checkArgument(!description.isEmpty(), "description cannot be an empty string");

            if (PATTERN_ASP_ALLOCATION_PICKER_SMALLEST_WEIGHTS.matcher(description).matches()) {
                return new BestAllocationPicker(new ExtendedTreeAllocationComparator(
                        new TreeAllocationComparator(bankSchema.getCommonBankName(),
                                preferHigherEstimateAllocations)));
            }

            final Matcher smallestWeightsFromBiggestSizeMatcher =
                    PATTERN_ASP_ALLOCATION_PICKER_SMALLEST_WEIGHTS_FROM_BIGGEST_SIZE.matcher(
                            description);
            if (smallestWeightsFromBiggestSizeMatcher.matches()) {
                return new SmallestWeightsFromBiggestSizePicker(parseAllocationsCount(
                        smallestWeightsFromBiggestSizeMatcher, description));
            }

            final Matcher biggestSizeFromSmallestWeightsMatcher =
                    PATTERN_ASP_ALLOCATION_PICKER_BIGGEST_SIZE_FROM_SMALLEST_WEIGHTS.matcher(
                            description);
            if (biggestSizeFromSmallestWeightsMatcher.matches()) {
                return new BiggestSizeFromSmallestWeightsPicker(parseAllocationsCount(
                        biggestSizeFromSmallestWeightsMatcher, description));
            }

            throw new IllegalArgumentException("invalid picker description '" + description + "'");
        }

        private int parseAllocationsCount(Matcher matcher, String pickerDescription) {
            final String allocationsCount = matcher.group("size");

            try {
                return Integer.parseInt(allocationsCount);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid picker description '"
                        + pickerDescription + "'", e);
            }
        }
    }

    /**
     * Interface for choosing a candidate allocation.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private interface AllocationPicker {
        /**
         * Add the given candidate allocation to consider for picking an
         * allocation that will be actually made.
         *
         * @param allocation Allocation to add.
         * @throws NullPointerException Allocation is null.
         */
        void add(ExtendedTreeAllocation allocation);

        /**
         * Get the best allocation amongst all candidate allocation added to
         * this picker.
         *
         * @return The best allocation added to this picker. The object is
         *         absent if no allocation has been added to this picker.
         */
        Optional<ExtendedTreeAllocation> pick();

        /**
         * Remove all candidate allocations stored in this picker (as if method
         * {@link AllocationPicker#add} was never called).
         */
        void clear();
    }

    /**
     * Allocation picker that stores a specific number of allocations that are
     * best according to a given storing comparator (a better allocation is the
     * one that is greater according to the storing comparator). It picks the
     * greatest allocation according to a picking comparator amongst all stored
     * allocations.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static abstract class AbstractAllocationPicker implements AllocationPicker {
        private final List<ExtendedTreeAllocation> allocations;
        private final int maxAllocationsCount;
        private final Comparator<ExtendedTreeAllocation> storingComparator;
        private final Comparator<ExtendedTreeAllocation> pickingComparator;

        protected AbstractAllocationPicker(int allocationsToStoreCount,
                    Comparator<ExtendedTreeAllocation> storingComparator,
                    Comparator<ExtendedTreeAllocation> pickingComparator) {
            checkNotNull(storingComparator, "storing comparator cannot be null");
            checkNotNull(pickingComparator, "picking comparator cannot be null");
            checkArgument(allocationsToStoreCount > 0, "count of allocations to store must be positive");
            this.allocations = new ArrayList<>();
            this.maxAllocationsCount = allocationsToStoreCount;
            this.storingComparator = storingComparator;
            this.pickingComparator = pickingComparator;
        }

        @Override
        public final void add(ExtendedTreeAllocation allocation) {
            checkNotNull(allocation, "allocation cannot be null");

            if (allocations.size() < maxAllocationsCount) {
                allocations.add(allocation);
            } else {
                final ExtendedTreeAllocation worstAllocation = Collections.min(
                        allocations, storingComparator);

                if (storingComparator.compare(worstAllocation, allocation) < 0) {
                    final int worstAllocationIndex = allocations.indexOf(worstAllocation);
                    if (worstAllocationIndex < 0) {
                        throw new RuntimeException("cannot find the index of the worst allocation");
                    }
                    allocations.set(worstAllocationIndex, allocation);
                }
            }
        }

        @Override
        public final Optional<ExtendedTreeAllocation> pick() {
            return !allocations.isEmpty()
                    ? Optional.of(Collections.max(allocations, pickingComparator))
                    : Optional.<ExtendedTreeAllocation>absent();
        }

        @Override
        public final void clear() {
            allocations.clear();
        }
    }

    /**
     * Allocation picker that chooses the best allocation according to a given
     * comparator.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class BestAllocationPicker extends AbstractAllocationPicker {
        private BestAllocationPicker(Comparator<ExtendedTreeAllocation> comparator) {
            super(1, comparator, comparator);
        }
    }

    private static final class SmallestWeightsFromBiggestSizePicker extends AbstractAllocationPicker {
        private SmallestWeightsFromBiggestSizePicker(int allocationsCount) {
            super(allocationsCount, new AllocationSizesComparator(new TotalTreeAllocationComparator()),
                    new OuterEdgesWeightsComparator(new TotalTreeAllocationComparator()));
        }

    }

    private static final class BiggestSizeFromSmallestWeightsPicker extends AbstractAllocationPicker {
        private BiggestSizeFromSmallestWeightsPicker(int allocationsCount) {
            super(allocationsCount, new OuterEdgesWeightsComparator(new TotalTreeAllocationComparator()),
                    new AllocationSizesComparator(new TotalTreeAllocationComparator()));
        }
    }

    /**
     * Comparator that imposes a total ordering on tree allocations.
     *
     * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
     */
    private static final class TotalTreeAllocationComparator implements Comparator<TreeAllocation> {
        @Override
        public int compare(TreeAllocation allocation1, TreeAllocation allocation2) {
            checkNotNull(allocation1, "first allocation cannot be null");
            checkNotNull(allocation2, "second allocation cannot be null");

            // Compare by function unique name
            final int functionNameResult = allocation1.vertex.getUniqueName()
                    .compareTo(allocation2.vertex.getUniqueName());
            if (functionNameResult != 0) {
                return functionNameResult;
            }

            // Compare by direction
            if (allocation1.direction != allocation2.direction) {
                if (allocation1.direction == AllocationDirection.UP_TREE) {
                    return 1;
                } else if (allocation2.direction == AllocationDirection.UP_TREE) {
                    return -1;
                } else {
                    throw new RuntimeException("unexpectedly no allocation is up-tree");
                }
            }

            // Compare by name of the target bank
            return allocation1.bankName.compareTo(allocation2.bankName);
        }
    }

    private static final class AllocationSizesComparator implements Comparator<ExtendedTreeAllocation> {
        private final Comparator<TreeAllocation> innerComparator;

        private AllocationSizesComparator(Comparator<TreeAllocation> innerComparator) {
            checkNotNull(innerComparator, "inner comparator cannot be null");
            this.innerComparator = innerComparator;
        }

        @Override
        public int compare(ExtendedTreeAllocation extendedAllocation1,
                ExtendedTreeAllocation extendedAllocation2) {
            checkNotNull(extendedAllocation1, "first allocation cannot be null");
            checkNotNull(extendedAllocation2, "second allocation cannot be null");

            // Cast to get access to private members of allocations
            final TreeAllocation allocation1 = extendedAllocation1,
                    allocation2 = extendedAllocation2;

            // Compare by size
            final int sizeResult = Integer.compare(allocation1.totalFunctionsSize,
                    allocation2.totalFunctionsSize);
            if (sizeResult != 0) {
                return sizeResult;
            }

            return innerComparator.compare(extendedAllocation1, extendedAllocation2);
        }
    }

    private static final class OuterEdgesWeightsComparator implements Comparator<ExtendedTreeAllocation> {
        private final Comparator<TreeAllocation> innerComparator;

        private OuterEdgesWeightsComparator(Comparator<TreeAllocation> innerComparator) {
            checkNotNull(innerComparator, "inner comparator cannot be null");
            this.innerComparator = innerComparator;
        }

        @Override
        public int compare(ExtendedTreeAllocation allocation1, ExtendedTreeAllocation allocation2) {
            checkNotNull(allocation1, "first allocation cannot be null");
            checkNotNull(allocation2, "second allocation cannot be null");

            final int weightsSumResult = Double.compare(allocation2.outerEdgesWeightsSum,
                    allocation1.outerEdgesWeightsSum);
            if (weightsSumResult != 0) {
                return weightsSumResult;
            }

            return innerComparator.compare(allocation1, allocation2);
        }
    }
}
