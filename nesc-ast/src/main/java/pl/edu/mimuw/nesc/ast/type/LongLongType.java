package pl.edu.mimuw.nesc.ast.type;

/**
 * Reflects the <code>long long int</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class LongLongType extends SignedIntegerType {
    public static final int INTEGER_RANK = 25;

    public LongLongType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public LongLongType() {
        this(false, false);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final LongLongType addQualifiers(boolean addConst, boolean addVolatile,
                                            boolean addRestrict) {
        return new LongLongType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final LongLongType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                               boolean removeRestrict) {
        return new LongLongType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public final int getIntegerRank() {
        return INTEGER_RANK;
    }

    @Override
    public final UnsignedLongLongType getUnsignedIntegerType() {
        return new UnsignedLongLongType(isConstQualified(), isVolatileQualified());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
