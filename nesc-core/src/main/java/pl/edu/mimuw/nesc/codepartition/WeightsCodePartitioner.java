package pl.edu.mimuw.nesc.codepartition;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Range;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
 * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
 */
public class WeightsCodePartitioner implements CodePartitioner {
    /**
     * Bank schema assumed by this partitioner.
     */
    private final BankSchema bankSchema;

    /**
     * Common bank allocator used by this partitioner.
     */
    private final CommonBankAllocator commonBankAllocator;

    /**
     * Listener that will be notified about detected issues.
     */
    private final CompilationListener listener;

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
     * Threshold used for the allocation.
     */
    private final double threshold;

    public WeightsCodePartitioner(BankSchema bankSchema, AtomicSpecification atomicSpec,
            double loopFactor, double conditionalFactor, double threshold,
            CommonBankAllocationAlgorithm commonBankAlgorithm, CompilationListener listener) {
        checkNotNull(bankSchema, "bank schema cannot be null");
        checkNotNull(atomicSpec, "atomic specification cannot be null");
        checkNotNull(commonBankAlgorithm, "common bank allocation algorithm cannot be null");
        checkNotNull(listener, "listener cannot be null");
        checkArgument(loopFactor >= 1., "the loop factor must be greater than or equal to 1");
        checkArgument(0. <= conditionalFactor && conditionalFactor <= 1., "the conditional factor must be in range [0, 1]");
        checkArgument(threshold >= 0., "threshold cannot be negative");

        this.bankSchema = bankSchema;
        this.commonBankAllocator = new CommonBankAllocator(atomicSpec);
        this.commonBankFiller = new CommonBankFillerFactory().create(commonBankAlgorithm);
        this.loopFactor = loopFactor;
        this.conditionalFactor = conditionalFactor;
        this.threshold = threshold;
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

    private void assignFunctions(BComponentsPartitionContext context) throws PartitionImpossibleException {
        final UndirectedCallGraph undirectedCallGraph = new UndirectedCallGraph(context.callGraph);

        while (!undirectedCallGraph.vertices.isEmpty()) {
            final Optional<UndirectedNeighbour> maxEdge = computeMaxEdge(context,
                    undirectedCallGraph);

            if (!maxEdge.isPresent()) {
                for (FunctionVertex vertex : undirectedCallGraph.vertices) {
                    final Optional<String> bank = context.getFloorBank(context.getFunctionSize(vertex.getUniqueName()));
                    if (!bank.isPresent()) {
                        throw new PartitionImpossibleException("cannot find bank for isolated vertex");
                    }
                    context.assign(context.functions.get(vertex.getUniqueName()), bank.get());
                }
                undirectedCallGraph.vertices.clear();
            } else {
                final Optional<String> maxFreeSpaceBank = context.getCeilingBank(1);
                if (!maxFreeSpaceBank.isPresent()) {
                    throw new PartitionImpossibleException("not enough space");
                } else {
                    final FunctionVertex bigger = getBiggerFunctionVertex(context, maxEdge.get());
                    if (!context.fitsIn(bigger.getUniqueName(), maxFreeSpaceBank.get())) {
                        throw new PartitionImpossibleException("not enough space");
                    }

                    context.assign(context.functions.get(bigger.getUniqueName()), maxFreeSpaceBank.get());

                    final Set<FunctionVertex> otherVertices = new HashSet<>(undirectedCallGraph.vertices);
                    otherVertices.remove(bigger);
                    final Set<FunctionVertex> assigned = new HashSet<>();
                    assigned.add(bigger);

                    while (!otherVertices.isEmpty()) {
                        final MaximumAggregatedWeightVertex maxAggregatedWeightVertex =
                                computeMaximumAggregatedWeightVertex(otherVertices, maxFreeSpaceBank.get());
                        if (maxAggregatedWeightVertex.weightsSum < this.threshold) {
                            break;
                        }

                        if (context.fitsIn(maxAggregatedWeightVertex.vertex.getUniqueName(), maxFreeSpaceBank.get())) {
                            context.assign(context.functions.get(maxAggregatedWeightVertex.vertex.getUniqueName()),
                                    maxFreeSpaceBank.get());
                            assigned.add(maxAggregatedWeightVertex.vertex);
                        }
                        otherVertices.remove(maxAggregatedWeightVertex.vertex);
                    }

                    for (FunctionVertex assignedVertex : assigned) {
                        undirectedCallGraph.removeVertex(assignedVertex);
                    }
                }
            }
        }
    }

    private MaximumAggregatedWeightVertex computeMaximumAggregatedWeightVertex(
                Set<FunctionVertex> vertices, String bankName) {
        FunctionVertex bestVertex = vertices.iterator().next();
        double bestSum = 0;

        for (FunctionVertex vertex : vertices) {
            double currentSum = 0.;

            for (CallEdge neighbour : vertex.getSuccessors()) {
                if (neighbour.getTargetVertex().getTargetBank().isPresent()
                        && neighbour.getTargetVertex().getTargetBank().get().equals(bankName)) {
                    currentSum += neighbour.getFrequencyEstimation();
                }
            }

            for (CallEdge neighbour : vertex.getPredecessors()) {
                if (neighbour.getSourceVertex().getTargetBank().isPresent()
                        && neighbour.getSourceVertex().getTargetBank().get().equals(bankName)) {
                    currentSum += neighbour.getFrequencyEstimation();
                }
            }

            if (currentSum > bestSum) {
                bestVertex = vertex;
                bestSum = currentSum;
            } else if (currentSum == bestSum) {
                if (vertex.getUniqueName().compareTo(bestVertex.getUniqueName()) > 0) {
                    bestVertex = vertex;
                    bestSum = currentSum;
                }
            }
        }

        return new MaximumAggregatedWeightVertex(bestVertex, bestSum);
    }

    private FunctionVertex getBiggerFunctionVertex(BComponentsPartitionContext context,
                UndirectedNeighbour neighbour) {
        final int sizeVertex = context.getFunctionSize(neighbour.vertex.getUniqueName());
        final int sizeNeighbour = context.getFunctionSize(neighbour.neighbour.getUniqueName());

        if (sizeVertex > sizeNeighbour) {
            return neighbour.vertex;
        } else if (sizeNeighbour > sizeVertex) {
            return neighbour.neighbour;
        } else if (neighbour.vertex.getUniqueName().compareTo(neighbour.neighbour.getUniqueName()) > 0) {
            return neighbour.vertex;
        } else {
            return neighbour.neighbour;
        }
    }

    private Optional<UndirectedNeighbour> computeMaxEdge(BComponentsPartitionContext context,
                UndirectedCallGraph callGraph) {
        Optional<UndirectedNeighbour> maxEdge = Optional.absent();

        for (Map.Entry<FunctionVertex, UndirectedNeighbour> edgeEntry : callGraph.neighbours.entries()) {
            final UndirectedNeighbour edge = edgeEntry.getValue();
            if (!maxEdge.isPresent()) {
                maxEdge = Optional.of(edge);
            } else {
                if (edge.frequency > maxEdge.get().frequency) {
                    maxEdge = Optional.of(edge);
                } else if (edge.frequency == maxEdge.get().frequency) {
                    final int maxEdgeSize = Math.max(context.getFunctionSize(maxEdge.get().vertex.getUniqueName()),
                            context.getFunctionSize(maxEdge.get().neighbour.getUniqueName()));
                    final int edgeSize = Math.max(context.getFunctionSize(edge.vertex.getUniqueName()),
                            context.getFunctionSize(edge.neighbour.getUniqueName()));

                    if (edgeSize > maxEdgeSize) {
                        maxEdge = Optional.of(edge);
                    } else if (edgeSize == maxEdgeSize) {
                        final String maxEdgeName = Collections.min(ImmutableList.of(maxEdge.get().vertex.getUniqueName(),
                                maxEdge.get().neighbour.getUniqueName()));
                        final String edgeName = Collections.min(ImmutableList.of(edge.vertex.getUniqueName(),
                                edge.neighbour.getUniqueName()));

                        if (edgeName.compareTo(maxEdgeName) < 0) {
                            maxEdge = Optional.of(edge);
                        }
                    }
                }
            }
        }

        return maxEdge;
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
         * Map with functions that will be partitioned.
         */
        private final ImmutableMap<String, FunctionDecl> functions;

        private BComponentsPartitionContext(Iterable<FunctionDecl> functions,
                    Map<String, Range<Integer>> functionsSizes, ReferencesGraph refsGraph) {
            super(bankSchema, functionsSizes);
            this.callGraph = new CallGraph(functions, refsGraph);

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

    private static final class UndirectedCallGraph {
        private final Set<FunctionVertex> vertices;
        private final ListMultimap<FunctionVertex, UndirectedNeighbour> neighbours;

        private UndirectedCallGraph(CallGraph callGraph) {
            checkNotNull(callGraph, "call graph cannot be null");

            final PrivateBuilder builder = new RealBuilder(callGraph);
            this.vertices = builder.buildVertices();
            this.neighbours = builder.buildNeighbours();
        }

        private void removeVertex(FunctionVertex vertex) {
            if (!vertices.remove(vertex)) {
                throw new IllegalStateException("removal of an absent vertex");
            }

            final Set<FunctionVertex> neighbours = new HashSet<>();
            for (UndirectedNeighbour neighbour : this.neighbours.get(vertex)) {
                neighbours.add(neighbour.neighbour);
            }

            this.neighbours.removeAll(vertex);
            for (FunctionVertex neighbour : neighbours) {
                final Iterator<UndirectedNeighbour> neighbourIt =
                        this.neighbours.get(neighbour).iterator();
                boolean removed = false;
                while (neighbourIt.hasNext()) {
                    final UndirectedNeighbour neighbourEdge = neighbourIt.next();
                    if (neighbourEdge.neighbour == vertex) {
                        neighbourIt.remove();
                        removed = true;
                    }
                }

                if (!removed) {
                    throw new RuntimeException("removal of a vertex failed");
                }
            }

            for (UndirectedNeighbour neighbour : this.neighbours.values()) {
                if (neighbour.vertex == vertex) {
                    throw new RuntimeException("removal of a vertex failed (vertex)");
                } else if (neighbour.neighbour == vertex) {
                    throw new RuntimeException("removal of a vertex failed (neighbour)");
                }
            }
        }

        private interface PrivateBuilder {
            Set<FunctionVertex> buildVertices();
            ListMultimap<FunctionVertex, UndirectedNeighbour> buildNeighbours();
        }

        private static final class RealBuilder implements PrivateBuilder {
            private final CallGraph callGraph;

            private RealBuilder(CallGraph callGraph) {
                this.callGraph = callGraph;
            }

            @Override
            public Set<FunctionVertex> buildVertices() {
                final Set<FunctionVertex> vertices = new HashSet<>();

                for (FunctionVertex vertex : callGraph.getVertices().values()) {
                    if (vertex.getTargetBank().isPresent()) {
                        continue;
                    }
                    vertices.add(vertex);
                }

                return vertices;
            }

            @Override
            public ListMultimap<FunctionVertex, UndirectedNeighbour> buildNeighbours() {
                final ListMultimap<FunctionVertex, UndirectedNeighbour> neighbours = ArrayListMultimap.create();

                for (FunctionVertex vertex : callGraph.getVertices().values()) {
                    if (vertex.getTargetBank().isPresent()) {
                        continue;
                    }

                    final Map<FunctionVertex, Double> accumulatedFrequencies = new HashMap<>();
                    accumulateFrequencies(accumulatedFrequencies, vertex.getPredecessors(), vertex);
                    accumulateFrequencies(accumulatedFrequencies, vertex.getSuccessors(), vertex);

                    if (neighbours.containsKey(vertex)) {
                        throw new RuntimeException("vertex unexpectedly present in neighbours map");
                    }

                    for (Map.Entry<FunctionVertex, Double> neighbourEntry : accumulatedFrequencies.entrySet()) {
                        neighbours.put(vertex, new UndirectedNeighbour(vertex, neighbourEntry.getKey(),
                                neighbourEntry.getValue()));
                    }
                }

                return neighbours;
            }

            private void accumulateFrequencies(Map<FunctionVertex, Double> frequencies,
                        Iterable<CallEdge> edges, FunctionVertex vertex) {
                for (CallEdge edge : edges) {
                    if (edge.getSourceVertex() == vertex && edge.getTargetVertex() == vertex) {
                        continue;
                    }
                    final FunctionVertex otherVertex = edge.getSourceVertex() == vertex
                            ? edge.getTargetVertex()
                            : edge.getSourceVertex();

                    if (otherVertex.getTargetBank().isPresent()) {
                        continue;
                    }

                    if (!frequencies.containsKey(otherVertex)) {
                        frequencies.put(otherVertex, 0.0);
                    }
                    frequencies.put(otherVertex, frequencies.get(otherVertex) + edge.getFrequencyEstimation());
                }
            }
        }
    }

    private static final class UndirectedNeighbour {
        private final FunctionVertex vertex;
        private final FunctionVertex neighbour;
        private final double frequency;

        private UndirectedNeighbour(FunctionVertex vertex, FunctionVertex neighbour,
                double frequency) {
            this.vertex = vertex;
            this.neighbour = neighbour;
            this.frequency = frequency;
        }
    }

    private static final class MaximumAggregatedWeightVertex {
        private final FunctionVertex vertex;
        private final double weightsSum;

        private MaximumAggregatedWeightVertex(FunctionVertex vertex, double weightsSum) {
            this.vertex = vertex;
            this.weightsSum = weightsSum;
        }
    }
}
