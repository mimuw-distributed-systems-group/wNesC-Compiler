package pl.edu.mimuw.nesc.ast.type;

import com.google.common.collect.Range;
import java.math.BigInteger;

/**
 * Reflects the <code>unsigned short int</code> type.
 */
public final class UnsignedShortType extends UnsignedIntegerType {
    public static final BigInteger MIN_VALUE = BigInteger.ZERO;
    public static final BigInteger MAX_VALUE = BigInteger.valueOf(65535L);
    public static final Range<BigInteger> RANGE = Range.closed(MIN_VALUE, MAX_VALUE);

    public UnsignedShortType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public UnsignedShortType() {
        this(false, false);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final UnsignedShortType addQualifiers(boolean addConst, boolean addVolatile,
                                                 boolean addRestrict) {
        return new UnsignedShortType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final UnsignedShortType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                    boolean removeRestrict) {
        return new UnsignedShortType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public final int getIntegerRank() {
        return ShortType.INTEGER_RANK;
    }

    @Override
    public final BigInteger getMinimumValue() {
        return MIN_VALUE;
    }

    @Override
    public final BigInteger getMaximumValue() {
        return MAX_VALUE;
    }

    @Override
    public final Range<BigInteger> getRange() {
        return RANGE;
    }

    @Override
    public final ShortType getSignedIntegerType() {
        return new ShortType(isConstQualified(), isVolatileQualified());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
