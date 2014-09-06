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

    @Override
    public final Type addQualifiers(boolean addConst, boolean addVolatile,
                                    boolean addRestrict) {
        return new LongDoubleType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
