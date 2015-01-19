package pl.edu.mimuw.nesc.abi.typedata;

import com.google.common.collect.Range;
import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Structure that represents data about a standard integer type, e.g.
 * <code>int</code>.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class StandardIntegerTypeData extends TypeData {
    private final Range<BigInteger> rangeSigned;
    private final Range<BigInteger> rangeUnsigned;

    public StandardIntegerTypeData(int size, int alignment, BigInteger signedMinValue,
            BigInteger signedMaxValue, BigInteger unsignedMaxValue) {
        super(size, alignment);

        checkNotNull(signedMinValue, "signed minimum value cannot be null");
        checkNotNull(signedMaxValue, "signed maximum value cannot be null");
        checkNotNull(unsignedMaxValue, "unsigned maximum value cannot be null");

        this.rangeSigned = Range.closed(signedMinValue, signedMaxValue);
        this.rangeUnsigned = Range.closed(BigInteger.ZERO, unsignedMaxValue);
    }

    /**
     * Get range with all values of the signed variant of the type.
     *
     * @return Range with values of signed type variant.
     */
    public Range<BigInteger> getSignedRange() {
        return rangeSigned;
    }

    /**
     * Get range with all values of the unsigned variant of the type.
     *
     * @return Range with values of the unsigned variant.
     */
    public Range<BigInteger> getUnsignedRange() {
        return rangeUnsigned;
    }
}
