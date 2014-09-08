package pl.edu.mimuw.nesc.ast.type;

/**
 * Reflects the <code>signed char</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SignedCharType extends SignedIntegerType {
    public static final int INTEGER_RANK = 5;

    public SignedCharType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public SignedCharType() {
        this(false, false);
    }

    @Override
    public final boolean isCharacterType() {
        return true;
    }

    @Override
    public final SignedCharType addQualifiers(boolean addConst, boolean addVolatile,
                                              boolean addRestrict) {
        return new SignedCharType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final SignedCharType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                 boolean removeRestrict) {
        return new SignedCharType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public final int getIntegerRank() {
        return INTEGER_RANK;
    }

    @Override
    public final UnsignedCharType getUnsignedIntegerType() {
        return new UnsignedCharType(isConstQualified(), isVolatileQualified());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
