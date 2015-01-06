package pl.edu.mimuw.nesc.type;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class SignedIntegerType extends IntegerType {
    protected SignedIntegerType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    @Override
    public final boolean isSignedIntegerType() {
        return true;
    }

    @Override
    public final boolean isUnsignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isComplete() {
        return true;
    }

    /**
     * @return Newly created object that represents the corresponding unsigned
     *         integer type (with the same type qualifiers as this type).
     */
    public abstract UnsignedIntegerType getUnsignedIntegerType();
}
