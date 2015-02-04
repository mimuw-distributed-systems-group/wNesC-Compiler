package pl.edu.mimuw.nesc.constexpr.value.factory;

import java.math.BigInteger;
import pl.edu.mimuw.nesc.constexpr.value.UnsignedIntegerConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.type.UnsignedIntegerConstantType;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * <p>Implementation of integer constant factory for unsigned integer
 * values.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnsignedIntegerConstantFactory implements IntegerConstantFactory {
    /**
     * Count of bits for the created types and values.
     */
    private final int bitsCount;

    public UnsignedIntegerConstantFactory(int bitsCount) {
        checkArgument(bitsCount > 0, "count of bits must be positive");
        this.bitsCount = bitsCount;
    }

    @Override
    public UnsignedIntegerConstantType newType() {
        return new UnsignedIntegerConstantType(this.bitsCount);
    }

    @Override
    public UnsignedIntegerConstantValue newValue(BigInteger value) {
        return new UnsignedIntegerConstantValue(value, newType());
    }

    @Override
    public UnsignedIntegerConstantValue newValue(String value) {
        return newValue(new BigInteger(value));
    }

    @Override
    public UnsignedIntegerConstantValue newValue(long value) {
        return newValue(BigInteger.valueOf(value));
    }
}
