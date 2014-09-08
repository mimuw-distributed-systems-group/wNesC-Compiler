package pl.edu.mimuw.nesc.ast.type;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnsignedCharType extends UnsignedIntegerType {
    public UnsignedCharType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public UnsignedCharType() {
        this(false, false);
    }

    @Override
    public final boolean isCharacterType() {
        return true;
    }

    @Override
    public final UnsignedCharType addQualifiers(boolean addConst, boolean addVolatile,
                                                boolean addRestrict) {
        return new UnsignedCharType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final UnsignedCharType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                   boolean removeRestrict) {
        return new UnsignedCharType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public final int getIntegerRank() {
        return SignedCharType.INTEGER_RANK;
    }

    @Override
    public final SignedCharType getSignedIntegerType() {
        return new SignedCharType(isConstQualified(), isVolatileQualified());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
