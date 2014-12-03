package pl.edu.mimuw.nesc.wiresgraph;

import com.google.common.base.Optional;
import java.util.LinkedList;
import pl.edu.mimuw.nesc.ast.gen.Expression;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that represents a directed edge in the graph of specification
 * elements.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class WiringEdge {
    /**
     * Node that this edge points to.
     */
    private final SpecificationElementNode destinationNode;

    /**
     * Parameters of the source specification entity if they are provided and
     * the entity is a parameterised interface or a bare parameterised command
     * or event.
     */
    private final Optional<LinkedList<Expression>> sourceParameters;

    /**
     * Parameters for the destination specification entity if they are provided
     * in the connection and the destination entity is a parameterised interface
     * or a parameterised bare command or event.
     */
    private final Optional<LinkedList<Expression>> destinationParameters;

    /**
     * Initializes this object by storing values of given parameters in member
     * fields.
     *
     * @param destinationNode Node this edge points to.
     * @param sourceParameters Parameters of the source node.
     * @param destinationParameters Parameters of the destination node.
     * @throws NullPointerException One of the arguments is null.
     */
    WiringEdge(SpecificationElementNode destinationNode, Optional<LinkedList<Expression>> sourceParameters,
            Optional<LinkedList<Expression>> destinationParameters) {

        checkNotNull(destinationNode, "destination node cannot be null");
        checkNotNull(sourceParameters, "source parameters cannot be null");
        checkNotNull(destinationParameters, "destination parameters cannot be null");

        this.destinationNode = destinationNode;
        this.sourceParameters = sourceParameters;
        this.destinationParameters = destinationParameters;
    }

    /**
     * <p>Get the node this edge points to.</p>
     *
     * @return Node this edge points to.
     */
    public SpecificationElementNode getDestinationNode() {
        return destinationNode;
    }

    /**
     * <p>Parameters for the source specification entity if it is parameterised
     * and the connection has had the parameters.</p>
     *
     * @return Parameters of the source entity.
     */
    public Optional<LinkedList<Expression>> getSourceParameters() {
        return sourceParameters;
    }

    /**
     * <p>Parameters for the destination entity if it is parameterised and the
     * parameters were given in the connection.</p>
     *
     * @return Parameters for the destination entity.
     */
    public Optional<LinkedList<Expression>> getDestinationParameters() {
        return destinationParameters;
    }
}
