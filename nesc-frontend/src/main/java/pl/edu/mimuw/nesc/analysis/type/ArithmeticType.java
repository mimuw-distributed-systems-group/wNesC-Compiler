package pl.edu.mimuw.nesc.analysis.type;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class ArithmeticType extends AbstractType {
    protected ArithmeticType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    @Override
    public final boolean isArithmetic() {
        return true;
    }

    @Override
    public final boolean isScalarType() {
        return true;
    }

    @Override
    public final boolean isVoid() {
        return false;
    }

    @Override
    public final boolean isDerivedType() {
        return false;
    }
}
