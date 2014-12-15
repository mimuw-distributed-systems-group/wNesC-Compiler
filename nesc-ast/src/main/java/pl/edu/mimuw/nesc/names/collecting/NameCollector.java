package pl.edu.mimuw.nesc.names.collecting;

import java.util.Collection;
import pl.edu.mimuw.nesc.ast.gen.Node;

/**
 * <p>Interface with operations related to collecting names related to some AST
 * nodes.</p>
 *
 * @param <T> Node type with names that will be collected.
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface NameCollector<T extends Node> {
    /**
     * Adds name of the given object to the internal collection of names.
     *
     * @param object Object with a name to collect.
     * @throws NullPointerException Given object is <code>null</code>.
     */
    void collect(T object);

    /**
     * Adds names of entities from the given collection to the internal
     * set of saved names. If some objects in the collection are not instances
     * of class <code>T</code>, they are ignored.
     *
     * @param objects Collection of objects that potentially contains objects
     *                whose names are collected by this collector.
     * @throws NullPointerException The objects collection is <code>null</code>.
     * @throws IllegalArgumentException One of the objects in the collection is
     *                                  <code>null</code>.
     */
    void collect(Collection<? extends Node> objects);

    /**
     * Get the collection with names currently collected by this collector.
     *
     * @return Collection with all currently gathered names.
     */
    Collection<String> get();
}
