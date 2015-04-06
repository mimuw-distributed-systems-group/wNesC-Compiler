package pl.edu.mimuw.nesc.astutil;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Node;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that tests if a node fulfills a predicate and saves references to
 * all nodes that fulfill it.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class PredicateTester<T extends Node> {
    /**
     * Predicate that will be tested.
     */
    private final Predicate<T> predicate;

    /**
     * List of nodes that fulfill the predicate.
     */
    private final List<T> fulfillingNodes;

    /**
     * Unmodifiable view of the fulfilling nodes list.
     */
    private final List<T> unmodifiableFulfillingNodes;

    public PredicateTester(Predicate<T> predicate) {
        checkNotNull(predicate, "predicate cannot be null");
        this.predicate = predicate;
        this.fulfillingNodes = new ArrayList<>();
        this.unmodifiableFulfillingNodes = Collections.unmodifiableList(fulfillingNodes);
    }

    /**
     * Check if a node from the given collection fulfills the predicate and
     * store its location.
     *
     * @param nodes Iterable with nodes to test.
     * @return <code>true</code> if and only if a node from the collection
     *         fulfills the predicate. During this call a list of nodes from
     *         the iterable that fulfill the predicate is created.
     */
    public boolean test(Iterable<? extends T> nodes) {
        checkNotNull(nodes, "nodes cannot be null");

        fulfillingNodes.clear();

        for (T node : nodes) {
            if (predicate.apply(node)) {
                fulfillingNodes.add(node);
            }
        }

        return !fulfillingNodes.isEmpty();
    }

    /**
     * Get the list of nodes that fulfilled the predicate in the last test
     * operation. The returned list is unmodifiable.
     *
     * @return Unmodifiable list with nodes that fulfilled the predicate in the
     *         last test operation.
     */
    public List<T> getFulfillingNodes() {
        return unmodifiableFulfillingNodes;
    }

    /**
     * Get the interval of the first node that fulfilled the predicate in the
     * last test operation.
     *
     * @return Interval that specifies location of the node that fulfilled the
     *         last test operation.
     */
    public Optional<Interval> getFirstInterval() {
        if (!fulfillingNodes.isEmpty()) {
            final T first = fulfillingNodes.get(0);
            return Optional.of(Interval.of(first.getLocation(), first.getEndLocation()));
        } else {
            return Optional.absent();
        }
    }
}
