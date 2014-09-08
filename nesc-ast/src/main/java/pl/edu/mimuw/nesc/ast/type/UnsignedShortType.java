package pl.edu.mimuw.nesc.ast.type;

/**
 * Reflects the <code>unsigned short int</code> type.
 */
public final class UnsignedShortType extends UnsignedIntegerType {
    public UnsignedShortType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public UnsignedShortType() {
        this(false, false);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final UnsignedShortType addQualifiers(boolean addConst, boolean addVolatile,
                                                 boolean addRestrict) {
        return new UnsignedShortType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final UnsignedShortType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                    boolean removeRestrict) {
        return new UnsignedShortType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public final int getIntegerRank() {
        return ShortType.INTEGER_RANK;
    }

    @Override
    public final ShortType getSignedIntegerType() {
        return new ShortType(isConstQualified(), isVolatileQualified());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
