package pl.edu.mimuw.nesc.ast.type;

import com.google.common.collect.Range;
import java.math.BigInteger;

/**
 * Reflects the <code>char</code> type. It is unspecified if it is signed or
 * unsigned.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class CharType extends IntegerType {
    public static final int INTEGER_RANK = 5;

    public CharType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    public CharType() {
        this(false, false);
    }

    @Override
    public final boolean isCharacterType() {
        return true;
    }

    @Override
    public final boolean isSignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isUnsignedIntegerType() {
        return false;
    }

    @Override
    public final CharType addQualifiers(boolean addConst, boolean addVolatile,
                                        boolean addRestrict) {
        return new CharType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final CharType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                           boolean removeRestrict) {
        return new CharType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile));
    }

    @Override
    public final int getIntegerRank() {
        return INTEGER_RANK;
    }

    @Override
    public final BigInteger getMinimumValue() {
        throw new UnsupportedOperationException("getting the minimum value is unsupported for type 'char'");
    }

    @Override
    public final BigInteger getMaximumValue() {
        throw new UnsupportedOperationException("getting the maximum value is unsupported for type 'char'");
    }

    @Override
    public final Range<BigInteger> getRange() {
        throw new UnsupportedOperationException("getting the range of values is unsupported for type 'char'");
    }

    @Override
    public final boolean isComplete() {
        return true;
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}