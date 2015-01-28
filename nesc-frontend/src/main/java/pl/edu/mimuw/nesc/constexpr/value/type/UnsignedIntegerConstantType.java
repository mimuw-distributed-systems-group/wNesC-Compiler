package pl.edu.mimuw.nesc.constexpr.value.type;

import com.google.common.base.Supplier;
import com.google.common.collect.Range;
import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * <p>An unsigned integer type. Values of this type are computed using the
 * modulo arithmetic.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnsignedIntegerConstantType extends IntegerConstantType {
    public UnsignedIntegerConstantType(int bitsCount) {
        super(bitsCount, new UnsignedRangeSupplier(bitsCount));
    }

    @Override
    public Type getType() {
        return Type.UNSIGNED_INTEGER;
    }

    /**
     * Class responsible for creating a range of values of usigned integer
     * constants types. It is the range of values that can be represented
     * using the given count of bits in natural binary representation.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class UnsignedRangeSupplier implements Supplier<Range<BigInteger>> {
        private final int bitsCount;

        private UnsignedRangeSupplier(int bitsCount) {
            checkArgument(bitsCount >= 1, "bits count cannot be non-positive");
            this.bitsCount = bitsCount;
        }

        @Override
        public Range<BigInteger> get() {
            final BigInteger minimumValue = BigInteger.ZERO;
            final BigInteger maximumValue = BigInteger.valueOf(2L)
                    .pow(bitsCount)
                    .subtract(BigInteger.ONE);

            return Range.closed(minimumValue, maximumValue);
        }
    }
}
