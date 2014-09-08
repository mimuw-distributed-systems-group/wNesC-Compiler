package pl.edu.mimuw.nesc.ast.type;

/**
 * Reflects the <code>unsigned int</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnsignedIntType extends UnsignedIntegerType {
    public UnsignedIntType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public UnsignedIntType() {
        this(false, false);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final UnsignedIntType addQualifiers(boolean addConst, boolean addVolatile,
                                               boolean addRestrict) {
        return new UnsignedIntType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final UnsignedIntType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                  boolean removeRestrict) {
        return new UnsignedIntType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public final int getIntegerRank() {
        return IntType.INTEGER_RANK;
    }

    @Override
    public final IntType getSignedIntegerType() {
        return new IntType(isConstQualified(), isVolatileQualified());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
