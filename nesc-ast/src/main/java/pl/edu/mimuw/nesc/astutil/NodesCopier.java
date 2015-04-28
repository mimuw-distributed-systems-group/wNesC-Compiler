package pl.edu.mimuw.nesc.astutil;

import com.google.common.base.Optional;
import java.util.LinkedList;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.gen.Node;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A utility class that offers copy-on-write operation for a list of nodes.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NodesCopier<T extends Node> {
    private final LinkedList<T> originalNodes;
    private Optional<LinkedList<T>> copy;

    public NodesCopier(LinkedList<T> originalNodes) {
        checkNotNull(originalNodes, "original nodes cannot be null");
        this.originalNodes = originalNodes;
        this.copy = Optional.absent();
    }

    /**
     * Get the list given at construction.
     *
     * @return Original list of nodes given at construction.
     */
    public LinkedList<T> getOriginalNodes() {
        return originalNodes;
    }

    /**
     * Copy the list given at construction if it hasn't already happened and
     * return it. If it has already happened, then the same copy is returned.
     *
     * @return The deep copy of the list given at construction. Always the same
     *         instance.
     */
    public LinkedList<T> getCopiedNodes() {
        if (!copy.isPresent()) {
            this.copy = Optional.of(AstUtils.deepCopyNodes(originalNodes,
                    true, Optional.<Map<Node, Node>>absent()));
        }
        return copy.get();
    }

    /**
     * Get the created copy. If it has not been created yet, the object is
     * absent.
     *
     * @return The copy of the original nodes if it has been created.
     */
    public Optional<LinkedList<T>> maybeCopy() {
        return copy;
    }
}
