package pl.edu.mimuw.nesc.wiresgraph;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that represents a node in the wires graph with indices for it that
 * specify (if they are present) particular element if it represents a command
 * or event from a parameterised interface or parameterised bare command or
 * event.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class IndexedNode {
    /**
     * The node.
     */
    private final SpecificationElementNode node;

    /**
     * List that specifies the element of the parameterised specification
     * element.
     */
    private final Optional<ImmutableList<BigInteger>> indices;

    public IndexedNode(SpecificationElementNode node, Optional<ImmutableList<BigInteger>> indices) {
        checkNotNull(node, "node cannot be null");
        checkNotNull(indices, "indices cannot be null");
        checkArgument(!indices.isPresent() || !indices.get().isEmpty(),
                "indices cannot be an empty list");

        if (indices.isPresent() && indices.get().size() != node.getIndicesCount()) {
            throw new IllegalArgumentException("invalid count of indices, expected "
                    + node.getIndicesCount()  + " but got " + indices.get().size());
        }

        this.node = node;
        this.indices = indices;
    }

    /**
     * Get the node.
     *
     * @return The node.
     */
    public SpecificationElementNode getNode() {
        return node;
    }

    /**
     * Get the indices that specify the element of the parameterised
     * interface or bare command or event of the node.
     *
     * @return List with indices that specify particular element.
     */
    public Optional<ImmutableList<BigInteger>> getIndices() {
        return indices;
    }
}
