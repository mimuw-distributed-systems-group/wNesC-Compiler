package pl.edu.mimuw.nesc.astutil;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>A simple helper class to carry information about the start location and
 * the end location of a language syntax element.
 * <code>Comparable<Interval></code> interface is implemented in a way that
 * the intervals that occur earlier are lesser.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class Interval implements Comparable<Interval> {
    private final Location startLocation;
    private final Location endLocation;

    /**
     * Get the builder for an interval.
     *
     * @return Newly created object that will create an interval.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Immediately create an interval.
     *
     * @param startLocation Start location of the interval.
     * @param endLocation End location of the interval
     * @return Newly created interval with given locations.
     * @throws NullPointerException One of the arguments is null.
     */
    public static Interval of(Location startLocation, Location endLocation) {
        return builder().startLocation(startLocation)
                .endLocation(endLocation)
                .build();
    }

    /**
     * Initializes this interval to store given locations.
     *
     * @throws NullPointerException One of the arguments is null.
     */
    private Interval(Builder builder) {
        this.startLocation = builder.startLocation;
        this.endLocation = builder.endLocation;
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

    @Override
    public int compareTo(Interval otherInterval) {
        checkNotNull(otherInterval, "the interval for comparison cannot be null");
        return getLocation().compareTo(otherInterval.getLocation());
    }

    /**
     * Builder for an interval.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder {
        /**
         * Data necessary to build an interval.
         */
        private Location startLocation;
        private Location endLocation;

        /**
         * Constructor accessible only for the <code>Interval</code> class.
         */
        private Builder() {
        }

        /**
         * Set the start location of the interval.
         *
         * @param startLocation Start location to set.
         * @return <code>this</code>
         */
        public Builder startLocation(Location startLocation) {
            this.startLocation = startLocation;
            return this;
        }

        /**
         * Set the end location of the interval.
         *
         * @param endLocation The end location to set.
         * @return <code>this</code>
         */
        public Builder endLocation(Location endLocation) {
            this.endLocation = endLocation;
            return this;
        }

        /**
         * Set the start location only if it has not been set previously.
         *
         * @param startLocation Start location to set if it has not been set
         *                      yet.
         * @return <code>this</code>
         */
        public Builder initStartLocation(Location startLocation) {
            this.startLocation = Optional.fromNullable(this.startLocation)
                    .or(Optional.fromNullable(startLocation))
                    .orNull();
            return this;
        }

        /**
         * Set the end location only if it has not been set yet.
         *
         * @param endLocation End location to use if it has not been set yet.
         * @return <code>this</code>
         */
        public Builder initEndLocation(Location endLocation) {
            this.endLocation = Optional.fromNullable(this.endLocation)
                    .or(Optional.fromNullable(endLocation))
                    .orNull();
            return this;
        }

        /**
         * Check if the builder is properly configured.
         *
         * @return <code>true</code> if and only if the interval can be built
         *         without any errors.
         */
        public boolean ready() {
            return startLocation != null && endLocation != null;
        }

        private void validate() {
            checkNotNull(startLocation, "start location cannot be null");
            checkNotNull(endLocation, "end location cannot be null");
        }

        /**
         * Build the interval.
         *
         * @return Creates the interval and returns it. If it is not possible,
         *         an exception is thrown.
         */
        public Interval build() {
            validate();
            return new Interval(this);
        }

        /**
         * Build the interval if it is possible.
         *
         * @return If the builder is properly configured, returns the created
         *         interval wrapped by <code>Optional</code>. Otherwise, the
         *         value is absent.
         */
        public Optional<Interval> tryBuild() {
            return   ready()
                   ? Optional.of(build())
                   : Optional.<Interval>absent();
        }
    }
}
