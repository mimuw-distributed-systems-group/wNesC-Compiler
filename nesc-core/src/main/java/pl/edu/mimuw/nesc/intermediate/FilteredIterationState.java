package pl.edu.mimuw.nesc.intermediate;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.Iterator;
import pl.edu.mimuw.nesc.wiresgraph.IndexedNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Object that contains state of the iteration over successors during a DFS
 * traversal of wires graph when iterating over successors of particular
 * indices of a node.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class FilteredIterationState extends SuccessorIterationState<IndexedNode> {
    /**
     * Indices for the node.
     */
    private final ImmutableList<BigInteger> nodeIndices;

    FilteredIterationState(NodeWrapper nodeWrapper, Iterator<IndexedNode> successorsIt,
            ImmutableList<BigInteger> nodeIndices) {
        super(nodeWrapper, successorsIt);
        checkNotNull(nodeIndices, "the indices cannot be null");
        this.nodeIndices = nodeIndices;
    }

    ImmutableList<BigInteger> getNodeIndices() {
        return nodeIndices;
    }

    @Override
    <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
