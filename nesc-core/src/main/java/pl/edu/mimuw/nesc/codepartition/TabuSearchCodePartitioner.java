package pl.edu.mimuw.nesc.codepartition;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.TreeMultimap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.codepartition.context.DynamicPartitionContext;
import pl.edu.mimuw.nesc.codesize.CodeSizeEstimation;
import pl.edu.mimuw.nesc.common.AtomicSpecification;
import pl.edu.mimuw.nesc.common.util.FindUnionSet;
import pl.edu.mimuw.nesc.common.util.NavigableInverseMap;
import pl.edu.mimuw.nesc.refsgraph.EntityNode;
import pl.edu.mimuw.nesc.refsgraph.Reference;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Code partitioner that uses the tabu search heuristic for generating the
 * partition.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TabuSearchCodePartitioner implements CodePartitioner {
    /**
     * Bank schema used by this partitioner.
     */
    private final BankSchema bankSchema;

    /**
     * Common bank allocator used by this partitioner.
     */
    private final CommonBankAllocator commonBankAllocator;

    public TabuSearchCodePartitioner(BankSchema bankSchema, AtomicSpecification atomicSpec) {
        checkNotNull(bankSchema, "bank schema cannot be null");
        checkNotNull(atomicSpec, "atomic specification cannot be null");
        this.bankSchema = bankSchema;
        this.commonBankAllocator = new CommonBankAllocator(atomicSpec);
    }

    @Override
    public BankSchema getBankSchema() {
        return bankSchema;
    }

    @Override
    public BankTable partition(Iterable<FunctionDecl> functions, CodeSizeEstimation sizesEstimation,
            ReferencesGraph refsGraph) throws PartitionImpossibleException {
        checkNotNull(functions, "functions cannot be null");
        checkNotNull(sizesEstimation, "estimation cannot be null");
        checkNotNull(refsGraph, "references graph cannot be null");

        final TabuSearchPartitionContext context = new TabuSearchPartitionContext(functions,
                sizesEstimation.getFunctionsSizes(), refsGraph);
        new StaticWeightsAdder().addWeights(context.switchingGraph, refsGraph);
        final ImmutableList<FunctionDecl> remainingFuns = allocateToCommonBank(context, functions);
        computeKCut(context, remainingFuns);
        tune(context);

        return context.finish();
    }

    private ImmutableList<FunctionDecl> allocateToCommonBank(TabuSearchPartitionContext context,
                Iterable<FunctionDecl> functions) throws PartitionImpossibleException {
        // Allocate functions that must be in common bank
        final ImmutableList<FunctionDecl> remainingFunctions =
                commonBankAllocator.allocate(context, functions);

        // Fix the assignment of common bank functions
        for (FunctionDecl function : context.getBankContents(bankSchema.getCommonBankName())) {
            context.fixedFunctions.add(DeclaratorUtils.getUniqueName(function.getDeclarator()).get());
        }

        return remainingFunctions;
    }

    private void computeKCut(TabuSearchPartitionContext context, Iterable<FunctionDecl> functions)
                throws PartitionImpossibleException {
        checkNotNull(context, "context cannot be null");

        // Assign all vertices to the common bank
        for (FunctionDecl function : functions) {
            context.assign(function, bankSchema.getCommonBankName());
        }

        while (!context.getOverloadedBanks().isEmpty() && !context.getEmptyBanks().isEmpty()) {
            final String overloadedBank = context.getOverloadedBanks().iterator().next();
            final String emptyBank = context.getEmptyBanks().last();
            final ImmutableSet.Builder<String> functionsInOverloadedBankBuilder =
                    ImmutableSet.builder();

            for (FunctionDecl function : context.getBankContents(overloadedBank)) {
                final String uniqueName = DeclaratorUtils.getUniqueName(function.getDeclarator()).get();
                if (!context.fixedFunctions.contains(uniqueName)) {
                    functionsInOverloadedBankBuilder.add(uniqueName);
                }
            }

            final Optional<Cut> minimumCut = computeMinimumCut(context.switchingGraph.newSubgraph(
                    functionsInOverloadedBankBuilder.build()));
            if (!minimumCut.isPresent()) {
                throw new PartitionImpossibleException("a function exceeds the size of a bank");
            }

            for (String functionName : minimumCut.get().getSecondSubgraph().getVertices()) {
                context.remove(context.functions.get(functionName));
                context.assign(context.functions.get(functionName), emptyBank);
            }
        }
    }

    /**
     * Compute the minimum cut of the given graph. The result is computed
     * exactly - it is not any heuristic algorithm.
     *
     * @param graph Graph whose minimum cut will be computed.
     * @return The minimum cut of the given graph. The result is absent if the
     *         given graph does not have any minimum cut - it is the case if the
     *         graph has only one vertex.
     */
    private Optional<Cut> computeMinimumCut(SwitchingGraph.Subgraph graph) {
        checkNotNull(graph, "graph cannot be null");

        Optional<Cut> currentMin = Optional.absent();
        final FindUnionSet<String> verticesSet = new FindUnionSet<>(graph.getVertices());

        while (verticesSet.size() > 1) {
            final String currentSet = verticesSet.getAllRepresentatives().iterator().next();
            final ImmutableMap<String, Integer> weightsToOtherSets =
                    computeWeightsToOtherSets(currentSet, verticesSet, graph);

            int totalWeight = 0;
            Optional<Map.Entry<String, Integer>> maximumEntry = Optional.absent();

            for (Map.Entry<String, Integer> weightEntry : weightsToOtherSets.entrySet()) {
                totalWeight += weightEntry.getValue();
                if (!maximumEntry.isPresent() || weightEntry.getValue() > maximumEntry.get().getValue()) {
                    maximumEntry = Optional.of(weightEntry);
                }
            }

            if (!currentMin.isPresent() || totalWeight < currentMin.get().getValue()) {
                currentMin = Optional.of(generateCut(currentSet, verticesSet, graph, totalWeight));
            }

            verticesSet.union(currentSet, maximumEntry.get().getKey());
        }

        return currentMin;
    }

    private ImmutableMap<String, Integer> computeWeightsToOtherSets(String currentSet,
                FindUnionSet<String> verticesSet, SwitchingGraph.Subgraph graph) {
        currentSet = verticesSet.find(currentSet);
        final Map<String, Integer> weightsToOtherSets = new HashMap<>();

        // Initialize the weights to zero
        for (String representative : verticesSet.getAllRepresentatives()) {
            if (!representative.equals(currentSet)) {
                weightsToOtherSets.put(representative, 0);
            }
        }

        // Compute the weights
        for (String vertex : graph.getVertices()) {
            for (SwitchingActionEdge edge : graph.getNeighbours(vertex)) {
                final String firstSet = verticesSet.find(edge.getFirstVertex().getFunctionUniqueName());
                final String secondSet = verticesSet.find(edge.getSecondVertex().getFunctionUniqueName());
                final Optional<String> otherSet;

                /* The last condition in these if statements is to ensure that
                   each edge contributes only once to the result. */
                if (firstSet.equals(currentSet) && !secondSet.equals(currentSet)
                        && vertex.equals(edge.getFirstVertex().getFunctionUniqueName())) {
                    otherSet = Optional.of(secondSet);
                } else if (!firstSet.equals(currentSet) && secondSet.equals(currentSet)
                        && vertex.equals(edge.getFirstVertex().getFunctionUniqueName())) {
                    otherSet = Optional.of(firstSet);
                } else {
                    otherSet = Optional.absent();
                }

                if (otherSet.isPresent()) {
                    weightsToOtherSets.put(otherSet.get(), weightsToOtherSets.get(otherSet.get())
                            + edge.getWeight());
                }
            }
        }

        return ImmutableMap.copyOf(weightsToOtherSets);
    }

    private Cut generateCut(String extractedSet, FindUnionSet<String> verticesSets,
                    SwitchingGraph.Subgraph graph, int value) {
        extractedSet = verticesSets.find(extractedSet);
        final ImmutableSet.Builder<String> firstSubgraphVerticesBuilder =
                ImmutableSet.builder();
        final ImmutableSet.Builder<String> secondSubgraphVerticesBuilder =
                ImmutableSet.builder();

        for (String vertex : graph.getVertices()) {
            if (verticesSets.find(vertex).equals(extractedSet)) {
                firstSubgraphVerticesBuilder.add(vertex);
            } else {
                secondSubgraphVerticesBuilder.add(vertex);
            }
        }

        return new Cut(graph.getSwitchingGraph().newSubgraph(firstSubgraphVerticesBuilder.build()),
                graph.getSwitchingGraph().newSubgraph(secondSubgraphVerticesBuilder.build()),
                value);
    }

    private void tune(TabuSearchPartitionContext context) throws PartitionImpossibleException {
        // Compute inner weights
        final ImmutableMap<String, NavigableInverseMap<String, Integer>> innerWeights =
                computeInnerWeights(context);

        // Correct the assignment
        while (!context.getOverloadedBanks().isEmpty()) {
            // Find the minimum weight function from an overloaded bank
            final String overloadedBank = context.getOverloadedBanks().iterator().next();
            final String minimumWeightFunction = findMinimumWeightFunction(
                    context, innerWeights.get(overloadedBank));

            // Find the target bank for the minimum weight function
            final TreeMultimap<Integer, String> weightsToOtherBanks = computeWeightsToOtherBanks(context,
                    minimumWeightFunction, overloadedBank);
            final String maximumWeightBank = findMaximumWeightBank(context, weightsToOtherBanks,
                    context.getFunctionSize(minimumWeightFunction));

            // Move the function
            context.remove(context.functions.get(minimumWeightFunction));
            context.assign(context.functions.get(minimumWeightFunction),
                    maximumWeightBank);
            updateInnerWeights(context, minimumWeightFunction, overloadedBank,
                    maximumWeightBank, innerWeights);
        }
    }

    private String findMinimumWeightFunction(TabuSearchPartitionContext context,
                NavigableInverseMap<String, Integer> bankInnerWeights)
                throws PartitionImpossibleException {
        for (int currentWeight : bankInnerWeights.inverseMap().keySet()) {
            for (String funUniqueName : bankInnerWeights.inverseMap().get(currentWeight)) {
                if (!context.fixedFunctions.contains(funUniqueName)) {
                    return funUniqueName;
                }
            }
        }

        throw new PartitionImpossibleException("cannot find a function to move from an overloaded bank");
    }

    private String findMaximumWeightBank(TabuSearchPartitionContext context,
                TreeMultimap<Integer, String> weightsToOtherBanks, int movedFunctionSize)
                throws PartitionImpossibleException {
        for (int currentWeight : weightsToOtherBanks.keySet().descendingSet()) {
            for (String bankName : weightsToOtherBanks.get(currentWeight)) {
                if (context.getFreeSpace(bankName) >= movedFunctionSize) {
                    return bankName;
                }
            }
        }

        throw new PartitionImpossibleException("cannot find a new bank for a transferred function");
    }

    private TreeMultimap<Integer, String> computeWeightsToOtherBanks(TabuSearchPartitionContext context,
                String funUniqueName, String funBankName) {
        // Initialize weights to zeroes
        final Map<String, Integer> unsortedWeights = new HashMap<>();
        for (String bankName : bankSchema.getBanksNames()) {
            if (!bankName.equals(funBankName)) {
                unsortedWeights.put(bankName, 0);
            }
        }

        final FunctionVertex funVertex = context.switchingGraph.getVertices().get(funUniqueName);

        // Compute the weights
        for (SwitchingActionEdge edge : funVertex.getNeighbours().values()) {
            final String otherFunBank = context.getTargetBank(context.functions.get(
                    edge.getOtherVertex(funVertex).getFunctionUniqueName())).get();
            if (!otherFunBank.equals(funBankName)) {
                unsortedWeights.put(otherFunBank, unsortedWeights.get(otherFunBank)
                        + edge.getWeight());
            }
        }

        // Sort the weights
        final TreeMultimap<Integer, String> weightsToOtherBanks = TreeMultimap.create();
        for (Map.Entry<String, Integer> weightEntry : unsortedWeights.entrySet()) {
            weightsToOtherBanks.put(weightEntry.getValue(), weightEntry.getKey());
        }

        return weightsToOtherBanks;
    }

    /**
     * Compute mapping from banks to maps that associate each function with the
     * count of calls from its bank.
     *
     * @param context Context with the current assignment.
     * @return Map with inner weights.
     */
    private ImmutableMap<String, NavigableInverseMap<String, Integer>> computeInnerWeights(
                TabuSearchPartitionContext context) {
        final ImmutableMap.Builder<String, NavigableInverseMap<String, Integer>> innerWeightsBuilder =
                ImmutableMap.builder();

        for (String bankName : bankSchema.getBanksNames()) {
            final Set<FunctionDecl> bankContents = context.getBankContents(bankName);
            final NavigableInverseMap<String, Integer> bankInnerWeights = new NavigableInverseMap<>();
            innerWeightsBuilder.put(bankName, bankInnerWeights);

            for (FunctionDecl function : bankContents) {
                final String funUniqueName = DeclaratorUtils.getUniqueName(
                        function.getDeclarator()).get();
                bankInnerWeights.put(funUniqueName, 0);

                for (Reference predecessorReference : context.referencesGraph.getOrdinaryIds().get(funUniqueName).getPredecessors()) {
                    if (!predecessorReference.isInsideNotEvaluatedExpr()
                            && predecessorReference.getType() == Reference.Type.CALL
                            && predecessorReference.getReferencingNode().getKind() == EntityNode.Kind.FUNCTION
                            && bankContents.contains(context.functions.get(predecessorReference.getReferencingNode().getUniqueName()))) {
                        bankInnerWeights.put(funUniqueName, bankInnerWeights.get(funUniqueName) + 1);
                    }
                }
            }
        }

        return innerWeightsBuilder.build();
    }

    private void updateInnerWeights(
                TabuSearchPartitionContext context,
                String movedFunction,
                String oldBank,
                String newBank,
                ImmutableMap<String, NavigableInverseMap<String, Integer>> innerWeights
    ) {
        final WeightsDifference weightsDifference = computeWeightsDiffMap(context,
                context.switchingGraph.getVertices().get(movedFunction), oldBank, newBank);

        // Set new inner weight for the moved function
        innerWeights.get(oldBank).remove(movedFunction);
        innerWeights.get(newBank).put(movedFunction, weightsDifference.getNewWeightForTransferredFunction());

        // Update values for other functions
        for (Map.Entry<String, Integer> diffEntry : weightsDifference.getDifferences().entrySet()) {
            final String bankName = context.getTargetBank(context.functions.get(diffEntry.getKey())).get();
            final NavigableInverseMap<String, Integer> bankInnerWeights = innerWeights.get(bankName);
            bankInnerWeights.put(diffEntry.getKey(), bankInnerWeights.get(diffEntry.getKey())
                    + diffEntry.getValue());
        }
    }

    private WeightsDifference computeWeightsDiffMap(TabuSearchPartitionContext context,
                FunctionVertex transferredFunVertex, String oldBank, String newBank) {
        final Map<String, Integer> diffMap = new HashMap<>();
        int newInnerWeight = 0;

        // Iterate over successors of the transferred vertex
        for (Reference successorReference : context.referencesGraph.getOrdinaryIds().get(transferredFunVertex.getFunctionUniqueName()).getSuccessors()) {
            if (!successorReference.isInsideNotEvaluatedExpr()
                    && successorReference.getType() == Reference.Type.CALL
                    && successorReference.getReferencedNode().getKind() == EntityNode.Kind.FUNCTION
                    && context.functions.containsKey(successorReference.getReferencedNode().getUniqueName())) {
                final String successorBank = context.getTargetBank(context.functions.get(
                        successorReference.getReferencedNode().getUniqueName())).get();
                final int previousValue = Optional.fromNullable(diffMap.get(
                        successorReference.getReferencedNode().getUniqueName())).or(0);
                final Optional<Integer> newValue;

                if (oldBank.equals(successorBank)) {
                    newValue = Optional.of(previousValue - 1);
                } else if (newBank.equals(successorBank)) {
                    newValue = Optional.of(previousValue + 1);
                } else {
                    newValue = Optional.absent();
                }

                if (newValue.isPresent()) {
                    diffMap.put(successorReference.getReferencedNode().getUniqueName(), newValue.get());
                }
            }
        }

        // Iterate over predecessors of the transferred vertex
        for (Reference predecessorReference : context.referencesGraph.getOrdinaryIds().get(transferredFunVertex.getFunctionUniqueName()).getPredecessors()) {
            if (!predecessorReference.isInsideNotEvaluatedExpr()
                    && predecessorReference.getType() == Reference.Type.CALL
                    && predecessorReference.getReferencingNode().getKind() == EntityNode.Kind.FUNCTION) {
                ++newInnerWeight;
            }
        }

        return new WeightsDifference(ImmutableMap.copyOf(diffMap), newInnerWeight);
    }

    /**
     * Class that represents the graph of bank switches that will occur in the
     * program. It is a weighted undirected graph.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class SwitchingGraph {
        /**
         * All vertices of the graph.
         */
        private final ImmutableMap<String, FunctionVertex> vertices;

        private SwitchingGraph(Iterable<FunctionDecl> functions, ReferencesGraph refsGraph) {
            checkNotNull(functions, "functions cannot be null");
            checkNotNull(refsGraph, "references graph cannot be null");
            final PrivateBuilder builder = new RealBuilder(functions, refsGraph);
            this.vertices = builder.buildVertices();
        }

        /**
         * Get the map with all vertices of the graph. Keys are unique names of
         * functions and values are vertices that represent them.
         *
         * @return Immutable map with all vertices of the graph.
         */
        private ImmutableMap<String, FunctionVertex> getVertices() {
            return vertices;
        }

        /**
         * Create a new subgraph of this graph that contains only vertices that
         * represent functions with names from the given set. Edges of the
         * subgraph are only edges that connect vertices with names from the
         * given set.
         *
         * @param funsUniqueNames Unique names of functions represented by
         *                        vertices of this graph that will constitute
         *                        the returned subgraph.
         * @return Subgraph with vertices that represent functions with names
         *         from the given set.
         */
        private Subgraph newSubgraph(ImmutableSet<String> funsUniqueNames) {
            return new Subgraph(funsUniqueNames);
        }

        /**
         * Builder for particular elements of a switching graph.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private interface PrivateBuilder {
            ImmutableMap<String, FunctionVertex> buildVertices();
        }

        /**
         * Implementation of the builder interface.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private static final class RealBuilder implements PrivateBuilder {
            private final Iterable<FunctionDecl> functions;
            private final ReferencesGraph refsGraph;

            private RealBuilder(Iterable<FunctionDecl> functions, ReferencesGraph refsGraph) {
                this.functions = functions;
                this.refsGraph = refsGraph;
            }

            @Override
            public ImmutableMap<String, FunctionVertex> buildVertices() {
                // Sort names of functions
                final Set<String> funsUniqueNames = new TreeSet<>();
                for (FunctionDecl function : functions) {
                    final String uniqueName = DeclaratorUtils.getUniqueName(
                            function.getDeclarator()).get();
                    if (!funsUniqueNames.add(uniqueName)) {
                        throw new RuntimeException("function with name '" + uniqueName
                                + "' occurs more than once");
                    }
                }

                // Create the vertices map
                final ImmutableMap.Builder<String, FunctionVertex> verticesBuilder =
                        ImmutableMap.builder();
                for (String funUniqueName : funsUniqueNames) {
                    verticesBuilder.put(funUniqueName, new FunctionVertex(funUniqueName));
                }
                final ImmutableMap<String, FunctionVertex> vertices = verticesBuilder.build();

                // Add edges
                for (String funUniqueName : vertices.keySet()) {
                    final Set<String> newNeighbours = new TreeSet<>();
                    for (Reference successorReference : refsGraph.getOrdinaryIds().get(funUniqueName).getSuccessors()) {
                        if (successorReference.getType() == Reference.Type.CALL
                                && !successorReference.isInsideNotEvaluatedExpr()
                                && vertices.containsKey(successorReference.getReferencedNode().getUniqueName())) {
                            newNeighbours.add(successorReference.getReferencedNode().getUniqueName());
                        }
                    }
                    final Map<FunctionVertex, SwitchingActionEdge> currentNeighbours =
                            vertices.get(funUniqueName).getNeighbours();
                    for (String neighbourUniqueName : newNeighbours) {
                        final FunctionVertex neighbour = vertices.get(neighbourUniqueName);
                        if (!currentNeighbours.containsKey(neighbour)) {
                            vertices.get(funUniqueName).addNeighbour(neighbour);
                        }
                    }
                }

                return vertices;
            }
        }

        /**
         * A subgraph of a switching graph.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private final class Subgraph {
            /**
             * Set with unique names of vertices of this subgraph.
             */
            private final ImmutableSet<String> subgraphVertices;

            private Subgraph(ImmutableSet<String> subgraphVertices) {
                checkNotNull(subgraphVertices, "vertices cannot be null");
                checkArgument(vertices.keySet().containsAll(subgraphVertices),
                        "the given set contains a vertex that does not belong to this switching graph");
                this.subgraphVertices = subgraphVertices;
            }

            /**
             * Get unique names of functions that represented by vertices of
             * this subgraph.
             *
             * @return Set with unique names of functions represented by
             *         vertices of this subgraph.
             */
            private ImmutableSet<String> getVertices() {
                return subgraphVertices;
            }

            /**
             * Get the switching graph whose subgraph is this object.
             *
             * @return The whole graph whose part is this subgraph.
             */
            private SwitchingGraph getSwitchingGraph() {
                return SwitchingGraph.this;
            }

            /**
             * Get all neighbours of the given vertex in this subgraph.
             *
             * @param funUniqueName Unique name of the function whose neighbours
             *                      will be returned.
             * @return Iterable with neighbours of the given vertex.
             */
            private Iterable<SwitchingActionEdge> getNeighbours(String funUniqueName) {
                checkNotNull(funUniqueName, "unique name of the function cannot be null");
                checkArgument(!funUniqueName.isEmpty(), "unique name of the function cannot be an empty string");
                checkState(subgraphVertices.contains(funUniqueName), "the subgraph does not contain a vertex with given unique name");
                return new NeighboursIterable(vertices.get(funUniqueName));
            }

            /**
             * Iterable with neighbours of a vertex in the subgraph.
             *
             * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
             */
            private final class NeighboursIterable implements Iterable<SwitchingActionEdge> {
                private final FunctionVertex vertex;

                private NeighboursIterable(FunctionVertex vertex) {
                    checkNotNull(vertex, "vertex cannot be null");
                    this.vertex = vertex;
                }

                @Override
                public Iterator<SwitchingActionEdge> iterator() {
                    return new NeighboursIterator();
                }

                /**
                 * Iterator that allows for iteration over neighbours in the subgraph.
                 *
                 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
                 */
                private final class NeighboursIterator implements Iterator<SwitchingActionEdge> {
                    private final Iterator<SwitchingActionEdge> allNeighboursIt =
                            NeighboursIterable.this.vertex.getNeighbours().values().iterator();
                    private Optional<SwitchingActionEdge> nextEdge = Optional.absent();

                    @Override
                    public boolean hasNext() {
                        extractNext();
                        return nextEdge.isPresent();
                    }

                    @Override
                    public SwitchingActionEdge next() {
                        extractNext();

                        if (nextEdge.isPresent()) {
                            final SwitchingActionEdge nextElement = nextEdge.get();
                            nextEdge = Optional.absent();
                            return nextElement;
                        } else {
                            throw new NoSuchElementException("this iterator is exhausted");
                        }
                    }

                    private void extractNext() {
                        while (!nextEdge.isPresent() && allNeighboursIt.hasNext()) {
                            final SwitchingActionEdge edge = allNeighboursIt.next();

                            if (subgraphVertices.contains(edge.getFirstVertex().getFunctionUniqueName())
                                    && subgraphVertices.contains(edge.getSecondVertex().getFunctionUniqueName())) {
                                nextEdge = Optional.of(edge);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Class that represents a single vertex of the switching graph, i.e.
     * a function.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class FunctionVertex {
        /**
         * Unique name of the function represented by this vertex.
         */
        private final String funUniqueName;

        /**
         * Map with all neighbours of this vertex. It is a simple graph. Keys
         * are the neighbours and values are the edges to the neighbours. Each
         * edge is represented by two <code>SwitchingActionEdge</code> objects.
         */
        private final Map<FunctionVertex, SwitchingActionEdge> neighbours;

        /**
         * Unmodifiable view of the neighbours map.
         */
        private final Map<FunctionVertex, SwitchingActionEdge> unmodifiableNeighbours;

        private FunctionVertex(String funUniqueName) {
            checkNotNull(funUniqueName, "unique name of the function cannot be null");
            checkArgument(!funUniqueName.isEmpty(), "unique name of the function cannot be an empty string");
            this.funUniqueName = funUniqueName;
            this.neighbours = new HashMap<>();
            this.unmodifiableNeighbours = Collections.unmodifiableMap(this.neighbours);
        }

        private String getFunctionUniqueName() {
            return funUniqueName;
        }

        private Map<FunctionVertex, SwitchingActionEdge> getNeighbours() {
            return unmodifiableNeighbours;
        }

        private void addNeighbour(FunctionVertex neighbour) {
            checkNotNull(neighbour, "neighbour cannot be null");
            checkArgument(neighbour != this, "a vertex cannot be a neighbour to itself");
            checkState(!neighbours.containsKey(neighbour), "the neighbour has been already added");
            checkState(!neighbour.neighbours.containsKey(this), "the neighbours contains this vertex as its neighbour");

            final SwitchingActionEdge newEdge = new SwitchingActionEdge(this, neighbour);
            neighbours.put(neighbour, newEdge);
            neighbour.neighbours.put(this, newEdge);
        }
    }

    /**
     * Class that represents a single edge. Each edge is represented by one
     * object.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class SwitchingActionEdge {
        /**
         * The first vertex incident to this edge.
         */
        private final FunctionVertex firstVertex;

        /**
         * The second vertex incident to this edge.
         */
        private final FunctionVertex secondVertex;

        /**
         * The weight on this edge.
         */
        private int weight;

        private SwitchingActionEdge(FunctionVertex firstVertex, FunctionVertex secondVertex) {
            checkNotNull(firstVertex, "the first vertex cannot be null");
            checkNotNull(secondVertex, "the second vertex cannot be null");
            this.firstVertex = firstVertex;
            this.secondVertex = secondVertex;
            this.weight = 0;
        }

        private FunctionVertex getFirstVertex() {
            return firstVertex;
        }

        private FunctionVertex getSecondVertex() {
            return secondVertex;
        }

        private FunctionVertex getOtherVertex(FunctionVertex vertex) {
            checkNotNull(vertex, "vertex cannot be null");

            if (vertex == firstVertex) {
                return secondVertex;
            } else if (vertex == secondVertex) {
                return firstVertex;
            } else {
                throw new IllegalArgumentException("given vertex is not incident with this edge");
            }
        }

        private int getWeight() {
            return weight;
        }

        private void increaseWeight(int value) {
            checkArgument(value > 0, "the weight can only be increased a positive value");
            this.weight += value;
        }
    }

    /**
     * Interface for adding weights to the switching graph in which initially
     * all weights are set to 0.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface WeightsAdder {
        /**
         * Increase weights of the edges of the switching graph to the value
         * that depends on the implementing class.
         *
         * @param switchingGraph Graph to add the weights to.
         * @param refsGraph The references graph that was the basis for creation
         *                  of the switching graph.
         */
        void addWeights(SwitchingGraph switchingGraph, ReferencesGraph refsGraph);
    }

    /**
     * Weights set by this adder are counts of directed versions of the edges
     * which are counts of switching actions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class StaticWeightsAdder implements WeightsAdder {
        @Override
        public void addWeights(SwitchingGraph switchingGraph, ReferencesGraph refsGraph) {
            checkNotNull(switchingGraph, "switching graph cannot be null");
            checkNotNull(refsGraph, "references graph cannot be null");

            final ImmutableMap<String, FunctionVertex> functionsVertices =
                    switchingGraph.getVertices();

            for (Map.Entry<String, FunctionVertex> vertexEntry : functionsVertices.entrySet()) {
                for (Reference successorReference : refsGraph.getOrdinaryIds().get(vertexEntry.getKey()).getSuccessors()) {
                    if (successorReference.getType() == Reference.Type.CALL
                            && !successorReference.isInsideNotEvaluatedExpr()
                            && functionsVertices.containsKey(successorReference.getReferencedNode().getUniqueName())) {
                        final FunctionVertex neighbour = functionsVertices.get(
                                successorReference.getReferencedNode().getUniqueName());
                        vertexEntry.getValue().getNeighbours().get(neighbour).increaseWeight(1);
                    }
                }
            }
        }
    }

    /**
     * Result of the algorithm for the minimum cut problem.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class Cut {
        private final SwitchingGraph.Subgraph firstSubgraph;
        private final SwitchingGraph.Subgraph secondSubgraph;
        private final int value;

        private Cut(SwitchingGraph.Subgraph firstSubgraph, SwitchingGraph.Subgraph secondSubgraph,
                    int value) {
            checkNotNull(firstSubgraph, "the first subgraph cannot be null");
            checkNotNull(secondSubgraph, "the second subgraph cannot be null");
            checkArgument(Collections.disjoint(firstSubgraph.getVertices(), secondSubgraph.getVertices()),
                    "the given subgraphs are not disjoint");
            checkArgument(value >= 0, "the value of a cut cannot be negative");
            checkArgument(!firstSubgraph.getVertices().isEmpty(), "the first subgraph cannot be empty");
            checkArgument(!secondSubgraph.getVertices().isEmpty(), "the second subgraph cannot be empty");

            this.firstSubgraph = firstSubgraph;
            this.secondSubgraph = secondSubgraph;
            this.value = value;
        }

        private SwitchingGraph.Subgraph getFirstSubgraph() {
            return firstSubgraph;
        }

        private SwitchingGraph.Subgraph getSecondSubgraph() {
            return secondSubgraph;
        }

        private int getValue() {
            return value;
        }
    }

    /**
     * Context specific to this partitioner.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class TabuSearchPartitionContext extends DynamicPartitionContext {
        /**
         * The graph of functions for partition.
         */
        private final SwitchingGraph switchingGraph;

        /**
         * The graph that represents references between entities in the program.
         */
        private final ReferencesGraph referencesGraph;

        /**
         * Map with all functions to partition. Keys are their unique names and
         * values are their definitions.
         */
        private final ImmutableMap<String, FunctionDecl> functions;

        /**
         * Set with unique names of functions whose assignment cannot be
         * changed.
         */
        private final Set<String> fixedFunctions;

        private TabuSearchPartitionContext(Iterable<FunctionDecl> functions,
                    Map<String, Range<Integer>> functionsSizes, ReferencesGraph refsGraph) {
            super(bankSchema, functionsSizes);

            // Switching graph
            this.switchingGraph = new SwitchingGraph(functions, refsGraph);

            // References graph
            this.referencesGraph = refsGraph;

            // Map of functions
            final ImmutableMap.Builder<String, FunctionDecl> functionsBuilder =
                    ImmutableMap.builder();
            for (FunctionDecl function : functions) {
                functionsBuilder.put(DeclaratorUtils.getUniqueName(function.getDeclarator()).get(),
                        function);
            }
            this.functions = functionsBuilder.build();

            // Fixed functions
            this.fixedFunctions = new HashSet<>();
        }
    }

    /**
     * Small helper class that represents difference in weights of functions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class WeightsDifference {
        private final ImmutableMap<String, Integer> differences;
        private final int newWeightForTransferredFunction;

        private WeightsDifference(ImmutableMap<String, Integer> differences,
                    int newWeightForTransferredFunction) {
            checkNotNull(differences, "differences cannot be null");
            checkArgument(newWeightForTransferredFunction >= 0, "new weight for transferred function cannot be negative");
            this.differences = differences;
            this.newWeightForTransferredFunction = newWeightForTransferredFunction;
        }

        private ImmutableMap<String, Integer> getDifferences() {
            return differences;
        }

        private int getNewWeightForTransferredFunction() {
            return newWeightForTransferredFunction;
        }
    }
}
