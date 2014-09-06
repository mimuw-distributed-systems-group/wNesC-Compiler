package pl.edu.mimuw.nesc.ast.type;

/**
 * Reflects the <code>char</code> type. It is unspecified if it is signed or
 * unsigned.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class CharType extends IntegerType {
    public CharType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    @Override
    public final boolean isCharacterType() {
        return true;
    }

    @Override
    public final boolean isSignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isUnsignedIntegerType() {
        return false;
    }

    @Override
    public final Type addQualifiers(boolean addConst, boolean addVolatile,
                                    boolean addRestrict) {
        return new CharType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
