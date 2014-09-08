package pl.edu.mimuw.nesc.ast.type;

/**
 * Reflects the <code>long double</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class LongDoubleType extends FloatingType {
    public LongDoubleType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public LongDoubleType() {
        this(false, false);
    }

    @Override
    public final boolean isRealType() {
        return true;
    }

    @Override
    public final LongDoubleType addQualifiers(boolean addConst, boolean addVolatile,
                                              boolean addRestrict) {
        return new LongDoubleType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final LongDoubleType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                 boolean removeRestrict) {
        return new LongDoubleType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
