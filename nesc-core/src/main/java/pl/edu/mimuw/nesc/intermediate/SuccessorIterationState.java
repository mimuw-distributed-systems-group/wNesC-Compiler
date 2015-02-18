package pl.edu.mimuw.nesc.intermediate;

import com.google.common.base.Optional;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
abstract class SuccessorIterationState<T> {
    /**
     * Wrapper of the node whose successors are iterated.
     */
    private final NodeWrapper nodeWrapper;

    /**
     * Iterator with the current state of the iteration.
     */
    private final Iterator<T> iterator;

    SuccessorIterationState(NodeWrapper nodeWrapper, Iterator<T> iterator) {
        checkNotNull(nodeWrapper, "node wrapper cannot be null");
        checkNotNull(iterator, "iterator cannot be null");
        this.nodeWrapper = nodeWrapper;
        this.iterator = iterator;
    }

    /**
     * Get the wrapper of the node whose successors are returned by the iterator
     * in this object.
     *
     * @return Wrapper of the node.
     */
    NodeWrapper getNodeWrapper() {
        return nodeWrapper;
    }

    /**
     * Method that allows usage of the visitor pattern.
     *
     * @param visitor Visitor that will visit this node.
     * @param arg Argument for the visitor method invocation.
     * @return Value returned by the given visitor after invocation of its
     *         method.
     */
    abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

    /**
     * Get the next element of the iteration.
     *
     * @return Next element of the iteration. The value is absent if there are
     *         no more elements.
     */
    Optional<T> nextElement() {
        return iterator.hasNext()
                ? Optional.of(iterator.next())
                : Optional.<T>absent();
    }

    /**
     * Interface for the visitor pattern.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    interface Visitor<R, A> {
        R visit(FilteredIterationState state, A arg);
        R visit(FullIterationState state, A arg);
    }
}
