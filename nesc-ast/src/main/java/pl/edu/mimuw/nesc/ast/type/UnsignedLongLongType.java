package pl.edu.mimuw.nesc.ast.type;

/**
 * Reflects the <code>unsigned long long</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnsignedLongLongType extends UnsignedIntegerType {
    public UnsignedLongLongType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public UnsignedLongLongType() {
        this(false, false);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final UnsignedLongLongType addQualifiers(boolean addConst, boolean addVolatile,
                                                    boolean addRestrict) {
        return new UnsignedLongLongType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final UnsignedLongLongType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                       boolean removeRestrict) {
        return new UnsignedLongLongType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public final int getIntegerRank() {
        return LongLongType.INTEGER_RANK;
    }

    @Override
    public final LongLongType getSignedIntegerType() {
        return new LongLongType(isConstQualified(), isVolatileQualified());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
