package pl.edu.mimuw.nesc.analysis.type;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reflects a pointer type, e.g. <code>const int * restrict</code>.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class PointerType extends DerivedType {
    /**
     * <code>true</code> if and only if this pointer type is restrict-qualified.
     * Only pointer types can be restrict-qualified.
     */
    private final boolean isRestrictQualified;

    /**
     * The type of the value that is referenced by this pointer. Never null.
     */
    private final Type referencedType;

    /**
     * Initializes the object with given parameters.
     *
     * @param referencedType Type that the pointer points to.
     * @throws NullPointerException <code>referencedType</code> is null.
     */
    public PointerType(boolean constQualified, boolean volatileQualified,
                       boolean restrictQualified, Type referencedType) {
        super(constQualified, volatileQualified);
        checkNotNull(referencedType, "the referenced type cannot be null");
        this.isRestrictQualified = restrictQualified;
        this.referencedType = referencedType;
    }

    /**
     * @return <code>true</code> if and only if this pointer type is
     *         restrict-qualified.
     */
    public final boolean isRestrictQualified() {
        return isRestrictQualified;
    }

    @Override
    public final boolean isScalarType() {
        return true;
    }
}
