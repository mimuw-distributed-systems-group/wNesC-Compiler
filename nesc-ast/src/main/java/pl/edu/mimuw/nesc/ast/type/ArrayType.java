package pl.edu.mimuw.nesc.ast.type;

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

    /**
     * @return <code>true</code> if and only if the size of an array this type
     *         represents is known (it has been explicitly given in the
     *         declaration).
     */
    public final boolean isOfKnownSize() {
        return ofKnownSize;
    }

    /**
     * @return Object that represents the element type of this array. Never
     *         null.
     */
    public final Type getElementType() {
        return elementType;
    }

    @Override
    public final boolean isScalarType() {
        return false;
    }

    @Override
    public final boolean isFieldTagType() {
        return false;
    }

    @Override
    public final ArrayType addQualifiers(boolean addConst, boolean addVolatile,
                                         boolean addRestrict) {
        return new ArrayType(getElementType().addQualifiers(addConst, addVolatile, addRestrict),
                             isOfKnownSize());
    }

    @Override
    public final ArrayType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                            boolean removeRestrict) {
        return new ArrayType(getElementType().removeQualifiers(removeConst, removeVolatile,
                             removeRestrict), isOfKnownSize());
    }

    @Override
    public final PointerType decay() {
        return new PointerType(getElementType());
    }

    @Override
    public final boolean isPointerType() {
        return false;
    }

    @Override
    public final boolean isArrayType() {
        return true;
    }

    @Override
    public final boolean isObjectType() {
        return true;
    }

    @Override
    public final boolean isFunctionType() {
        return false;
    }

    @Override
    public final boolean isModifiable() {
        return getElementType().isModifiable();
    }

    @Override
    public boolean isCompatibleWith(Type type) {
        if (!super.isCompatibleWith(type)) {
            return false;
        }

        final ArrayType arrayType = (ArrayType) type;
        return getElementType().isCompatibleWith(arrayType.getElementType());
    }

    @Override
    public final boolean isComplete() {
        return isOfKnownSize();
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
