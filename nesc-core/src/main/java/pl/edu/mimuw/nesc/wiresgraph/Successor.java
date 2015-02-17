package pl.edu.mimuw.nesc.wiresgraph;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that represents a successor in the wires graph.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Successor {
    /**
     * The node of the successor.
     */
    private final SpecificationElementNode node;

    /**
     * List that specifies the element of the parameterised specification
     * element that is the successor.
     */
    private final Optional<ImmutableList<BigInteger>> indices;

    Successor(SpecificationElementNode node, Optional<ImmutableList<BigInteger>> indices) {
        checkNotNull(node, "node cannot be null");
        checkNotNull(indices, "indices cannot be null");

        this.node = node;
        this.indices = indices;
    }

    /**
     * Get the node of this successor.
     *
     * @return Node of this successor.
     */
    public SpecificationElementNode getNode() {
        return node;
    }

    /**
     * Get the indices that specify the element of the parameterised
     * interface or bare command or event of the node.
     *
     * @return List with indices that specify the successor.
     */
    public Optional<ImmutableList<BigInteger>> getIndices() {
        return indices;
    }
}
