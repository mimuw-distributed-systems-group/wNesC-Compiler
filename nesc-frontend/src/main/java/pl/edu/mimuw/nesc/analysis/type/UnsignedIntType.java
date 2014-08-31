package pl.edu.mimuw.nesc.analysis.type;

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
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
