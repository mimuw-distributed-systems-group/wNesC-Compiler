package pl.edu.mimuw.nesc.ast;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A simple helper class to carry information about the start location and
 * the end location of a language syntax element.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class Interval {
    private final Location startLocation;
    private final Location endLocation;

    /**
     * Initializes this interval to store given locations.
     *
     * @throws NullPointerException One of the arguments is null.
     */
    public Interval(Location startLoc, Location endLoc) {
        checkNotNull(startLoc, "the start location cannot be null");
        checkNotNull(endLoc, "the end location cannot be null");
        this.startLocation = startLoc;
        this.endLocation = endLoc;
    }

    /**
     * @return The start location contained in this object.
     */
    public Location getLocation() {
        return startLocation;
    }

    /**
     * @return The end location contained in this object.
     */
    public Location getEndLocation() {
        return endLocation;
    }
}