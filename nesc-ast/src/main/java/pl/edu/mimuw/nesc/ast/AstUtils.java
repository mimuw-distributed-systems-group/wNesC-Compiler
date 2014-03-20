package pl.edu.mimuw.nesc.ast;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.common.util.list.Lists;

import java.util.LinkedList;

/**
 * <h1>Locations</h1>
 * <p>Methods for extracting locations are provided. They are
 * useful especially when one want to retrieve start/end location of some
 * language construct but the corresponding production consists of
 * several tokens list that are likely to be empty or some tokens are
 * optional.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class AstUtils {

    /**
     * Returns start location of nodes in the list.
     *
     * @param nodesList nodes list
     * @param <T>       type of nodes
     * @return <code>absent</code> of start location of the first token
     */
    public static <T extends Node> Optional<Location> getStartLocation(LinkedList<T> nodesList) {
        if (nodesList.isEmpty()) {
            return Optional.absent();
        }
        return Optional.of(nodesList.getFirst().getLocation());
    }

    /**
     * <p>Return the minimum start location among all given node lists or
     * returns <code>absent</code>, when all lists are empty.</p>
     * <p>Lists are search from the first to the last one.</p>
     *
     * @param nodesList1 first nodes list
     * @param nodesList2 second nodes list
     * @param <T>        type of nodes list
     * @param <V>        type of nodes list
     * @return <code>absent</code> of start location of the first token
     */
    public static <T extends Node, V extends Node> Optional<Location> getStartLocation(
            LinkedList<T> nodesList1, LinkedList<V> nodesList2) {
        final Optional<Location> startLocation1 = getStartLocation(nodesList1);
        if (startLocation1.isPresent()) {
            return startLocation1;
        }
        final Optional<Location> startLocation2 = getStartLocation(nodesList2);
        if (startLocation2.isPresent()) {
            return startLocation2;
        }
        return Optional.absent();
    }

    /**
     * <p>Return the minimum start location in given node list or
     * returns the specified location, when the list is empty.</p>
     *
     * @param location  the location of the last token in production,
     *                  that is guaranteed to appear
     * @param nodesList nodes list
     * @param <T>       type of nodes list
     * @return start location
     */
    public static <T extends Node> Location getStartLocation(Location location, LinkedList<T> nodesList) {
        return getStartLocation(location, nodesList, Lists.<Node>newList());
    }

    /**
     * <p>Return the minimum start location among all given node lists or
     * returns the specified location, when all lists are empty.</p>
     * <p>Lists are search from the first to the last one.</p>
     *
     * @param location   the location of the last token in production,
     *                   that is guaranteed to appear
     * @param nodesList1 first nodes list
     * @param nodesList2 second nodes list
     * @param <T>        type of nodes list
     * @param <V>        type of nodes list
     * @return start location
     */
    public static <T extends Node, V extends Node> Location getStartLocation(Location location,
                                                                             LinkedList<T> nodesList1,
                                                                             LinkedList<V> nodesList2) {
        final Optional<Location> startLocation1 = getStartLocation(nodesList1);
        if (startLocation1.isPresent()) {
            return startLocation1.get();
        }
        final Optional<Location> startLocation2 = getStartLocation(nodesList2);
        if (startLocation2.isPresent()) {
            return startLocation2.get();
        }
        return location;
    }

    /**
     * <p>Returns end location of the last element of AST nodes list.</p>
     *
     * @param nodesList nodes list
     * @param <T>       type of nodes
     * @return <code>absent</code> when list is empty,
     * otherwise the end location of the last element of the list
     */
    public static <T extends Node> Optional<Location> getEndLocation(LinkedList<T> nodesList) {
        if (nodesList.isEmpty()) {
            return Optional.absent();
        }
        return Optional.of(nodesList.getLast().getEndLocation());
    }

    /**
     * <p>Return the maximum end location among all specified nodes list or
     * returns <code>absent</code>, when all lists are empty.</p>
     *
     * @param nodesList1 first nodes list
     * @param nodesList2 second nodes list
     * @param <T>        type of nodes list
     * @param <V>        type of nodes list
     * @return the maximum end location
     */
    public static <T extends Node, V extends Node> Optional<Location> getEndLocation(
            LinkedList<T> nodesList1, LinkedList<V> nodesList2) {
        final Optional<Location> endLocation2 = getEndLocation(nodesList2);
        if (endLocation2.isPresent()) {
            return endLocation2;
        }
        final Optional<Location> endLocation1 = getEndLocation(nodesList1);
        if (endLocation1.isPresent()) {
            return endLocation1;
        }
        return Optional.absent();
    }

    /**
     * Returns the maximum of the two end locations.
     *
     * @param endLocation         end location
     * @param endLocationOptional optional end location
     * @return the maximum of the two end locations
     */
    public static Location getEndLocation(Location endLocation, Optional<Location> endLocationOptional) {
        if (endLocationOptional.isPresent()) {
            return endLocationOptional.get();
        }
        return endLocation;
    }

    /**
     * <p>Return the maximum end location among given node lists or
     * returns the specified location, when all lists are empty.</p>
     *
     * @param location  the location of the last token in production,
     *                  that is guaranteed to appear
     * @param nodesList first nodes list
     * @param <T>       type of nodes list
     * @return the maximum end location
     */
    public static <T extends Node> Location getEndLocation(Location location, LinkedList<T> nodesList) {
        return getEndLocation(location, nodesList, Lists.<Node>newList(), Lists.<Node>newList());
    }

    /**
     * <p>Return the maximum end location among all specified nodes list or
     * returns the specified location, when all lists are empty.</p>
     *
     * @param location   the location of the last token in production,
     *                   that is guaranteed to appear
     * @param nodesList1 first nodes list
     * @param nodesList2 second nodes list
     * @param <T>        type of nodes list
     * @param <V>        type of nodes list
     * @return the maximum end location
     */
    public static <T extends Node, V extends Node> Location getEndLocation(
            Location location, LinkedList<T> nodesList1, LinkedList<V> nodesList2) {
        return getEndLocation(location, nodesList1, nodesList2, Lists.<Node>newList());
    }

    /**
     * <p>Return the maximum end location among all specified nodes list or
     * returns the specified location, when all lists are empty.</p>
     *
     * @param location   the location of the last token in production,
     *                   that is guaranteed to appear
     * @param nodesList1 first nodes list
     * @param nodesList2 second nodes list
     * @param nodesList3 last nodes list
     * @param <T>        type of nodes list
     * @param <V>        type of nodes list
     * @param <U>        type of nodes list
     * @return the maximum end location
     */
    public static <T extends Node, V extends Node, U extends Node> Location getEndLocation(
            Location location, LinkedList<T> nodesList1, LinkedList<V> nodesList2, LinkedList<U> nodesList3) {
        final Optional<Location> endLocation3 = getEndLocation(nodesList3);
        if (endLocation3.isPresent()) {
            return endLocation3.get();
        }
        final Optional<Location> endLocation2 = getEndLocation(nodesList2);
        if (endLocation2.isPresent()) {
            return endLocation2.get();
        }
        final Optional<Location> endLocation1 = getEndLocation(nodesList1);
        if (endLocation1.isPresent()) {
            return endLocation1.get();
        }
        return location;
    }

    private AstUtils() {
    }
}
