package pl.edu.mimuw.nesc.ast.type;

/**
 * Reflects the <code>float</code> type.
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class FloatType extends FloatingType {
    public FloatType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public FloatType() {
        this(false, false);
    }

    @Override
    public final boolean isRealType() {
        return true;
    }

    @Override
    public final FloatType addQualifiers(boolean addConst, boolean addVolatile,
                                         boolean addRestrict) {
        return new FloatType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final FloatType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                            boolean removeRestrict) {
        return new FloatType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
