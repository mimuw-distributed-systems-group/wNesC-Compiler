package pl.edu.mimuw.nesc.refsgraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import pl.edu.mimuw.nesc.ast.gen.Node;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that represents a C entity node that is an element of a graph.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class EntityNode {
    /**
     * Unique name of this entity.
     */
    private final String uniqueName;

    /**
     * Kind of this entity.
     */
    private final Kind kind;

    /**
     * List of entities referenced by this node.
     */
    private final List<Reference> successors;

    /**
     * List of entities that reference this entity.
     */
    private final List<Reference> predecessors;

    /**
     * Unmodifiable list of entities referenced by this node.
     */
    private final List<Reference> unmodifiableSuccessors;

    /**
     * Unmodifiable list of entities that reference this entity.
     */
    private final List<Reference> unmodifiablePredecessors;

    /**
     * Initialize this node with given name and kind and empty list of
     * successors and predecessors.
     *
     * @param uniqueName Unique name of this entity.
     * @param kind Kind of this entity.
     */
    EntityNode(String uniqueName, Kind kind) {
        checkNotNull(uniqueName, "unique name cannot be null");
        checkNotNull(kind, "kind of the entity cannot be null");
        checkArgument(!uniqueName.isEmpty(), "unique name cannot be an empty string");

        this.uniqueName = uniqueName;
        this.kind = kind;
        this.successors = new ArrayList<>();
        this.predecessors = new ArrayList<>();
        this.unmodifiableSuccessors = Collections.unmodifiableList(this.successors);
        this.unmodifiablePredecessors = Collections.unmodifiableList(this.predecessors);
    }

    /**
     * Get the unique name of this entity.
     *
     * @return Unique name of entity represented by this node.
     */
    public String getUniqueName() {
        return uniqueName;
    }

    /**
     * Get the kind of the entity represented by this node.
     *
     * @return Kind of this entity.
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Get the list of successors of this node. This node is the referencing
     * node in all references in the returned list.
     *
     * @return Unmodifiable list with successors of this node.
     */
    public List<Reference> getSuccessors() {
        return unmodifiableSuccessors;
    }

    /**
     * Get the list of predecessors of this node. This node is the referenced
     * node in all references in the returned list.
     *
     * @return Unmodifiable list with predecessors of this node.
     */
    public List<Reference> getPredecessors() {
        return unmodifiablePredecessors;
    }

    /**
     * Add a reference such that this node is the referencing node and the given
     * entity is the referenced entity. The reference is simultaneously added to
     * the list of predecessors of the referenced node.
     *
     * @param referencedEntity Entity referenced by this node.
     * @param referenceType Type of the reference.
     * @param insideNotEvaluatedExpr Value indicating if the reference occurs
     *                               in an expression that is not evaluated.
     * @param insideAtomic Value indicating if this reference occurs inside
     *                     a part of code that is executed atomically.
     */
    void addReference(EntityNode referencedEntity, Reference.Type referenceType,
            Node astNode, boolean insideNotEvaluatedExpr, boolean insideAtomic) {
        final Reference newReference = new Reference(this, referencedEntity,
                referenceType, astNode, insideNotEvaluatedExpr, insideAtomic);

        successors.add(newReference);
        referencedEntity.predecessors.add(newReference);
    }

    /**
     * Remove all edges to and from this node. After this operation the in and
     * out degrees of this node are equal to zero.
     */
    void removeAllEdges() {
        // Remove edges in successors
        for (Reference successorRef : successors) {
            final Iterator<Reference> predecessorsIt =
                    successorRef.getReferencedNode().predecessors.iterator();

            while (predecessorsIt.hasNext()) {
                if (this == predecessorsIt.next().getReferencingNode()) {
                    predecessorsIt.remove();
                }
            }
        }

        // Remove edges in predecessors
        for (Reference predecessorRef : predecessors) {
            final Iterator<Reference> successorsIt =
                    predecessorRef.getReferencingNode().successors.iterator();

            while (successorsIt.hasNext()) {
                if (this == successorsIt.next().getReferencedNode()) {
                    successorsIt.remove();
                }
            }
        }

        // Remove edges in this node
        successors.clear();
        predecessors.clear();
    }

    /**
     * <p>Enum type that represents kind of a C entity.</p>
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum Kind {
        FUNCTION,
        VARIABLE,
        TAG,
        TYPE_DEFINITION,
        CONSTANT,
    }
}
