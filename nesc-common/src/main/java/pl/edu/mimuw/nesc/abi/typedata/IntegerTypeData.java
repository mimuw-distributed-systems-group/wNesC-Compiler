package pl.edu.mimuw.nesc.abi.typedata;

import com.google.common.collect.Range;
import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Structure with information about an integer type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class IntegerTypeData extends TypeData {
    private final Range<BigInteger> range;

    public IntegerTypeData(int size, int alignment, BigInteger minimumValue, BigInteger maximumValue) {
        super(size, alignment);

        checkNotNull(minimumValue, "minimum value cannot be null");
        checkNotNull(maximumValue, "maximum value cannot be null");

        this.range = Range.closed(minimumValue, maximumValue);
    }

    /**
     * Get the range of value of the type.
     *
     * @return Range with all values that can be represented in the type.
     */
    public Range<BigInteger> getRange() {
        return range;
    }
}
