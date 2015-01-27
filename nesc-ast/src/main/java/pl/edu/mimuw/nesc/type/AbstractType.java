package pl.edu.mimuw.nesc.type;

import pl.edu.mimuw.nesc.ast.gen.AstType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for all true types. All types can be const and volatile-qualified
 * and this class allows specifying it.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class AbstractType implements Type {
    /**
     * <code>true</code> if and only if this type is const-qualified.
     */
    private final boolean constQualified;

    /**
     * <code>true</code> if and only if this type is volatile-qualified.
     */
    private final boolean volatileQualified;

    /**
     * Copies values from parameters to internal variables.
     */
    protected AbstractType(boolean constQualified, boolean volatileQualified) {
        this.constQualified = constQualified;
        this.volatileQualified = volatileQualified;
    }

    /**
     * @return <code>true</code> if and only if this type is const-qualified or
     *         the qualifier is to be added.
     */
    protected final boolean addConstQualifier(boolean add) {
        return isConstQualified() || add;
    }

    /**
     * @return <code>true</code> if and only if this type is volatile-qualified
     *         or the qualifier it to be added.
     */
    protected final boolean addVolatileQualifier(boolean add) {
        return isVolatileQualified() || add;
    }

    /**
     * @return <code>true</code> if and only if this type is const-qualified and
     *         the qualifier is not to be removed.
     */
    protected final boolean removeConstQualifier(boolean remove) {
        return !remove && isConstQualified();
    }

    /**
     * @return <code>true</code> if and only if this type is volatile-qualified
     *         and the qualifier is not to be removed.
     */
    protected final boolean removeVolatileQualifier(boolean remove) {
        return !remove && isVolatileQualified();
    }

    @Override
    public Type addQualifiers(Type otherType) {
        // An override is present in 'PointerType'
        checkNotNull(otherType, "other type cannot be null");
        return addQualifiers(otherType.isConstQualified(), otherType.isVolatileQualified(), false);
    }

    @Override
    public final Type removeQualifiers() {
        return removeQualifiers(true, true, true);
    }

    @Override
    public final boolean isTypeDefinition() {
        return false;
    }

    @Override
    public final boolean isArtificialType() {
        return false;
    }

    @Override
    public final boolean isGeneralizedIntegerType() {
        return isIntegerType() || isUnknownIntegerType();
    }

    @Override
    public final boolean isGeneralizedArithmeticType() {
        return isArithmetic() || isUnknownArithmeticType();
    }

    @Override
    public final boolean isGeneralizedScalarType() {
        return isScalarType() || isUnknownArithmeticType();
    }

    @Override
    public final boolean isGeneralizedRealType() {
        return isRealType() || isUnknownArithmeticType();
    }

    @Override
    public final boolean isConstQualified() {
        return constQualified;
    }

    @Override
    public final boolean isVolatileQualified() {
        return volatileQualified;
    }

    @Override
    public boolean hasAllQualifiers(Type otherType) {
        checkNotNull(otherType, "the other type cannot be null");

        boolean result = hasAllBasicQualifiers(otherType);

        if (result && otherType.isPointerType()) {
            final PointerType ptrType = (PointerType) otherType;
            result = !ptrType.isRestrictQualified();
        }

        return result;
    }

    /**
     * Check if this type has all basic qualifiers that the given type has. The
     * basic qualifiers are <code>const</code> and <code>volatile</code>.
     *
     * @param otherType Type with qualifiers to compare to qualifiers of this
     *                  type.
     * @return <code>true</code> if and only if this type has all basic
     *         qualifiers that the given type has.
     */
    protected final boolean hasAllBasicQualifiers(Type otherType) {
        return (!otherType.isConstQualified() || isConstQualified())
                && (!otherType.isVolatileQualified() || isVolatileQualified());
    }

    @Override
    public boolean isCompatibleWith(Type type) {
        checkNotNull(type, "type checked for compatibility cannot be null");

        return getClass() == type.getClass()
               && isConstQualified() == type.isConstQualified()
               && isVolatileQualified() == type.isVolatileQualified();
    }

    @Override
    public final String toString() {
        final PrintVisitor visitor = new PrintVisitor();
        accept(visitor, false);
        return visitor.get();
    }

    @Override
    public final AstType toAstType() {
        final AstTypeBuildingVisitor visitor = new AstTypeBuildingVisitor();
        accept(visitor, null);
        return visitor.getAstType();
    }
}
