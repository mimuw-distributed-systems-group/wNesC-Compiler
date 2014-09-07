package pl.edu.mimuw.nesc.ast.type;

/**
 * Reflects the <code>unsigned short int</code> type.
 */
public final class UnsignedShortType extends UnsignedIntegerType {
    public UnsignedShortType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final Type addQualifiers(boolean addConst, boolean addVolatile,
                                    boolean addRestrict) {
        return new UnsignedShortType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final int getIntegerRank() {
        return ShortType.INTEGER_RANK;
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
