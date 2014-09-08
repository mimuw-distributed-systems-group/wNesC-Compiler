package pl.edu.mimuw.nesc.ast.type;

/**
 * Reflects the <code>short int</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ShortType extends SignedIntegerType {
    public static final int INTEGER_RANK = 10;

    public ShortType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public ShortType() {
        this(false, false);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final ShortType addQualifiers(boolean addConst, boolean addVolatile,
                                         boolean addRestrict) {
        return new ShortType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final ShortType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                            boolean removeRestrict) {
        return new ShortType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public final int getIntegerRank() {
        return INTEGER_RANK;
    }

    @Override
    public final UnsignedShortType getUnsignedIntegerType() {
        return new UnsignedShortType(isConstQualified(), isVolatileQualified());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
