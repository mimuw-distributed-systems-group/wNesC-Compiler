package pl.edu.mimuw.nesc.ast.type;

/**
 * Reflects the <code>double</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class DoubleType extends FloatingType {
    public DoubleType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public DoubleType() {
        this(false, false);
    }

    @Override
    public final boolean isRealType() {
        return true;
    }

    @Override
    public final DoubleType addQualifiers(boolean addConst, boolean addVolatile,
                                          boolean addRestrict) {
        return new DoubleType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final DoubleType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                           boolean removeRestrict) {
        return new DoubleType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
