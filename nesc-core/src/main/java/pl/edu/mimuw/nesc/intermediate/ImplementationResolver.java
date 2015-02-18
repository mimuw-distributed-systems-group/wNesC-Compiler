package pl.edu.mimuw.nesc.intermediate;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
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

    public ListMultimap<Optional<ImmutableList<BigInteger>>, IndexedNode> resolve() {
        while (!stateStack.isEmpty()) {
            stateStack.peek().accept(iterationStepGateway, null);
        }

        return implementations;
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
}
