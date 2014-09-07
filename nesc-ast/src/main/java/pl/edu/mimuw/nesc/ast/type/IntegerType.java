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

    @Override
    public final boolean isRealType() {
        return true;
    }

    /**
     * @return The integer rank of this type as defined in the ISO C standard.
     */
    public abstract int getIntegerRank();

    @Override
    public final Type promote() {
        final boolean properIntegerType = !(this instanceof IntType)
                && !(this instanceof UnsignedIntType);

        return   getIntegerRank() <= IntType.INTEGER_RANK && properIntegerType
               ? new IntType(isConstQualified(), isVolatileQualified())
               : this;
    }
}
