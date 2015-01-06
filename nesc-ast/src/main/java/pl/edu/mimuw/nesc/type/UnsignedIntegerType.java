package pl.edu.mimuw.nesc.type;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class UnsignedIntegerType extends IntegerType {
    protected UnsignedIntegerType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    @Override
    public final boolean isSignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isUnsignedIntegerType() {
        return true;
    }

    @Override
    public final boolean isComplete() {
        return true;
    }

    /**
     * @return Newly created object that represents the corresponding signed
     *         integer type with the same type qualifiers.
     */
    public abstract SignedIntegerType getSignedIntegerType();
}
