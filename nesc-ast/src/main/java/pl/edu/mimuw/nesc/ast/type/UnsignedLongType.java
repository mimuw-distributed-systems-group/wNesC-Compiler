package pl.edu.mimuw.nesc.ast.type;

/**
 * Reflects the <code>unsigned long</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnsignedLongType extends UnsignedIntegerType {
    public UnsignedLongType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public UnsignedLongType() {
        this(false, false);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final UnsignedLongType addQualifiers(boolean addConst, boolean addVolatile,
                                                boolean addRestrict) {
        return new UnsignedLongType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final UnsignedLongType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                   boolean removeRestrict) {
        return new UnsignedLongType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public final int getIntegerRank() {
        return LongType.INTEGER_RANK;
    }

    @Override
    public final LongType getSignedIntegerType() {
        return new LongType(isConstQualified(), isVolatileQualified());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
