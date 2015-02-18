package pl.edu.mimuw.nesc.intermediate;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import pl.edu.mimuw.nesc.wiresgraph.SpecificationElementNode;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class with information about a specification element node that are related
 * to traversing the wires graph.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class NodeWrapper {
    /**
     * The wrapped node.
     */
    private final SpecificationElementNode node;

    /**
     * Flag indicating if the node is on the current path.
     */
    private boolean isOnPath;

    /**
     * Set with indices of the node that specify elements on the current path.
     */
    private final Set<ImmutableList<BigInteger>> indices;

    NodeWrapper(SpecificationElementNode wrappedNode) {
        checkNotNull(wrappedNode, "the wrapped node cannot be null");
        this.node = wrappedNode;
        this.isOnPath = false;
        this.indices = new HashSet<>();
    }

    /**
     * Get the wrapped node.
     *
     * @return The node wrapped by this object.
     */
    public SpecificationElementNode getNode() {
        return node;
    }

    /**
     * Check the state of <code>isOnPath</code> flag.
     *
     * @return Value of the <code>isOnPath</code> flag.
     */
    public boolean isOnPath() {
        return isOnPath;
    }

    /**
     * Set the value of <code>isOnPath</code> flag.
     *
     * @param isOnPath The value of flag <code>isOnPath</code> to set.
     */
    public void setOnPath(boolean isOnPath) {
        this.isOnPath = isOnPath;
    }

    /**
     * Add the given indices to the set of indices that specify elements
     * currently on the path.
     *
     * @param indices List with indices to add.
     * @return <code>true</code> if the given indices have not been in the set.
     */
    public boolean addIndices(ImmutableList<BigInteger> indices) {
        checkNotNull(indices, "indices cannot be null");
        checkArgument(!indices.isEmpty(), "indices cannot be an empty list");
        checkArgument(indices.size() == node.getIndicesCount(),
                "invalid count of indices, expected " + node.getIndicesCount()
                + " but got " + indices.size());

        return this.indices.add(indices);
    }

    /**
     * Remove the given indices from the set contained in this wrapper.
     *
     * @param indices Indices to remove.
     * @throws IllegalStateException The set does not contain the given indices.
     */
    public void removeIndices(ImmutableList<BigInteger> indices) {
        checkNotNull(indices, "indices cannot be null");

        if (!this.indices.remove(indices)) {
            throw new IllegalStateException("the indices set does not contain given element");
        }
    }
}
