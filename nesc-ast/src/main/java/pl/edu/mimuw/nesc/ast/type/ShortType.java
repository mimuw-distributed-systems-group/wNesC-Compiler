package pl.edu.mimuw.nesc.ast.type;

import com.google.common.collect.Range;
import java.math.BigInteger;

/**
 * Reflects the <code>short int</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ShortType extends SignedIntegerType {
    public static final int INTEGER_RANK = 10;
    public static final BigInteger MIN_VALUE = BigInteger.valueOf(-32767L);
    public static final BigInteger MAX_VALUE = BigInteger.valueOf(32767L);
    public static final Range<BigInteger> RANGE = Range.closed(MIN_VALUE, MAX_VALUE);

    public ShortType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public ShortType() {
        this(false, false);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final ShortType addQualifiers(boolean addConst, boolean addVolatile,
                                         boolean addRestrict) {
        return new ShortType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final ShortType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                            boolean removeRestrict) {
        return new ShortType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public final int getIntegerRank() {
        return INTEGER_RANK;
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
    public final UnsignedShortType getUnsignedIntegerType() {
        return new UnsignedShortType(isConstQualified(), isVolatileQualified());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
