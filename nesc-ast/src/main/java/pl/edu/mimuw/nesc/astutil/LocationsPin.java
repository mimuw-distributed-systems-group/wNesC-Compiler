package pl.edu.mimuw.nesc.astutil;

import pl.edu.mimuw.nesc.ast.Location;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>A class whose objects are responsible for an easy storage of other objects
 * of an arbitrary type with information about their start and end locations.
 * </p>
 * <p>Objects of this type are supposed to behave exactly as objects of the
 * parameter type in collections, i.e. <code>equals</code> and
 * <code>hashCode</code> methods return the same value as the same methods from
 * the contained object (<code>equals</code> method performs a check for
 * <code>null</code> and a comparison of class and can return <code>false</code>
 * before calling <code>equals</code> on the contained object)</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class LocationsPin<T> {
    /**
     * Start location of the contained object. Never null.
     */
    private final Location startLocation;

    /**
     * End location of the contained object. Never null.
     */
    private final Location endLocation;

    /**
     * The contained object. Never null.
     */
    private final T object;

    /**
     * Creates an object that stores the given parameter and dummy locations.
     *
     * @param object Object that will be stored by the returned instance.
     * @return Newly created pin object that stores the given object and has
     *         locations set to the dummy location.
     * @throws NullPointerException Given argument is null.
     */
    public static <T> LocationsPin<T> withDummyLocations(T object) {
        return new LocationsPin<>(object, Location.getDummyLocation(),
                                  Location.getDummyLocation());
    }

    /**
     * Creates an object that carries data from given parameters.
     *
     * @param object Object that will be stored in the returned instance.
     * @param startLocation Start location that will be stored in the returned
     *                      instance.
     * @param endLocation End location that will be stored in the returned
     *                    instance.
     * @return Newly created pin object that stores data from arguments.
     * @throws NullPointerException One of the arguments is null.
     */
    public static <T> LocationsPin<T> of(T object, Location startLocation, Location endLocation) {
        return new LocationsPin<>(object, startLocation, endLocation);
    }

    /**
     * Initializes this object to store values from given arguments.
     *
     * @throws NullPointerException One of the arguments is null.
     */
    private LocationsPin(T object, Location startLocation, Location endLocation) {
        checkNotNull(object, "the object cannot be null");
        checkNotNull(startLocation, "the start location cannot be null");
        checkNotNull(endLocation, "the end location cannot be null");

        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.object = object;
    }

    /**
     * @return The start location of the contained object. Never null.
     */
    public Location getLocation() {
        return startLocation;
    }

    /**
     * @return The end location of the contained object. Never null.
     */
    public Location getEndLocation() {
        return endLocation;
    }

    /**
     * @return The contained object. Never null.
     */
    public T get() {
        return object;
    }

    @Override
    public int hashCode() {
        return object.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final LocationsPin<?> afterCast = (LocationsPin<?>) o;
        return object.equals(afterCast.object);
    }
}
