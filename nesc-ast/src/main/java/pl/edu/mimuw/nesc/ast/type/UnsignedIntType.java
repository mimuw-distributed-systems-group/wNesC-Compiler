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

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final Type addQualifiers(boolean addConst, boolean addVolatile,
                                    boolean addRestrict) {
        return new UnsignedIntType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final int getIntegerRank() {
        return IntType.INTEGER_RANK;
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
