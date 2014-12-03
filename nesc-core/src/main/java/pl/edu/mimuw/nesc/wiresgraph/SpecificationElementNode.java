package pl.edu.mimuw.nesc.wiresgraph;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import pl.edu.mimuw.nesc.ast.gen.Expression;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * <p>Class that represents a single node in the wires graph. It is associated
 * with a single specification element from a component.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SpecificationElementNode {
    /**
     * Name of the component the specification element represented by this node
     * comes from.
     */
    private final String componentName;

    /**
     * Name of this specification element in the component indicated by
     * {@link SpecificationElementNode#componentName}.
     */
    private final String entityName;

    /**
     * List with all successors of this node, i.e. nodes that provide interface
     * or bare command or event this node represents.
     */
    private final List<WiringEdge> successors = new ArrayList<>();

    /**
     * List with all predecessors of this node, i.e. nodes the use the interface
     * or bare command or event this node represents.
     */
    private final List<WiringEdge> predecessors = new ArrayList<>();

    /**
     * Unmodifiable view of the successors list.
     */
    private final List<WiringEdge> unmodifiableSuccessors = Collections.unmodifiableList(successors);

    /**
     * Unmodifiable view of the predecessors list.
     */
    private final List<WiringEdge> unmodifiablePredecessors = Collections.unmodifiableList(predecessors);

    /**
     * <p>Initializes this node by storing values from given parameters in
     * member fields.</p>
     *
     * @param componentName Name of the component the specification element
     *                      represented by this node comes from.
     * @param entityName Name of the specification element in the component.
     * @throws NullPointerException One of the arguments is null.
     * @throws IllegalArgumentException One of the arguments is an empty string.
     */
    SpecificationElementNode(String componentName, String entityName) {
        checkNotNull(componentName, "name of the component cannot be null");
        checkNotNull(entityName, "name of the specification element cannot be null");
        checkArgument(!componentName.isEmpty(), "name of the component cannot be an empty string");
        checkArgument(!entityName.isEmpty(), "name of the specification element cannot be an empty string");

        this.componentName = componentName;
        this.entityName = entityName;
    }

    /**
     * <p>Get the name of the component the specification element this node
     * represents comes from.</p>
     *
     * @return Name of the component.
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * <p>Get the name of the specification element this node represents in the
     * component.</p>
     *
     * @return Name of the specification element.
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * <p>Get the name of this node in the following format: "{0}.{1}" where {0}
     * is the name of the component and {1} is the name of the specification
     * element.</p>
     *
     * @return Name of this node depicted above.
     */
    public String getName() {
        return format("%s.%s", componentName, entityName);
    }

    /**
     * <p>Create and add a new edge to a successor of this node.</p>
     *
     * @param successor Successor of this node and the destination node of the
     *                  edge.
     * @param sourceParameters Parameters for this node.
     * @param destinationParameters Parameters for the destination node.
     */
    void addSuccessor(SpecificationElementNode successor, Optional<LinkedList<Expression>> sourceParameters,
            Optional<LinkedList<Expression>> destinationParameters) {

        final WiringEdge newEdge = new WiringEdge(successor, sourceParameters, destinationParameters);
        successors.add(newEdge);
    }

    /**
     * <p>Create and add a new edge to a predecessor of this node.</p>
     *
     * @param predecessor Predecessor of this node and simultaneously the
     *                    destination node of the edge to add.
     * @param sourceParameters Parameters for this node.
     * @param destinationParameters Parameters for the destination node.
     */
    void addPredecessor(SpecificationElementNode predecessor, Optional<LinkedList<Expression>> sourceParameters,
            Optional<LinkedList<Expression>> destinationParameters) {

        final WiringEdge newEdge = new WiringEdge(predecessor, sourceParameters, destinationParameters);
        predecessors.add(newEdge);
    }

    /**
     * <p>Get the unmodifiable view of the list of successors of this node.</p>
     *
     * @return Unmodifiable view of list with successors of this node.
     */
    public List<WiringEdge> getSuccessors() {
        return unmodifiableSuccessors;
    }

    /**
     * <p>Get the unmodifiable view of the list of predecessors of this
     * node.</p>
     *
     * @return Unmodifiable view of the list with predecessors of this node.
     */
    public List<WiringEdge> getPredecessors() {
        return unmodifiablePredecessors;
    }
}
