package pl.edu.mimuw.nesc.constexpr.value.type;

import com.google.common.base.Supplier;
import com.google.common.collect.Range;
import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * <p>Type that for a signed constant. Operations on values of this type
 * simulate two's complements arithmetic.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SignedIntegerConstantType extends IntegerConstantType {
    /**
     * The smallest positive number that after addition to every negative
     * value of this type yields a non-negative value.
     */
    private final BigInteger unsignedDisplacement;

    public SignedIntegerConstantType(int bitsCount) {
        super(bitsCount, new SignedRangeSupplier(bitsCount));
        this.unsignedDisplacement = BigInteger.valueOf(2L)
                .pow(bitsCount - 1);
    }

    /**
     * The smallest positive number that after addition to every negative value
     * of this type yields a non-negative value.
     *
     * @return The unsigned displacement of this type.
     */
    public BigInteger getUnsignedDisplacement() {
        return unsignedDisplacement;
    }

    @Override
    public Type getType() {
        return Type.SIGNED_INTEGER;
    }

    /**
     * Class responsible for creating a range of value of signed integer
     * constants types. It is the range of values that can be represented
     * using the two's complement representation.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class SignedRangeSupplier implements Supplier<Range<BigInteger>> {
        private final int bitsCount;

        private SignedRangeSupplier(int bitsCount) {
            checkArgument(bitsCount >= 1, "bits count cannot be non-positive");
            this.bitsCount = bitsCount;
        }

        @Override
        public Range<BigInteger> get() {
            final BigInteger minimumValue = BigInteger.valueOf(2L)
                    .pow(bitsCount - 1)
                    .negate();
            final BigInteger maximumValue = BigInteger.valueOf(2L)
                    .pow(bitsCount - 1)
                    .subtract(BigInteger.ONE);
            return Range.closed(minimumValue, maximumValue);
        }
    }
}
