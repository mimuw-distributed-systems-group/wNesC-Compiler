package pl.edu.mimuw.nesc.type;

/**
 * Reflects the <code>void</code> type, the type that cannot be completed.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class VoidType extends AbstractType {
    public VoidType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public VoidType() {
        this(false, false);
    }

    @Override
    public final boolean isUnsignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isSignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isIntegerType() {
        return false;
    }

    @Override
    public final boolean isScalarType() {
        return false;
    }

    @Override
    public final boolean isVoid() {
        return true;
    }

    @Override
    public final boolean isDerivedType() {
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
    public final boolean isArithmetic() {
        return false;
    }

    @Override
    public final boolean isFieldTagType() {
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
        return true;
    }

    @Override
    public final boolean isFunctionType() {
        return false;
    }

    @Override
    public final boolean isUnknownType() {
        return false;
    }

    @Override
    public final boolean isUnknownArithmeticType() {
        return false;
    }

    @Override
    public final boolean isUnknownIntegerType() {
        return false;
    }

    @Override
    public final boolean isModifiable() {
        return false;
    }

    @Override
    public final VoidType addQualifiers(boolean addConst, boolean addVolatile,
                                        boolean addRestrict) {
        return new VoidType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final VoidType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                           boolean removeRestrict) {
        return new VoidType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public final Type promote() {
        return this;
    }

    @Override
    public final Type decay() {
        return this;
    }

    @Override
    public final boolean isComplete() {
        return false;
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
