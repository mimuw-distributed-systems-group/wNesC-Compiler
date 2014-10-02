package pl.edu.mimuw.nesc.ast.type;

import com.google.common.collect.Range;
import java.math.BigInteger;

/**
 * Reflects the <code>unsigned int</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnsignedIntType extends UnsignedIntegerType {
    public static final BigInteger MIN_VALUE = BigInteger.ZERO;
    public static final BigInteger MAX_VALUE = BigInteger.valueOf(65535L);
    public static final Range<BigInteger> RANGE = Range.closed(MIN_VALUE, MAX_VALUE);

    public UnsignedIntType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public UnsignedIntType() {
        this(false, false);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final UnsignedIntType addQualifiers(boolean addConst, boolean addVolatile,
                                               boolean addRestrict) {
        return new UnsignedIntType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final UnsignedIntType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                  boolean removeRestrict) {
        return new UnsignedIntType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public final int getIntegerRank() {
        return IntType.INTEGER_RANK;
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
    public final IntType getSignedIntegerType() {
        return new IntType(isConstQualified(), isVolatileQualified());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
