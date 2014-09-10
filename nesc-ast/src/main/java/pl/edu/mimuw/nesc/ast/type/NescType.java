package pl.edu.mimuw.nesc.ast.type;

/**
 * Artificial base type for NesC objects: interfaces and components.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class NescType implements Type {
    @Override
    public final boolean isArithmetic() {
        return false;
    }

    @Override
    public final boolean isIntegerType() {
        return false;
    }

    @Override
    public final boolean isSignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isUnsignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isFloatingType() {
        return false;
    }

    @Override
    public final boolean isRealType() {
        return false;
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final boolean isScalarType() {
        return false;
    }

    @Override
    public final boolean isVoid() {
        return false;
    }

    @Override
    public final boolean isDerivedType() {
        return false;
    }

    @Override
    public final boolean isFieldTagType() {
        return false;
    }

    @Override
    public final boolean isTypeDefinition() {
        return false;
    }

    @Override
    public final boolean isPointerType() {
        return false;
    }

    @Override
    public final boolean isArrayType() {
        return false;
    }

    @Override
    public final boolean isObjectType() {
        return false;
    }

    @Override
    public final boolean isFunctionType() {
        return false;
    }

    @Override
    public final Type addQualifiers(boolean constQualified,
            boolean volatileQualified, boolean restrictQualified) {
        throw new UnsupportedOperationException("adding qualifiers to an artificial type");
    }

    @Override
    public final Type removeQualifiers(boolean removeConst,
            boolean removeVolatile, boolean removeRestrict) {
        throw new UnsupportedOperationException("removing qualifiers from an artificial type");
    }

    @Override
    public final Type removeQualifiers() {
        return removeQualifiers(true, true, true);
    }

    @Override
    public final boolean isCompatibleWith(Type type) {
        throw new UnsupportedOperationException("checking the compatibility with an artificial type");
    }

    @Override
    public final Type promote() {
        throw new UnsupportedOperationException("promoting an artificial type");
    }

    @Override
    public final boolean isConstQualified() {
        return false;
    }

    @Override
    public final boolean isVolatileQualified() {
        return false;
    }

    @Override
    public final boolean isComplete() {
        return false;
    }

    @Override
    public final String toString() {
        final PrintVisitor visitor = new PrintVisitor();
        accept(visitor, false);
        return visitor.get();
    }
}
