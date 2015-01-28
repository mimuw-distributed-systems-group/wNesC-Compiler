package pl.edu.mimuw.nesc.constexpr.value.type;

import com.google.common.base.Supplier;
import com.google.common.collect.Range;
import java.math.BigInteger;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Type that represents an integer constant type.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class IntegerConstantType extends AbstractConstantType {
    /**
     * Count of bits used to represent values of this integer constant type.
     */
    private final int bitsCount;

    /**
     * Range of all values of this constant type.
     */
    private final Range<BigInteger> range;

    /**
     * The greatest value that can be represented with the given bits count in
     * the natural binary representation added to 1.
     */
    private final BigInteger unsignedBoundary;

    /**
     * Initialize this object by storing given values in member fields.
     *
     * @param bitsCount Count of bits for this type.
     * @param rangeSupplier Supplier of the range of values of this type.
     */
    protected IntegerConstantType(int bitsCount, Supplier<Range<BigInteger>> rangeSupplier) {
        checkArgument(bitsCount > 0, "count of bits cannot be non-positive");
        checkNotNull(rangeSupplier, "supplier of the range cannot be null");

        this.bitsCount = bitsCount;
        this.range = rangeSupplier.get();
        this.unsignedBoundary = BigInteger.valueOf(2L).pow(bitsCount);
    }

    /**
     * Get the bits count that is used to represent values of this type.
     *
     * @return Count of bits used to represent values of this constant type.
     */
    public int getBitsCount() {
        return bitsCount;
    }

    /**
     * Range that specifies all representable values in this type.
     *
     * @return Range of all values.
     */
    public Range<BigInteger> getRange() {
        return range;
    }

    /**
     * The greatest value that can be represented on the bits count of this type
     * in the natural binary representation added to 1.
     *
     * @return The unsigned boundary of this type.
     */
    public BigInteger getUnsignedBoundary() {
        return unsignedBoundary;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        final IntegerConstantType otherType = (IntegerConstantType) obj;
        return getBitsCount() == otherType.getBitsCount();
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(getBitsCount());
    }
}
