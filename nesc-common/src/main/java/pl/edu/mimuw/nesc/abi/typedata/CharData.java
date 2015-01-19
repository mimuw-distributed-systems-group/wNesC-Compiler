package pl.edu.mimuw.nesc.abi.typedata;

import com.google.common.collect.Range;
import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Structure with information about <code>char</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class CharData {
    private final boolean isSigned;
    private final Range<BigInteger> rangeSigned;
    private final Range<BigInteger> rangeUnsigned;

    public CharData(boolean isSigned, BigInteger signedMin, BigInteger signedMax,
                BigInteger unsignedMax) {
        checkNotNull(signedMin, "signed minimum value cannot be null");
        checkNotNull(signedMax, "signed maximum value cannot be null");
        checkNotNull(unsignedMax, "unsigned maximum value cannot be null");

        this.isSigned = isSigned;
        this.rangeSigned = Range.closed(signedMin, signedMax);
        this.rangeUnsigned = Range.closed(BigInteger.ZERO, unsignedMax);
    }

    /**
     * Check if <code>char</code> is a signed type.
     *
     * @return <code>true</code> if and only ic <code>char</code> is signed.
     */
    public boolean isSigned() {
        return isSigned;
    }

    /**
     * Get range of values of <code>char</code> type.
     *
     * @return Range of all values of <code>char</code> type.
     */
    public Range<BigInteger> getRange() {
        return isSigned
                ? rangeSigned
                : rangeUnsigned;
    }

    /**
     * Get range of values of <code>signed char</code> type.
     *
     * @return Range of all values of <code>signed char</code>.
     */
    public Range<BigInteger> getSignedRange() {
        return rangeSigned;
    }

    /**
     * Get range of values of <code>unsigned char</code> type.
     *
     * @return Range of all values of <code>unsigned char</code>.
     */
    public Range<BigInteger> getUnsignedRange() {
        return rangeUnsigned;
    }
}
