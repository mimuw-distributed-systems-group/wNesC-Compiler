package pl.edu.mimuw.nesc.ast.type;

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

    @Override
    public final boolean isFieldTagType() {
        return false;
    }

    @Override
    public final boolean isPointerType() {
        return false;
    }

    @Override
    public final boolean isArrayType() {
        return false;
    }

    @Override
    public final boolean isObjectType() {
        return true;
    }

    @Override
    public final boolean isFunctionType() {
        return false;
    }

    @Override
    public final boolean isUnknownType() {
        return false;
    }

    @Override
    public final boolean isUnknownArithmeticType() {
        return false;
    }

    @Override
    public final boolean isUnknownIntegerType() {
        return false;
    }

    @Override
    public final boolean isModifiable() {
        return isComplete() && !isConstQualified();
    }

    @Override
    public abstract ArithmeticType promote();

    @Override
    public final Type decay() {
        return this;
    }
}
