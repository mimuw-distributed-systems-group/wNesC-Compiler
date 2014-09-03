package pl.edu.mimuw.nesc.ast.type;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class IntegerType extends ArithmeticType {
    protected IntegerType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    @Override
    public final boolean isIntegerType() {
        return true;
    }

    @Override
    public final boolean isFloatingType() {
        return false;
    }
}
