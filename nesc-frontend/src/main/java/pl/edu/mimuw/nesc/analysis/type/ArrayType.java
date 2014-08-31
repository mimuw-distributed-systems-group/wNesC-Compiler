package pl.edu.mimuw.nesc.analysis.type;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reflects an array type, e.g. <code>const int [2]</code>. However, the size of
 * the array is not available in those objects.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ArrayType extends DerivedType {
    /**
     * <code>true</code> if and only if the number of elements of this array
     * type is specified in the declarator.
     */
    public final boolean ofKnownSize;

    /**
     * Type of the elements that an array of this type contains.
     * Never null.
     */
    public final Type elementType;

    /**
     * Initializes this array type with given parameters.
     *
     * @throws NullPointerException The element type is null.
     */
    public ArrayType(Type elementType, boolean ofKnownSize) {
        super(false, false);
        checkNotNull(elementType, "element type of an array type cannot be null");
        this.ofKnownSize = ofKnownSize;
        this.elementType = elementType;
    }

    @Override
    public final boolean isScalarType() {
        return false;
    }
}
