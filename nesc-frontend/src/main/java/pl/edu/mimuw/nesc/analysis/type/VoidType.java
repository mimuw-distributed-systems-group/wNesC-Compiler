package pl.edu.mimuw.nesc.analysis.type;

/**
 * Reflects the <code>void</code> type, the type that cannot be completed.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class VoidType extends AbstractType {
    public VoidType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    @Override
    public final boolean isUnsignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isSignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isIntegerType() {
        return false;
    }

    @Override
    public final boolean isScalarType() {
        return false;
    }

    @Override
    public final boolean isVoid() {
        return true;
    }

    @Override
    public final boolean isDerivedType() {
        return false;
    }

    @Override
    public final boolean isFloatingType() {
        return false;
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final boolean isArithmetic() {
        return false;
    }
}
