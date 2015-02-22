package pl.edu.mimuw.nesc.astutil;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import java.util.Collection;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Node;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that tests if a node fulfills a predicate and saves its location.
 * </p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class PredicateTester<T extends Node> {
    /**
     * Predicate that will be tested.
     */
    private final Predicate<T> predicate;

    /**
     * Locations of the node.
     */
    private Optional<Location> startLocation = Optional.absent();
    private Optional<Location> endLocation = Optional.absent();

    public PredicateTester(Predicate<T> predicate) {
        checkNotNull(predicate, "predicate cannot be null");
        this.predicate = predicate;
    }

    /**
     * Check if a node from the given collection fulfills the predicate and
     * store its location.
     *
     * @param nodes Collection with nodes to test.
     * @return <code>true</code> if and only if a node from the collection
     *         fulfills the predicate. In such case, its location is stored
     *         in the object and available by getter methods. Otherwise,
     *         locations are set to absent optional objects.
     */
    public boolean test(Collection<? extends T> nodes) {
        checkNotNull(nodes, "nodes cannot be null");

        for (T node : nodes) {
            if (predicate.apply(node)) {
                startLocation = Optional.of(node.getLocation());
                endLocation = Optional.of(node.getEndLocation());
                return true;
            }
        }

        startLocation = endLocation = Optional.absent();
        return false;
    }

    /**
     * Get the start location of the node that fulfilled the predicate in the
     * last test operation.
     *
     * @return Start location of the node that fulfilled the predicate in the
     *         last test operation.
     */
    public Optional<Location> getStartLocation() {
        return startLocation;
    }

    /**
     * Get the end location of the node that fulfilled the predicate in the
     * last test operation.
     *
     * @return End location of the node that fulfilled the predicate in the
     *         last test operation.
     */
    public Optional<Location> getEndLocation() {
        return endLocation;
    }
}
