package pl.edu.mimuw.nesc.constexpr.value.type;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface ConstantType {
    /**
     * Get the enumeration constant associated with this type.
     *
     * @return Enumeration constant that allows identifying the class of this
     *         type.
     */
    Type getType();

    /**
     * Check if this type is equal to the given object. If the other object
     * is a type, <code>true</code> shall be returned if and only if it is
     * the same type.
     *
     * @param obj Other object.
     * @return <code>true</code> if and only if the given object is the same
     *         type as this (not necessarily the same instance as this).
     */
    @Override
    boolean equals(Object obj);

    /**
     * The method shall be overridden if {@link ConstantType#equals} is
     * implemented and hence this method is included in this interface.
     *
     * @return The hash code as specified in {@link Object#hashCode}.
     */
    @Override
    int hashCode();

    /**
     * Enumeration type that represents a general type of a constant value.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum Type {
        UNSIGNED_INTEGER,
        SIGNED_INTEGER,
        FLOAT,
        DOUBLE,
    }
}
