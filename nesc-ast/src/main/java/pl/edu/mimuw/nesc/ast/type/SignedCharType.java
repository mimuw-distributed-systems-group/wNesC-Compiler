package pl.edu.mimuw.nesc.ast.type;

import com.google.common.collect.Range;
import java.math.BigInteger;

/**
 * Reflects the <code>signed char</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SignedCharType extends SignedIntegerType {
    public static final int INTEGER_RANK = 5;
    public static final BigInteger MIN_VALUE = BigInteger.valueOf(-127L);
    public static final BigInteger MAX_VALUE = BigInteger.valueOf(127L);
    public static final Range<BigInteger> RANGE = Range.closed(MIN_VALUE, MAX_VALUE);

    public SignedCharType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public SignedCharType() {
        this(false, false);
    }

    @Override
    public final boolean isCharacterType() {
        return true;
    }

    @Override
    public final SignedCharType addQualifiers(boolean addConst, boolean addVolatile,
                                              boolean addRestrict) {
        return new SignedCharType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final SignedCharType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                 boolean removeRestrict) {
        return new SignedCharType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
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
    public final UnsignedCharType getUnsignedIntegerType() {
        return new UnsignedCharType(isConstQualified(), isVolatileQualified());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
