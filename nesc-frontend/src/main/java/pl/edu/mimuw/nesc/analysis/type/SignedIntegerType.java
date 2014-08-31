package pl.edu.mimuw.nesc.analysis.type;

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
}
