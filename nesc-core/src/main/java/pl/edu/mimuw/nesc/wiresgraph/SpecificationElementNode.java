package pl.edu.mimuw.nesc.wiresgraph;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import pl.edu.mimuw.nesc.ast.gen.Expression;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * <p>Class that represents a single node in the wires graph. It is associated
 * with a single command or event from a component, i.e. bare commands and
 * events are represented directly and for each command and event from each
 * interface reference there is a separate node.</p>
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
     * Name of the interface reference this command or event comes from.
     */
    private final Optional<String> interfaceRefName;

    /**
     * Name of this command or event.
     */
    private final String entityName;

    /**
     * Data for the generation of intermediate functions.
     */
    private final EntityData entityData;

    /**
     * List with all successors of this node, i.e. nodes that provide command or
     * event this node represents.
     */
    private final List<WiringEdge> successors = new ArrayList<>();

    /**
     * Unmodifiable view of the successors list.
     */
    private final List<WiringEdge> unmodifiableSuccessors = Collections.unmodifiableList(successors);

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
    SpecificationElementNode(String componentName, Optional<String> interfaceRefName, String entityName,
            EntityData entityData) {
        checkNotNull(componentName, "name of the component cannot be null");
        checkNotNull(interfaceRefName, "name of the interface reference cannot be null");
        checkNotNull(entityName, "name of the specification element cannot be null");
        checkNotNull(entityData, "entity data cannot be null");
        checkArgument(!componentName.isEmpty(), "name of the component cannot be an empty string");
        checkArgument(!interfaceRefName.isPresent() || !interfaceRefName.get().isEmpty(),
                "name of the interface reference cannot be null");
        checkArgument(!entityName.isEmpty(), "name of the specification element cannot be an empty string");

        this.componentName = componentName;
        this.interfaceRefName = interfaceRefName;
        this.entityName = entityName;
        this.entityData = entityData;
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
     * <p>Get the name of the interface reference this command or event comes
     * from.</p>
     *
     * @return Name of the interface reference if this command or event comes
     *         from an interface.
     */
    public Optional<String> getInterfaceRefName() {
        return interfaceRefName;
    }

    /**
     * <p>Get the name of the command or event represented by this node.</p>
     *
     * @return Name of the command or event.
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * <p>Get the data of the function that corresponds to this node.</p>
     *
     * @return Entity data object with information about the function that
     *         corresponds to this node.
     */
    public EntityData getEntityData() {
        return entityData;
    }

    /**
     * <p>Get the name of this node in the following format: "{0}[.{1}].{2}"
     * where {0} is the name of the component, {2} is the name of the command or
     * event and ".{1}" appears if and only if this element comes from an
     * interface.</p>
     *
     * @return Name of this node depicted above.
     */
    public String getName() {
        return interfaceRefName.isPresent()
                ? format("%s.%s.%s", componentName, interfaceRefName.get(), entityName)
                : format("%s.%s", componentName, entityName);
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
     * <p>Get the unmodifiable view of the list of successors of this node.</p>
     *
     * @return Unmodifiable view of list with successors of this node.
     */
    public List<WiringEdge> getSuccessors() {
        return unmodifiableSuccessors;
    }
}
