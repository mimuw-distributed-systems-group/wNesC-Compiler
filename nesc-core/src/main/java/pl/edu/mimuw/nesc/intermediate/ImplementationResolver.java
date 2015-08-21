package pl.edu.mimuw.nesc.intermediate;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultiset;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import pl.edu.mimuw.nesc.wiresgraph.IndexedNode;
import pl.edu.mimuw.nesc.wiresgraph.SpecificationElementNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class responsible for finding implementation of used commands and events
 * using the connections in the wires graph.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class ImplementationResolver {
    /**
     * Stack that contains state of iteration at each point of the current path
     * from the source.
     */
    private final Deque<SuccessorIterationState<?>> stateStack = new ArrayDeque<>();

    /**
     * Multimap that contains implementations found for the source command or
     * event given at construction.
     */
    private final ListMultimap<Optional<ImmutableList<BigInteger>>, IndexedNode> implementations =
            ArrayListMultimap.create();

    /**
     * Indices for the source of the found implementation.
     */
    private Optional<ImmutableList<BigInteger>> sourceIndices = Optional.absent();

    /**
     * Map with wrappers for nodes in the graph.
     */
    private final Map<SpecificationElementNode, NodeWrapper> nodeWrappers;

    /**
     * Visitor that facilitates invoking appropriate method to make an iteration
     * step.
     */
    private final SuccessorIterationState.Visitor<Void, Void> iterationStepGateway = new SuccessorIterationState.Visitor<Void, Void>() {
        @Override
        public Void visit(FilteredIterationState state, Void arg) {
            iterationStep(state);
            return null;
        }

        @Override
        public Void visit(FullIterationState state, Void arg) {
            iterationStep(state);
            return null;
        }
    };

    ImplementationResolver(SpecificationElementNode source, Map<SpecificationElementNode, NodeWrapper> nodeWrappers) {
        checkNotNull(source, "source cannot be null");
        checkNotNull(nodeWrappers, "wrappers map cannot be null");

        if (!nodeWrappers.containsKey(source)) {
            nodeWrappers.put(source, new NodeWrapper(source));
        }

        this.nodeWrappers = nodeWrappers;
        this.stateStack.push(new FullIterationState(nodeWrappers.get(source),
                source.getSuccessors().entries().iterator()));
    }

    public ImmutableListMultimap<Optional<ImmutableList<BigInteger>>, IndexedNode> resolve() {
        while (!stateStack.isEmpty()) {
            stateStack.peek().accept(iterationStepGateway, null);
        }

        return sortImplementations();
    }

    private ImmutableListMultimap<Optional<ImmutableList<BigInteger>>, IndexedNode> sortImplementations() {
        // Sort the keys
        final NavigableSet<Optional<ImmutableList<BigInteger>>> sortedIndices =
                new TreeSet<>(new IndicesComparator());
        sortedIndices.addAll(implementations.keySet());
        if (sortedIndices.size() != implementations.keySet().size()) {
            throw new RuntimeException("invalid size of the set with indices, actual "
                    + sortedIndices.size() + ", expected " + implementations.keySet().size());
        }

        // Create the multimap with proper ordering of keys and values
        final ImmutableListMultimap.Builder<Optional<ImmutableList<BigInteger>>, IndexedNode>
                sortedImplementationsBuilder = ImmutableListMultimap.builder();
        for (Optional<ImmutableList<BigInteger>> indices : sortedIndices) {
            final Multiset<IndexedNode> sortedIndexedNodes = TreeMultiset.create(new IndexedNodeComparator());
            sortedIndexedNodes.addAll(implementations.get(indices));

            if (sortedIndexedNodes.size() != implementations.get(indices).size()) {
                throw new RuntimeException("invalid size of a set with indexed nodes, actual "
                        + sortedIndexedNodes.size() + ", expected " + implementations.get(indices).size());
            }

            for (IndexedNode indexedNode : sortedIndexedNodes) {
                sortedImplementationsBuilder.put(indices, indexedNode);
            }
        }

        return sortedImplementationsBuilder.build();
    }

    private void iterationStep(FullIterationState state) {
        final Optional<Map.Entry<Optional<ImmutableList<BigInteger>>, IndexedNode>> optSuccessor =
                state.nextElement();

        if (state.getNodeWrapper().getNode().getIndicesCount() > 0) {
            sourceIndices = optSuccessor.isPresent()
                    ? optSuccessor.get().getKey()
                    : Optional.<ImmutableList<BigInteger>>absent();
        }

        if (optSuccessor.isPresent()) {
            final Map.Entry<Optional<ImmutableList<BigInteger>>, IndexedNode> successor = optSuccessor.get();
            handleSuccessor(successor.getValue());
        } else {
            state.getNodeWrapper().setOnPath(false);
            stateStack.pop();
        }
    }

    private void iterationStep(FilteredIterationState state) {
        final Optional<IndexedNode> optSuccessor = state.nextElement();

        if (optSuccessor.isPresent()) {
            final IndexedNode successor = optSuccessor.get();

            if (successor.getNode().getIndicesCount() > 0
                    && !successor.getIndices().isPresent()) {
                handleSuccessor(new IndexedNode(successor.getNode(),
                        Optional.of(state.getNodeIndices())));
            } else {
                handleSuccessor(successor);
            }
        } else {
            state.getNodeWrapper().removeIndices(state.getNodeIndices());
            stateStack.pop();
        }
    }

    private void handleSuccessor(IndexedNode node) {
        if (node.getNode().getEntityData().isImplemented()) {
            implementations.put(sourceIndices, node);
        } else if (node.getIndices().isPresent()) {
            final NodeWrapper wrapper = getNodeWrapper(node.getNode());

            if (wrapper.isOnPath() && sourceIndices.get().equals(node.getIndices().get())
                    || !wrapper.addIndices(node.getIndices().get())) {
                // FIXME throw checked exception
                throw new RuntimeException("cycle detected");
            }

            final Iterable<IndexedNode> specificSuccessors =
                    node.getNode().getSuccessors().get(node.getIndices());
            final Iterable<IndexedNode> generalSuccessors =
                    node.getNode().getSuccessors().get(Optional.<ImmutableList<BigInteger>>absent());
            final Iterable<IndexedNode> allSuccessors = FluentIterable.from(generalSuccessors)
                    .append(specificSuccessors);

            stateStack.push(new FilteredIterationState(wrapper, allSuccessors.iterator(), node.getIndices().get()));
        } else {
            final NodeWrapper wrapper = getNodeWrapper(node.getNode());

            if (wrapper.isOnPath()) {
                // FIXME throw checked exception
                throw new RuntimeException("cycle detected");
            }

            wrapper.setOnPath(true);
            stateStack.push(new FullIterationState(wrapper, node.getNode().getSuccessors().entries().iterator()));
        }
    }

    private NodeWrapper getNodeWrapper(SpecificationElementNode node) {
        if (!nodeWrappers.containsKey(node)) {
            nodeWrappers.put(node, new NodeWrapper(node));
        }

        return nodeWrappers.get(node);
    }

    /**
     * Comparator for the indices of connections. An absent element is first and
     * further elements are sorted in an ascending lexicographical order.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class IndicesComparator implements Comparator<Optional<ImmutableList<BigInteger>>> {
        /**
         * Comparator for comparing the lists.
         */
        private final Comparator<Iterable<BigInteger>> listComparator = Ordering.<BigInteger>natural().lexicographical();

        @Override
        public int compare(Optional<ImmutableList<BigInteger>> indices1,
                Optional<ImmutableList<BigInteger>> indices2) {
            checkNotNull(indices1, "first indices cannot be null");
            checkNotNull(indices2, "second indices cannot be null");

            if (indices1.isPresent() && !indices2.isPresent()) {
                return 1;
            } else if (!indices1.isPresent() && indices2.isPresent()) {
                return -1;
            } else if (!indices1.isPresent()) {
                return 0;
            } else {
                return listComparator.compare(indices1.get(), indices2.get());
            }
        }
    }

    /**
     * Comparator for indexed nodes. First, the name of the specification
     * element is compared and second the indices.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class IndexedNodeComparator implements Comparator<IndexedNode> {
        /**
         * Comparator used for comparisons of the indices.
         */
        private final IndicesComparator indicesComparator = new IndicesComparator();

        @Override
        public int compare(IndexedNode indexedNode1, IndexedNode indexedNode2) {
            checkNotNull(indexedNode1, "first indexed node cannot be null");
            checkNotNull(indexedNode2, "second indexed node cannot be null");

            final int resultName = indexedNode1.getNode().getName().compareTo(
                    indexedNode2.getNode().getName());
            return resultName != 0
                    ? resultName
                    : indicesComparator.compare(indexedNode1.getIndices(), indexedNode2.getIndices());
        }
    }
}
