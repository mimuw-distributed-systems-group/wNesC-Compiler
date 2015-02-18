package pl.edu.mimuw.nesc.wiresgraph;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import java.math.BigInteger;

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
     * Count of indices for this specification element. If it is not a command
     * or event from a parameterised interface or a parameterised bare command
     * or event, then it is equal to zero.
     */
    private final int indicesCount;

    /**
     * Data for the generation of intermediate functions.
     */
    private final EntityData entityData;

    /**
     * Multimap with successors of this node. Absent object as the key
     * represents connections without parameters specified for this node.
     */
    private final ListMultimap<Optional<ImmutableList<BigInteger>>, IndexedNode> successors = ArrayListMultimap.create();

    /**
     * Unmodifiable view of the successors multimap.
     */
    private final ListMultimap<Optional<ImmutableList<BigInteger>>, IndexedNode> unmodifiableSuccessors =
            Multimaps.unmodifiableListMultimap(successors);

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
            int indicesCount, EntityData entityData) {
        checkNotNull(componentName, "name of the component cannot be null");
        checkNotNull(interfaceRefName, "name of the interface reference cannot be null");
        checkNotNull(entityName, "name of the specification element cannot be null");
        checkNotNull(entityData, "entity data cannot be null");
        checkArgument(!componentName.isEmpty(), "name of the component cannot be an empty string");
        checkArgument(!interfaceRefName.isPresent() || !interfaceRefName.get().isEmpty(),
                "name of the interface reference cannot be null");
        checkArgument(!entityName.isEmpty(), "name of the specification element cannot be an empty string");
        checkArgument(indicesCount >= 0, "count of indices cannot be negative");

        this.componentName = componentName;
        this.interfaceRefName = interfaceRefName;
        this.entityName = entityName;
        this.indicesCount = indicesCount;
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
     * <p>Get the count of indices of this specification element node if it
     * represents a command or event from a parameterised interface or
     * a bare parameterised command or event.</p>
     *
     * @return Count of indices.
     */
    public int getIndicesCount() {
        return indicesCount;
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
     * <p>Get the full name of the specification element this node is associated
     * with in the following format: "{0}.{1}" where {0} is the name of the
     * component and {1} is the name of the specification element.</p>
     *
     * @return Name of the specification element depicted above.
     */
    public String getSpecificationElementFullName() {
        return format("%s.%s", componentName, interfaceRefName.or(entityName));
    }

    /**
     * <p>Create and add a new edge to a successor of this node.</p>
     *
     * @param destinationNode Successor of this node.
     * @param sourceIndices Indices for this node.
     * @param destinationIndices Indices for the destination node.
     */
    void addSuccessor(SpecificationElementNode destinationNode, Optional<ImmutableList<BigInteger>> sourceIndices,
            Optional<ImmutableList<BigInteger>> destinationIndices) {

        // Validate the edge

        checkNotNull(destinationNode, "destination node cannot be null");
        checkNotNull(sourceIndices, "source indices cannot be null");
        checkNotNull(destinationIndices, "destination indices cannot be null");
        checkArgument(!sourceIndices.isPresent() || !sourceIndices.get().isEmpty(),
                "source indices cannot be an empty list");

        if (sourceIndices.isPresent() && sourceIndices.get().size() != indicesCount) {
            throw new IllegalArgumentException("invalid count of source indices, expected "
                    + indicesCount + " but got " + sourceIndices.get().size());
        }

        // Create and add the successor

        final IndexedNode newSuccessor = new IndexedNode(destinationNode, destinationIndices);
        successors.put(sourceIndices, newSuccessor);
    }

    /**
     * <p>Get the unmodifiable view of the successors map of this node.</p>
     *
     * @return Unmodifiable view of map with successors of this node.
     */
    public ListMultimap<Optional<ImmutableList<BigInteger>>, IndexedNode> getSuccessors() {
        return unmodifiableSuccessors;
    }
}
