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

    @Override
    public final Type addQualifiers(boolean addConst, boolean addVolatile,
                                    boolean addRestrict) {
        return new DoubleType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
